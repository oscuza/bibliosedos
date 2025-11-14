/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.bibliotecasedaos.biblioteca.error;

/**
 * Excepció personalitzada llançada quan una operació de cerca
 * no troba l'entitat {@code Prestec} esperada a la base de dade
 * 
 * @author David García Rodríguez
 */
public class PrestecNotFoundException extends RuntimeException{
    
    /**
     * Constructor que crea una nova excepció amb un missatge concret.
     * @param message 
     */
    public PrestecNotFoundException(String message){
        super(message);
    }
}
