/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.bibliotecasedaos.biblioteca.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;


/** 
 * Servei per gestionar les dades dels JSON Web Tokens.
 * 
 * @author David García Rodríguez
 */
@Service
public class JwtService {
    
    /** Temps d'expiració del token*/
    final long EXPIRATION_TIME = 1000 * 60 * 60 * 24;
    /** Clau secreta en Base64*/
    private static final String SECRET_KEY = "0a471b72362c992091af2a538eda277a4c1b0d2b2079d39c7b2fe8eeda59699a";
    
    /**
     * David García Rodríguez
     * Genera un JWT.
     * @param userDetails Detalls de l'usuari.
     * @return JWT de tipus String.
     */
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }
    
    /**
     * David García Rodríguez
     * Genera un JWT signat amb claims, id, subject, emissió i expiració.
     * @param extraClaims Claims.
     * @param userDetails Detalls de l'usuari.
     * @return El JWT signat.
     */
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        String tokenId = UUID.randomUUID().toString(); 
        
        return Jwts.builder()
                .setClaims(extraClaims)
                .setId(tokenId)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
       
    }

    /**
     * David García Roddríguez
     * Obté el nom d'usuari del token.
     * @param token.
     * @return userName.
     */
    public String getUserName(String token) {
        return getClaim(token, Claims::getSubject);
        
    }
    
    /**
     * David García Rodríguez
     * Obté un claim del token.
     * @param <T> Tipus del valor del claim.
     * @param token.
     * @param claimsResolver Funció per extreure el claim.
     * @return El claim.
     */
    public <T> T getClaim(String token, Function<Claims,T> claimsResolver) {
        final Claims claims = getAllClaims(token);
        return claimsResolver.apply(claims);
    }
    
    //nuevo----------------------------------------------------
    /**
     * Mètode principal que descodifica el token i extreu totes les claims (dades).
     * @param token El JWT en format String.
     * @return L'objecte Claims que conté totes les dades del token.
     */
    public Claims extractAllClaims(String token) {
        // Utilitzem el parseBuilder per verificar la signatura abans de processar.
        return Jwts
                .parserBuilder() // Constructor de l'analitzador JWT
                .setSigningKey(getSignInKey()) // Establim la clau secreta per verificar la signatura
                .build() // Construïm l'analitzador
                .parseClaimsJws(token) // Processem el token: verifica signatura i descodifica
                .getBody(); // Obtenim les claims (el cos del token)
    }

    /**
     * Obté tots els claims del token.
     * @param token El JWT.
     * @return Claims.
     */
    private Claims getAllClaims(String token) {
        return Jwts
                .parserBuilder()         
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        
    }

    /**
     * David García rodríguez
     * Converteix la clau secreta en una clau en Base64.
     * @return La clau.
     */
    private Key getSignInKey() {
        
        //byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
        byte[] keyBytes = SECRET_KEY.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    
    /**
     * David GArcía Rodríguez
     * Obté un valor true o false, si el token a expirat i concideix el nom d'suari.
     * @param token
     * @param userDetails
     * @return 
     */
    public boolean validateToken(String token, UserDetails userDetails) {
        final String username = getUserName(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    /**
     * Comprova si la data a expirat.
     * @param token.
     * @return {@code true} si ha expirat.
     */
    private boolean isTokenExpired(String token) {
        return getExpiration(token).before(new Date());
    }
    
    /**
     * David García Rodríguez
     * Obté la data d'expiració del token.
     * @param token.
     * @return La data.
     */
    private Date getExpiration(String token) {
        return getClaim(token, Claims::getExpiration);
    }
}
