/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.bibliotecasedaos.biblioteca.config;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Clase de tipus component que fa la funció de llista negra per als tokens no autoritzats.
 * 
 * @author dg
 */
@Component
public class TokenBlacklist {
    private final Set<String> revokedTokens = Collections.synchronizedSet(new HashSet<>());

    /**
     * David GArcía Rodríguez
     * Afegeix id del token a la llista negra.
     * @param tokenId L'ID del token a revocar.
     */
    public void blacklistToken(String tokenId) {
        revokedTokens.add(tokenId);
    }

    /**
     * David García Rodríguez
     * Comprova si un token ID es troba a la llista negra.
     * @param tokenId L'ID del token a comprovar.
     * @return True si el token està revocat, false en cas contrari.
     */
    public boolean isBlacklisted(String tokenId) {
        return revokedTokens.contains(tokenId);
    }
}
