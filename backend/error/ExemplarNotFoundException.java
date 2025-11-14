/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.bibliotecasedaos.biblioteca.error;

/**
 * Excepció personalitzada llançada quan una operació de cerca
 * no troba l'entitat {@code Exemplar} esperada a la base de dade
 * 
 * @author David García Rodríguez
 */
public class ExemplarNotFoundException extends Exception {
    public ExemplarNotFoundException (String message) {
        super(message);   
    }   
}
