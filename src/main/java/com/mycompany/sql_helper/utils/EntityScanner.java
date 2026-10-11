
package com.mycompany.sql_helper.utils;

import com.mycompany.sql_helper.annotation.SQLTable;
import com.mycompany.sql_helper.connection.SQLiteConnection;

import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;

public final class EntityScanner {

    private EntityScanner() {
    }

    public static void initialize(
            String databaseName,
            Class<?> rootClass
    ) throws SQLException {

        if (rootClass == null) {
            throw new IllegalArgumentException(
                    "Root class must not be null."
            );
        }

        // Package ng consumer project na isi-scan.
        String basePackage = rootClass.getPackageName();

        Reflections reflections = new Reflections(
                basePackage,
                Scanners.TypesAnnotated
        );

        Set<Class<?>> entities =
                reflections.getTypesAnnotatedWith(SQLTable.class);

        try (Connection connection =
                     SQLiteConnection.getConnection(databaseName)) {

            for (Class<?> entity : entities) {
                SQLHelper.createTable(connection, entity);
            }
        }

        System.out.println(
                "SQLite initialization complete. Tables checked: "
                        + entities.size()
        );
    }
}
