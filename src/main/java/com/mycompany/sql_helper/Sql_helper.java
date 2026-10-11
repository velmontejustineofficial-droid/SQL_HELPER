
package com.mycompany.sql_helper;

import com.mycompany.sql_helper.repository.SQLiteCrudRepository;
import com.mycompany.sql_helper.utils.EntityScanner;

public class Sql_helper {

    private static final String DATABASE_NAME = "test";

    public static void main(String[] args) throws Exception {

        // Create tables first
        EntityScanner.initialize(DATABASE_NAME, Sql_helper.class);
        SQLiteCrudRepository<User, Integer> repo = new SQLiteCrudRepository<>(DATABASE_NAME, User.class);

        User user = new User(1, "test", "test@gmail.com");

        // CREATE
        repo.insert(user);

        // READ ALL
        repo.findAll().forEach(System.out::println);

        // READ ONE
        repo.findById(user.getId()).ifPresent(System.out::println);

        // UPDATE
        repo.update(new User(
                user.getId(),
                "Juan Dela Cruz",
                "test@gmail.com"
        ));

        // DELETE
        repo.deleteById(user.getId());
    }
}
