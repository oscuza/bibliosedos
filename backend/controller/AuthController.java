/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.bibliotecasedaos.biblioteca.controller;

import com.bibliotecasedaos.biblioteca.config.TokenBlacklist;
import com.bibliotecasedaos.biblioteca.config.JwtService;
import com.bibliotecasedaos.biblioteca.controller.models.AuthResponse;
import com.bibliotecasedaos.biblioteca.controller.models.AuthenticationRequest;
import com.bibliotecasedaos.biblioteca.controller.models.RegisterRequest;
import com.bibliotecasedaos.biblioteca.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador que gestiona el login i registre d'usuaris de l'aplicació.
 * Conte els endpoints i l'autenticació es fa mitjançant JSON Web Tokens (JWT)
 * 
 * @author David García Rodríguez
 */
@RestController
//@RequestMapping("/api/auth/")
@RequestMapping("/biblioteca/auth")
@RequiredArgsConstructor
public class AuthController {
    
    @Autowired
    private AuthService authService;   
    //Añadido--------------------------------------
    private final TokenBlacklist tokenBlacklist;
    private final JwtService jwtService;
    
    /**
     * David García Rodríguez
     * Registra un usuari a la base de dades.
     * EndPoint de access públic per afegir un usuari. Si el registre es exitos retorna un JWT Token.
     * @param request (nick, password)
     * @return {@code ResponseEntity<AuthResponse>} amb l'estat HTTP 200 (OK) i al cos un JWT Token, id, nom, cognom1, cognom2, rol.
     */
    @PostMapping("/afegirUsuari")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request){
        return ResponseEntity.ok(authService.register(request));
    }
    
    /**
     * David García Rodríguez
     * login  d'usuari a la base de dades.
     * EndPoint per fer login d'usuari. Si el login es exitos retorna un JWT Token i les dades del usuari.
     * @param request (nick, password)
     * @return {@code ResponseEntity<AuthResponse>} amb l'estat HTTP 200 (OK) i al cos un JWT Token, id, nom, cognom1, cognom2, rol.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> authenticate(@RequestBody AuthenticationRequest request){
        return ResponseEntity.ok(authService.authenticate(request));
    }
    
    //Añadido-------------------------------
    /**
     * David García Rodríguez
     * Logout d'usuari.
     * EndPoint per fer logout d'usuari
     * @param authorizationHeader La capçalera Authorization que conté el token "Bearer ".
     * @return Resposta 200 OK amb missatge de confirmació.
     */
    @PostMapping("logout")
    public ResponseEntity<String> logout(@RequestHeader("Authorization") String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body("Token no trobat o format incorrecte.");
        }

        try {

            String token = authorizationHeader.substring(7);          
            String tokenId = jwtService.extractAllClaims(token).getId(); 
            //Afegir a la llista negra
            tokenBlacklist.blacklistToken(tokenId);

            return ResponseEntity.ok("Logout am exit. Token revocat.");
        } catch (Exception e) {
            // Maneig d'errors si el token no és vàlid, invàlid o no té JTI
            return ResponseEntity.status(401).body("Token invàlid o error en la revocació.");
        }
    }
}
