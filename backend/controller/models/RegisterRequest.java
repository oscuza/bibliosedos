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
 * Oobjecte de Sol·licitud (Request) utilitzat per al procés de registre
 * d'un nou usuari al sistema.
 * Conté totes les dades necessaries per crear un usuari.
 *
 * @author David García Rodríguez
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    
    /** Pseudónim del usuari. */
    private String nick;
    /** Número d'Identificació Fiscal (NIF) de l'usuari. */
    private String nif;
    /** Nom de l'usuari. */
    private String nom;
    /** Primer cognom de l'usuari. */
    private String cognom1;
    /** Segon cognom de l'usuari. */
    private String cognom2;
    /** Localitat residència. */
    private String localitat;
    /** Província de residència. */
    private String provincia;
    /** carrer de residència. */
    private String carrer;
    /** Codi Postal. */
    private String cp;
    /** Número de telèfon. */
    private String tlf;
    /** Adreça de correu electrònic. */
    private String email;
    /** Contrasenya de l'usuari. */
    private String password;
    /** rol de l'usuari (admin o normal). */
    private int rol;
    
}
