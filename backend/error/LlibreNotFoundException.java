/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.bibliotecasedaos.biblioteca.error;

/**
 * Excepció personalitzada llançada quan una operació de cerca
 * no troba l'entitat {@code Llibre} esperada a la base de dades.
 * 
 * @author David García Rodríguez
 */
public class LlibreNotFoundException extends Exception{
    public LlibreNotFoundException(String message) {
        super(message);
    }
}
