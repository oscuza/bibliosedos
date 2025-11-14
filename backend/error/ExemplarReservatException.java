/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.bibliotecasedaos.biblioteca.error;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * * Excepció personalitzada llançada quan s'intenta realitzar un préstec amb un exemplar que es troba en un estat "prestat".
 * 
 * @author David García Rodríguez
 */
public class ExemplarReservatException extends RuntimeException{
    public ExemplarReservatException(String message) {
        super(message);
    }
}
