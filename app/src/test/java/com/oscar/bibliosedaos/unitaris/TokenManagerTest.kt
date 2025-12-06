package com.oscar.bibliosedaos.unitaris

import com.oscar.bibliosedaos.data.network.TokenManager
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Tests unitaris per a la classe TokenManager.
 * 
 * **Descripció:**
 * Aquests tests verifiquen la funcionalitat relacionada amb la gestió de tokens JWT
 * en el TokenManager (singleton en memòria).
 * 
 * **Cobertura:**
 * - Guardar token (casos bàsics i sobrescriptura)
 * - Obtenir token (amb i sense token guardat)
 * - Netejar token (casos bàsics i múltiples neteges)
 * - Comprovar si existeix token (amb i sense token guardat)
 * - Gestió de l'estat del token (null, no null, etc.)
 * 
 *
 * **Nota:**
 * Aquests tests netegen el token abans i després de cada test per assegurar
 * que cada test comença amb un estat net (sense token guardat).
 * 
 * @author Oscar
 * @since 1.0
 * @see TokenManager
 */
class TokenManagerTest {

    @Before
    fun setUp() {
        // Netejar token abans de cada test
        TokenManager.clearToken()
    }

    @After
    fun tearDown() {
        // Netejar token després de cada test
        TokenManager.clearToken()
    }

    /**
     * Test: Guardar token amb èxit.
     * 
     * **Què s'està provant:**
     * - Que el TokenManager pot guardar un token correctament
     * - Que després de guardar, el token està disponible
     * - Que després de guardar, hasToken() retorna true
     * - Que després de guardar, getToken() retorna el token guardat
     * 
     * **Condicions:**
     * - No hi ha token guardat inicialment (netejar a setUp)
     * - Es guarda un token "test-token-123"
     * 
     * **Resultats esperats:**
     * - getToken() == "test-token-123"
     * - hasToken() == true
     */
    @Test
    fun saveToken_guardaToken() {
        // Given
        val token = "test-token-123"

        // When
        TokenManager.saveToken(token)

        // Then
        assertEquals("Token hauria d'estar guardat", token, TokenManager.getToken())
        assertTrue("Hauria de tenir token", TokenManager.hasToken())
    }

    /**
     * Test: Obtenir token sense token guardat.
     * 
     * **Què s'està provant:**
     * - Que el TokenManager retorna null quan no hi ha token guardat
     * - Que hasToken() retorna false quan no hi ha token guardat
     * - Que getToken() i hasToken() són consistents
     * 
     * **Condicions:**
     * - No hi ha token guardat inicialment (netejar a setUp)
     * 
     * **Resultats esperats:**
     * - getToken() == null
     * - hasToken() == false
     */
    @Test
    fun getToken_senseToken_retornaNull() {
        // Given - No hi ha token guardat

        // When
        val token = TokenManager.getToken()

        // Then
        assertNull("Token hauria de ser null", token)
        assertFalse("No hauria de tenir token", TokenManager.hasToken())
    }

    /**
     * Test: Obtenir token amb token guardat.
     * 
     * **Què s'està provant:**
     * - Que el TokenManager retorna el token correcte quan hi ha token guardat
     * - Que getToken() retorna el mateix token que s'ha guardat
     * - Que hasToken() retorna true quan hi ha token guardat
     * 
     * **Condicions:**
     * - Es guarda un token "test-token-456"
     * - Es consulta el token guardat
     * 
     * **Resultats esperats:**
     * - getToken() == "test-token-456"
     * - hasToken() == true
     */
    @Test
    fun getToken_ambToken_retornaToken() {
        // Given
        val token = "test-token-456"
        TokenManager.saveToken(token)

        // When
        val retrievedToken = TokenManager.getToken()

        // Then
        assertEquals("Token hauria de coincidir", token, retrievedToken)
    }

    /**
     * Test: Netejar token amb èxit.
     * 
     * **Què s'està provant:**
     * - Que el TokenManager pot eliminar un token guardat correctament
     * - Que després de netejar, el token ja no està disponible
     * - Que després de netejar, hasToken() retorna false
     * - Que després de netejar, getToken() retorna null
     * 
     * **Condicions:**
     * - Es guarda un token "test-token-789"
     * - Es neteja el token
     * 
     * **Resultats esperats:**
     * - Abans de netejar: hasToken() == true
     * - Després de netejar: getToken() == null
     * - Després de netejar: hasToken() == false
     */
    @Test
    fun clearToken_eliminaToken() {
        // Given
        val token = "test-token-789"
        TokenManager.saveToken(token)
        assertTrue("Hauria de tenir token abans de netejar", TokenManager.hasToken())

        // When
        TokenManager.clearToken()

        // Then
        assertNull("Token hauria de ser null després de netejar", TokenManager.getToken())
        assertFalse("No hauria de tenir token", TokenManager.hasToken())
    }

    /**
     * Test: Comprovar si existeix token sense token guardat.
     * 
     * **Què s'està provant:**
     * - Que hasToken() retorna false quan no hi ha token guardat
     * - Que hasToken() és consistent amb getToken() == null
     * 
     * **Condicions:**
     * - No hi ha token guardat inicialment (netejar a setUp)
     * 
     * **Resultats esperats:**
     * - hasToken() == false
     * - getToken() == null
     */
    @Test
    fun hasToken_senseToken_retornaFalse() {
        // Given - No hi ha token

        // When
        val hasToken = TokenManager.hasToken()

        // Then
        assertFalse("No hauria de tenir token", hasToken)
    }

    /**
     * Test: Comprovar si existeix token amb token guardat.
     * 
     * **Què s'està provant:**
     * - Que hasToken() retorna true quan hi ha token guardat
     * - Que hasToken() és consistent amb getToken() != null
     * 
     * **Condicions:**
     * - Es guarda un token "test-token-abc"
     * 
     * **Resultats esperats:**
     * - hasToken() == true
     * - getToken() != null
     */
    @Test
    fun hasToken_ambToken_retornaTrue() {
        // Given
        val token = "test-token-abc"
        TokenManager.saveToken(token)

        // When
        val hasToken = TokenManager.hasToken()

        // Then
        assertTrue("Hauria de tenir token", hasToken)
    }

    /**
     * Test: Sobrescriptura de token.
     * 
     * **Què s'està provant:**
     * - Que el TokenManager pot sobrescriure un token existent
     * - Que després de sobrescriure, el token anterior ja no està disponible
     * - Que després de sobrescriure, el nou token està disponible
     * - Que el TokenManager només manté un token a la vegada
     * 
     * **Condicions:**
     * - Es guarda un token "token-1"
     * - Es guarda un nou token "token-2" (sobrescriu el primer)
     * 
     * **Resultats esperats:**
     * - getToken() == "token-2"
     * - getToken() != "token-1"
     * - hasToken() == true
     */
    @Test
    fun saveToken_sobrescriuTokenAnterior() {
        // Given
        val token1 = "token-1"
        val token2 = "token-2"
        TokenManager.saveToken(token1)

        // When
        TokenManager.saveToken(token2)

        // Then
        assertEquals("Token hauria d'haver estat sobrescrit", token2, TokenManager.getToken())
        assertNotEquals("Token anterior no hauria d'estar present", token1, TokenManager.getToken())
    }

    /**
     * Test: Netejar token múltiples vegades.
     * 
     * **Què s'està provant:**
     * - Que netejar el token múltiples vegades no causa errors
     * - Que netejar el token quan ja està net no canvia l'estat
     * - Que el TokenManager gestiona correctament múltiples neteges
     * 
     * **Condicions:**
     * - No hi ha token guardat inicialment (netejar a setUp)
     * - Es neteja el token múltiples vegades
     * 
     * **Resultats esperats:**
     * - getToken() == null (després de cada neteja)
     * - hasToken() == false (després de cada neteja)
     * - No hi ha errors ni excepcions
     */
    @Test
    fun clearToken_multiplesVegades_noFaRes() {
        // Given
        TokenManager.clearToken()

        // When
        TokenManager.clearToken()
        TokenManager.clearToken()

        // Then
        assertNull("Token hauria de continuar sent null", TokenManager.getToken())
        assertFalse("No hauria de tenir token", TokenManager.hasToken())
    }

    /**
     * Test: Obtenir token després de netejar.
     * 
     * **Què s'està provant:**
     * - Que després de netejar el token, getToken() retorna null
     * - Que després de netejar el token, hasToken() retorna false
     * - Que el TokenManager gestiona correctament la neteja del token
     * 
     * **Condicions:**
     * - Es guarda un token "test-token-xyz"
     * - Es verifica que el token està guardat
     * - Es neteja el token
     * - Es consulta el token després de netejar
     * 
     * **Resultats esperats:**
     * - Abans de netejar: getToken() == "test-token-xyz"
     * - Després de netejar: getToken() == null
     * - Després de netejar: hasToken() == false
     */
    @Test
    fun getToken_despresClear_retornaNull() {
        // Given
        val token = "test-token-xyz"
        TokenManager.saveToken(token)
        assertEquals("Token hauria d'estar guardat", token, TokenManager.getToken())

        // When
        TokenManager.clearToken()

        // Then
        assertNull("Token hauria de ser null després de clear", TokenManager.getToken())
    }
}









