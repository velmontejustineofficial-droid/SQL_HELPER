/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.sql_helper;

import com.mycompany.sql_helper.annotation.SQLColumn;
import com.mycompany.sql_helper.annotation.SQLId;
import com.mycompany.sql_helper.annotation.SQLTable;



@SQLTable(name = "users_table")
public class User {

    @SQLId
    @SQLColumn(name = "id", type = "INTEGER")
    private int id;

    @SQLColumn(name = "username", nullable = false)
    private String username;

    @SQLColumn(name = "email", unique = true)
    private String email;

    public User() {
    }

    // Constructor
    public User(int id, String username, String email) {
        this.id = id;
        this.username = username;
        this.email = email;
    }

    // Getter
    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    // Setter
    public void setId(int id) {
        this.id = id;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setEmail(String email) {
        this.email = email;
    }
    
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                '}';
    }

}
