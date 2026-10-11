
package com.mycompany.sql_helper.repository;

import com.mycompany.sql_helper.annotation.SQLColumn;
import com.mycompany.sql_helper.annotation.SQLId;
import com.mycompany.sql_helper.annotation.SQLTable;
import com.mycompany.sql_helper.connection.SQLiteConnection;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;

public class SQLiteCrudRepository<T, ID>{

    private final String databaseName;
    private final Class<T> entityClass;
    private final String tableName;
    private final Field idField;
    private final List<Field> columns;

    public SQLiteCrudRepository(
            String databaseName,
            Class<T> entityClass
    ) {
        this.databaseName = databaseName;
        this.entityClass = entityClass;

        SQLTable table = entityClass.getAnnotation(SQLTable.class);

        if (table == null) {
            throw new IllegalArgumentException(
                    "Entity must have @SQLTable: "
                    + entityClass.getName()
            );
        }

        this.tableName = table.name();
        validateIdentifier(tableName);

        List<Field> mappedColumns = new ArrayList<>();
        Field primaryKey = null;

        for (Field field : entityClass.getDeclaredFields()) {
            SQLColumn column = field.getAnnotation(SQLColumn.class);

            if (column == null) {
                continue;
            }

            validateIdentifier(column.name());
            field.setAccessible(true);
            mappedColumns.add(field);

            if (field.isAnnotationPresent(SQLId.class)) {
                if (primaryKey != null) {
                    throw new IllegalArgumentException(
                            "Only one @SQLId field is supported."
                    );
                }

                primaryKey = field;
            }
        }

        if (primaryKey == null) {
            throw new IllegalArgumentException(
                    "Entity must have one @SQLId field."
            );
        }

        this.idField = primaryKey;
        this.columns = List.copyOf(mappedColumns);
    }


    public void insert(T entity) throws SQLException {
        List<Field> insertColumns = columns.stream()
                .filter(field -> !field.isAnnotationPresent(SQLId.class))
                .toList();

        if (insertColumns.isEmpty()) {
            throw new SQLException(
                    "No non-ID columns available for insert."
            );
        }

        StringJoiner names = new StringJoiner(", ");
        StringJoiner placeholders = new StringJoiner(", ");

        for (Field field : insertColumns) {
            names.add(columnName(field));
            placeholders.add("?");
        }

        String sql = "INSERT INTO " + tableName
                + " (" + names + ") VALUES (" + placeholders + ")";

        try (Connection connection =
                     SQLiteConnection.getConnection(databaseName);
             PreparedStatement statement = connection.prepareStatement(sql)) {

            int index = 1;

            for (Field field : insertColumns) {
                statement.setObject(index++, getFieldValue(field, entity));
            }

            statement.executeUpdate();

            // Kunin ang generated SQLite ID.
            try (Statement idStatement = connection.createStatement();
                 ResultSet rs = idStatement.executeQuery(
                         "SELECT last_insert_rowid()")) {

                if (rs.next()) {
                    setFieldValue(idField, entity, rs.getObject(1));
                }
            }
        } catch (IllegalAccessException e) {
            throw new SQLException("Unable to read entity fields.", e);
        }
    }


    public List<T> findAll() throws SQLException {
        String sql = "SELECT * FROM " + tableName;
        List<T> results = new ArrayList<>();

        try (Connection connection =
                     SQLiteConnection.getConnection(databaseName);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                results.add(mapRow(rs));
            }
        }

        return results;
    }
    public Optional<T> findById(ID id) throws SQLException {
        String sql = "SELECT * FROM " + tableName
                + " WHERE " + columnName(idField) + " = ?";

        try (Connection connection =
                     SQLiteConnection.getConnection(databaseName);
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, id);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }

        return Optional.empty();
    }

    public void update(T entity) throws SQLException {
        List<Field> updateColumns = columns.stream()
                .filter(field -> !field.isAnnotationPresent(SQLId.class))
                .toList();

        if (updateColumns.isEmpty()) {
            throw new SQLException("No columns available for update.");
        }

        StringJoiner assignments = new StringJoiner(", ");

        for (Field field : updateColumns) {
            assignments.add(columnName(field) + " = ?");
        }

        String sql = "UPDATE " + tableName + " SET " + assignments
                + " WHERE " + columnName(idField) + " = ?";

        try (Connection connection =
                     SQLiteConnection.getConnection(databaseName);
             PreparedStatement statement = connection.prepareStatement(sql)) {

            int index = 1;

            for (Field field : updateColumns) {
                statement.setObject(index++, getFieldValue(field, entity));
            }

            statement.setObject(index, getFieldValue(idField, entity));
            statement.executeUpdate();

        } catch (IllegalAccessException e) {
            throw new SQLException("Unable to read entity fields.", e);
        }
    }


    public void deleteById(ID id) throws SQLException {
        String sql = "DELETE FROM " + tableName
                + " WHERE " + columnName(idField) + " = ?";

        try (Connection connection =
                     SQLiteConnection.getConnection(databaseName);
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setObject(1, id);
            statement.executeUpdate();
        }
    }

    private T mapRow(ResultSet rs) throws SQLException {
        try {
            Constructor<T> constructor =
                    entityClass.getDeclaredConstructor();

            constructor.setAccessible(true);
            T entity = constructor.newInstance();

            for (Field field : columns) {
                Object value = rs.getObject(columnName(field));
                setFieldValue(field, entity, value);
            }

            return entity;

        } catch (ReflectiveOperationException e) {
            throw new SQLException(
                    "Unable to map database row to "
                    + entityClass.getName(), e
            );
        }
    }

    private Object getFieldValue(Field field, T entity)
            throws IllegalAccessException {
        return field.get(entity);
    }

    private void setFieldValue(
            Field field,
            T entity,
            Object value
    ) throws IllegalAccessException {

        if (value == null) {
            if (field.getType().isPrimitive()) {
                throw new IllegalArgumentException(
                        "Cannot assign NULL to primitive field: "
                        + field.getName()
                );
            }

            field.set(entity, null);
            return;
        }

        Class<?> type = field.getType();

        if (type == int.class || type == Integer.class) {
            field.set(entity, ((Number) value).intValue());
        } else if (type == long.class || type == Long.class) {
            field.set(entity, ((Number) value).longValue());
        } else if (type == double.class || type == Double.class) {
            field.set(entity, ((Number) value).doubleValue());
        } else if (type == float.class || type == Float.class) {
            field.set(entity, ((Number) value).floatValue());
        } else if (type == boolean.class || type == Boolean.class) {
            field.set(entity, ((Number) value).intValue() != 0);
        } else if (type == String.class) {
            field.set(entity, value.toString());
        } else {
            field.set(entity, value);
        }
    }

    private String columnName(Field field) {
        return field.getAnnotation(SQLColumn.class).name();
    }

    private void validateIdentifier(String name) {
        if (name == null
                || !name.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException(
                    "Invalid SQL identifier: " + name
            );
        }
    }
}
