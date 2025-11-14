/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.bibliotecasedaos.biblioteca.controller.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Objecte de Sol·licitud (Request) utilitzat per a l'autenticació
 * d'un usuari.
 * Conté els atributs necessàries per verificar la identitat de l'usuari.
 *
 * @author David García Rodríguez
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticationRequest {
    
    /** Pseudónim d'usuari únic. */
    private String nick;
    /** Contrasenya de l'usuari. */
    private String password;
}
