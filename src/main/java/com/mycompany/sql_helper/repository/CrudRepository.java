package com.mycompany.sql_helper.repository;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface CrudRepository<T, ID> {

    void insert(T entity) throws SQLException;

    List<T> findAll() throws SQLException;

    Optional<T> findById(ID id) throws SQLException;

    void update(T entity) throws SQLException;

    void deleteById(ID id) throws SQLException;
}