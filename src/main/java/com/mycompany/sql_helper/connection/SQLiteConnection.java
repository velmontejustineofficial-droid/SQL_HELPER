
package com.mycompany.sql_helper.connection;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class SQLiteConnection {

    private SQLiteConnection() {
    }

    public static Connection getConnection(String databaseName)
            throws SQLException {

        if (databaseName == null || databaseName.isBlank()) {
            throw new IllegalArgumentException(
                    "Database name must not be empty."
            );
        }

        // Automatically add .db extension when omitted.
        if (!databaseName.endsWith(".db")) {
            databaseName += ".db";
        }

        Path databasePath = Path.of("data", databaseName)
                .toAbsolutePath()
                .normalize();

        try {
            Files.createDirectories(databasePath.getParent());
        } catch (IOException e) {
            throw new SQLException(
                    "Unable to create database directory.", e
            );
        }

        String url = "jdbc:sqlite:" + databasePath;

        return DriverManager.getConnection(url);
    }
}
