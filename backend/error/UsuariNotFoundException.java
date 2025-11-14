/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.bibliotecasedaos.biblioteca.error;

/**
 * Excepció personalitzada llançada quan una operació de cerca
 * no troba l'entitat {@code Usuari} esperada a la base de dade
 * 
 * @author David García Rodríguez
 */
public class UsuariNotFoundException extends Exception{

    /**
     * Constructor que crea una nova excepció amb un missatge concret.
     * @param message 
     */
    public UsuariNotFoundException(String message) {
        super(message);
    }
    
}
