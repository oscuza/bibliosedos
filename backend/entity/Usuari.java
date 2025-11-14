/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.bibliotecasedaos.biblioteca.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Collection;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Entitat que representa un usuari en la base de dades de la biblioteca.
 * Aquesta classe implementa {@code UserDetails} per a la integració amb
 * Spring Security, proporcionant la informació necessària per a
 * l'autenticació i l'autorització.
 *
 * @author David García Rodríguez
 */
@Entity
@Table(name = "usuaris")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Usuari implements UserDetails{
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false)
    @NotBlank(message = "Per favor afegeix un nick")
    @Size(max = 10, message = "El Nick ha de tenir un màxim de 10 caràcters")
    private String nick;
    //@Length(min = 9, max = 9)  
    @Column(unique = true, nullable = false)
    @NotBlank(message = "Per favor afegeix un NIF")
    @Size(min = 9, max = 9, message = "El NIF ha de tenir 9 caràcters (8 números i 1 lletra).")
    private String nif;
    @Column(nullable = false)
    @NotBlank(message = "Per favor afegeix un nom")
    private String nom;
    @Column(nullable = false)
    @NotBlank(message = "Per favor afegeix un cognom")
    private String cognom1;
    private String cognom2;
    @Column(nullable = false)
    @NotBlank(message = "Per favor afegeix una localitat")
    private String localitat;
    @Column(nullable = false)
    @NotBlank(message = "Per favor afegeix una provincia")
    private String provincia;
    @Column(nullable = false)
    @NotBlank(message = "Per favor afegeix un carrer")
    private String carrer;
    @Column(nullable = false)
    @NotBlank(message = "Per favor afegeix un codi postal")
    private String cp;
    @Column(nullable = false)
    @NotBlank(message = "Per favor afegeix un telèfon")
    @Size(min = 9, max = 9, message = "El telèfon ha de tenir 9 dígits.")
    private String tlf;
    @Column(unique = true, nullable = false)
    @Email(message = "Format d'email invàlid")
    @NotBlank(message = "Per favor afegeix un email")
    private String email;
    @Column(nullable = false)
    @NotBlank(message = "Per favor afegeix un password")
    private String password;
    private int rol;
    
    /**
     * Retorna les autoritats o rols concedits a l'usuari.
     * Converteix el valor enter 'rol' en una autoritat de tipus String (ADMIN o USER).
     *
     * @return Col·lecció d'autoritats concedides (rols).
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        
        String roleName = "USER";
        if (this.rol == 2) {
            roleName = "ADMIN";
        }
        return List.of(new SimpleGrantedAuthority(roleName));
    }

    /**
     * Retorna el nom d'usuari utilitzat per a l'autenticació (nick).
     *
     * @return El nick de l'usuari.
     */
    @Override
    public String getUsername() {
        return nick;
    }

    /**
     * Indica si el compte de l'usuari ha expirat.
     * (Retorna true per defecte).
     *
     * @return {@code true} si el compte és vàlid (no ha expirat).
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Indica si el compte de l'usuari està bloquejat.
     * (Retorna true per defecte).
     *
     * @return {@code true} si el compte no està bloquejat.
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * Indica si les credencials (contrasenya) de l'usuari han caducat.
     * (Retorna true per defecte).
     *
     * @return {@code true} si les credencials són vàlides (no han caducat).
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Indica si l'usuari està habilitat.
     * (Retorna true per defecte).
     *
     * @return {@code true} si l'usuari està habilitat.
     */
    @Override
    public boolean isEnabled() {
        return true;
    }

    /**
     * Retorna la contrasenya utilitzada per a l'autenticació.
     *
     * @return La contrasenya de l'usuari.
     */
    @Override
    public String getPassword() {
        return password;
    }
    
    
}
