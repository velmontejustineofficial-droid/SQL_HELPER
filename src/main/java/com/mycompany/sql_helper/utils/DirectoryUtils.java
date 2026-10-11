/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.sql_helper.utils;

import java.nio.file.Path;

public final class DirectoryUtils {

    private DirectoryUtils() {
    }

    public static Path getCurrentDirectory() {
        return Path.of(System.getProperty("user.dir"))
                  .toAbsolutePath()
                  .normalize();
    }
}