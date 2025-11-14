/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.bibliotecasedaos.biblioteca.service;

import com.bibliotecasedaos.biblioteca.config.JwtService;
import com.bibliotecasedaos.biblioteca.controller.models.AuthResponse;
import com.bibliotecasedaos.biblioteca.controller.models.AuthenticationRequest;
import com.bibliotecasedaos.biblioteca.controller.models.RegisterRequest;
import com.bibliotecasedaos.biblioteca.entity.Role;
import com.bibliotecasedaos.biblioteca.entity.Usuari;
import com.bibliotecasedaos.biblioteca.error.NickAlreadyExistsException;
import com.bibliotecasedaos.biblioteca.repository.UsuariRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Servei d'autenticació per gestionar els registres i autenticació d'usuaris
 * mitjançant Spring Security i JSON Web tokens.
 * 
 * @author David García Rodríguez
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService{

    /** Repositori per a l'accés a les dades de l'Usuari. */
    private final UsuariRepository usuariRepository;
    /** Codificador de contrasenyes. */
    private final PasswordEncoder passwordEncoder;
    /** Servei per a la creació i gestió dels JSON Web Tokens. */
    private final JwtService jwtService;
    /** Gestor d'autenticació de Spring Security. */
    private final AuthenticationManager authenticationManager;
    
    /**
     * David GArcía Rodríguez
     * Afegir un nou usuari.
     * Crea un nou Usuari a partir de les dades proporcionades, 
     * xifra la contrasenya i la desa al repositori. Finalment, genera
     * un token JWT per al nou usuari.
     * * @param request Dades de l'usuari (nick, nif, nom, etc.).
     * @return AuthResponse que conté el token JWT per al nou usuari registrat.
     */
    @Override
    public AuthResponse register(RegisterRequest request) {
        
        if (usuariRepository.existsByNick(request.getNick())) {
            // Llança la nostra excepció de negoci
            throw new NickAlreadyExistsException("El nick '" + request.getNick() + "' ja existeix a la base de dades. Si us plau, escolliu-ne un altre.");
        }
        
        var user = Usuari.builder()              
                .nick(request.getNick())
                .nif(request.getNif())
                .nom(request.getNom())
                .cognom1(request.getCognom1())
                .cognom2(request.getCognom2())
                .localitat(request.getLocalitat())
                .provincia(request.getProvincia())
                .carrer(request.getCarrer())
                .cp(request.getCp())
                .tlf(request.getTlf())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .rol(request.getRol())
                //.role(Role.USER)
                .build();
        usuariRepository.save(user);
        var jwtToken = jwtService.generateToken(user);
        return AuthResponse.builder()
                .token(jwtToken)
                .nom(user.getNom())
                .cognom1(user.getCognom1())
                .cognom2(user.getCognom2())
                .rol(user.getRol())
                .id(user.getId())
                .build();
                
                
    }

    /**
     * David García Rodríguez
     * Autentica un usuari existent passant pel request el seu nick i contrasenya.
     * Autentica l'suari amb auhtenticationManager, si l'autenticació és exitosa retorna les
     * dades del usuari amb un token JWT.
     * * @param request Dades d'autenticació (nick i contrasenya).
     * @return AuthResponse que conté el token JWT i dades essencials de l'usuari (nom, id, rol).
     * @throws org.springframework.security.core.AuthenticationException si les credencials són invàlides.
     * @throws java.util.NoSuchElementException si l'usuari no es troba després de l'autenticació (si no es gestiona l'orElseThrow).
     */
    @Override
    public AuthResponse authenticate(AuthenticationRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                request.getNick(), 
                request.getPassword())
        );
        
        var user = usuariRepository.findUsuariByNickWithJPQL(request.getNick()).orElseThrow();
        var jwtToken = jwtService.generateToken(user);
        
        return AuthResponse.builder()
                .token(jwtToken)
                .nom(user.getNom())
                .cognom1(user.getCognom1())
                .cognom2(user.getCognom2())
                .rol(user.getRol())
                //.role(user.getRole())
                .id(user.getId())
                .build();
        
    }   
}
