/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.bibliotecasedaos.biblioteca.service;

import com.bibliotecasedaos.biblioteca.controller.models.AuthResponse;
import com.bibliotecasedaos.biblioteca.controller.models.AuthenticationRequest;
import com.bibliotecasedaos.biblioteca.controller.models.RegisterRequest;


/**
 * Interfície de servei que defineix els métodes per gestionar el registre i autenticació 
 * sobre la entitat Usuari
 * 
 * @author David García Rodríguez
 */
public interface AuthService {
    
    /**
     * Afegeix un registre d'un nou usuari a la base de dades.
     * @param request Petició amb les dades de registre.
     * @return {@link AuthResponse} amb el JWT i les dades de l'usuari registrat.
     */
    AuthResponse register (RegisterRequest request);
    /**
     * Realitza el procés d'autenticació i login d'un usuari.
     * @param request Petició amb nick i contrasenya.
     * @return {@link AuthResponse} amb el JWT generat i les dades de l'usuari.
     */
    AuthResponse authenticate (AuthenticationRequest request);
    
        
    
}
