/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.bibliotecasedaos.biblioteca.controller.models;

import com.bibliotecasedaos.biblioteca.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 * @author dg
 */
/**
 * Objecte de resposta (DTO) retornat després d'un procés
 * de login o registre.
 * Conté el token JWT i les dades essencials de l'usuari autenticat.
 *
 * @author DavidGarcíaRodríguez
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    
    /** Token JWT generat per a l'usuari, utilitzat en futures peticions. */
    private String token;
    /** Identificador únic de l'usuari (ID) a la base de dades. */
    private Long id;
    /** Nom de l'usuari. */
    private String nom;
    /** Primer cognom de l'usuari. */
    private String cognom1;
    /** Segon cognom de l'usuari. */
    private String cognom2;
    /** rol de l'usuari (admin o normal). */
    private int rol;
    private Role role;
    
}
