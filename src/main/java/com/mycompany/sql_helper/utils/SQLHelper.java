/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.sql_helper.utils;


import com.mycompany.sql_helper.annotation.SQLColumn;
import com.mycompany.sql_helper.annotation.SQLId;
import com.mycompany.sql_helper.annotation.SQLTable;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class SQLHelper {

    private SQLHelper() {
    }

    public static void createTable(
            Connection connection,
            Class<?> entityClass
    ) throws SQLException {

        String sql = buildCreateTableSQL(entityClass);

        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }

        System.out.println(
                "Table ready: " + entityClass.getSimpleName()
        );
        System.out.println(sql + ";");
    }

    public static String buildCreateTableSQL(Class<?> entityClass) {

        SQLTable table = entityClass.getAnnotation(SQLTable.class);

        if (table == null) {
            throw new IllegalArgumentException(
                    "Missing @SQLTable: " + entityClass.getName()
            );
        }

        validateIdentifier(table.name());

        List<String> definitions = new ArrayList<>();
        int idCount = 0;

        for (Field field : entityClass.getDeclaredFields()) {

            SQLColumn column = field.getAnnotation(SQLColumn.class);
            boolean isId = field.isAnnotationPresent(SQLId.class);

            if (column == null) {
                if (isId) {
                    throw new IllegalArgumentException(
                            "@SQLId must also have @SQLColumn: "
                                    + field.getName()
                    );
                }
                continue;
            }

            validateIdentifier(column.name());

            String type = column.type().trim();

            if (!type.matches(
                    "(?i)[A-Z]+(?:\\s*\\(\\s*\\d+\\s*\\))?"
            )) {
                throw new IllegalArgumentException(
                        "Invalid SQL type: " + type
                );
            }

            StringBuilder definition = new StringBuilder()
                    .append(column.name())
                    .append(" ")
                    .append(type);

            if (isId) {
                idCount++;
                definition.append(" PRIMARY KEY");
            }

            if (!column.nullable() || isId) {
                definition.append(" NOT NULL");
            }

            if (column.unique() && !isId) {
                definition.append(" UNIQUE");
            }

            definitions.add(definition.toString());
        }

        if (idCount != 1) {
            throw new IllegalArgumentException(
                    entityClass.getSimpleName()
                            + " must have exactly one @SQLId field."
            );
        }

        if (definitions.isEmpty()) {
            throw new IllegalArgumentException(
                    "No @SQLColumn fields in " + entityClass.getName()
            );
        }

        return "CREATE TABLE IF NOT EXISTS "
                + table.name()
                + " ("
                + String.join(", ", definitions)
                + ")";
    }

    private static void validateIdentifier(String identifier) {
        if (identifier == null
                || !identifier.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException(
                    "Invalid SQL identifier: " + identifier
            );
        }
    }
}

