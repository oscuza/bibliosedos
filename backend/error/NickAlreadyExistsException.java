/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.bibliotecasedaos.biblioteca.error;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
/**
 * Excepció d'execució (RuntimeException) llançada quan s'intenta crear o actualitzar
 * un recurs amb un 'nick' que ja existeix a la base de dades.
 * 
 * @author David García Rodríguez
 */
@ResponseStatus(HttpStatus.CONFLICT) // 409 Conflict
public class NickAlreadyExistsException extends RuntimeException {
    /**
     * Excepció llançada quan es detecta que el Nick d'usuari ja existeix.
     * @param message
     */
    public NickAlreadyExistsException(String message) {
        super(message);
    }
}
