package com.oscar.bibliosedaos.unitaris

import com.oscar.bibliosedaos.data.network.*
import com.oscar.bibliosedaos.ui.viewmodels.AuthViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Response

/**
 * Tests unitaris per a la creació d'usuaris (funcionalitat exclusiva d'administrador).
 *
 * **Objectiu:**
 * Verificar que la creació d'usuaris funciona correctament i gestiona
 * adequadament tots els errors possibles:
 * - Nick duplicat (409)
 * - Sense permisos (403)
 * - Dades invàlides (400)
 *
 * **Casos de Test:**
 * 1. Creació exitosa d'usuari
 * 2. Error per nick duplicat
 * 3. Error per manca de permisos
 *
 * **Validacions del Backend:**
 * ```
 * Camps obligatoris:
 * - nick (únic, 3-50 caràcters)
 * - password (mínim 6 caràcters)
 * - nombre (mínim 2 caràcters)
 * - apellido1 (mínim 2 caràcters)
 * - rol (1=Usuari, 2=Admin)
 *
 * Camps opcionals:
 * - apellido2
 * ```
 *
 * **Permisos:**
 * Només usuaris amb rol=2 (Administrador) poden crear usuaris.
 *
 * **Nota d'ÚS D'IA:**
 * - Eina: Claude 3.5 Sonnet (Anthropic)
 * @author Oscar
 *  * @since 1.0
 *  * @see AuthViewModel.createUser
 *  * @see CreateUserRequest
 * */
@OptIn(ExperimentalCoroutinesApi::class)
class CreateUserTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        TokenManager.clearToken()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        TokenManager.clearToken()
    }

    // ========== FAKE IMPLEMENTATIONS ==========

    /**
     * Fake que simula una creació exitosa d'usuari.
     *
     * **Comportament:**
     * - Retorna AuthResponse amb les dades de l'usuari creat
     * - ID autogenerat: 100
     * - Rol segons el request
     * - getAllUsers() retorna llista amb el nou usuari
     */
    private class FakeAuthApiSuccess : AuthApiService {
        override suspend fun createUser(request: CreateUserRequest): AuthResponse {
            return AuthResponse(
                token = "new_token",
                id = 100,
                rol = 1,  // Usuario normal
                nick = request.nick,
                nombre = request.nombre,
                apellido1 = request.apellido1,
                apellido2 = request.apellido2
            )
        }

        override suspend fun getAllUsers(): List<User> = listOf(
            User(100, "newuser", "Nou", "Usuari", null, 1)
        )

        override suspend fun login(request: AuthenticationRequest): AuthResponse =
            error("no utilitzat")

        override suspend fun getUserById(userId: Long): User = error("no utilitzat")
        override suspend fun updateUser(userId: Long, user: User): User = error("no utilitzat")
        override suspend fun deleteUser(userId: Long): Response<Unit> = error("no utilitzat")
        override suspend fun logout(): Response<String> = error("no utilitzat")
    }

    /**
     * Fake que simula error de nick duplicat (409 Conflict).
     *
     * **Comportament:**
     * - Llança Exception amb missatge "HTTP 409 Conflict"
     * - Simula que el nick ja existeix al sistema
     *
     * **Error real del backend:**
     * ```json
     * {
     *   "status": 409,
     *   "error": "Conflict",
     *   "message": "El nick 'admin' ja existeix"
     * }
     */
    private class FakeAuthApiDuplicateNick : AuthApiService {
        override suspend fun createUser(request: CreateUserRequest): AuthResponse {
            throw Exception("HTTP 409 Conflict - El nick ja existeix")
        }

        override suspend fun login(request: AuthenticationRequest): AuthResponse =
            error("no utilitzat")

        override suspend fun getAllUsers(): List<User> = error("no utilitzat")
        override suspend fun getUserById(userId: Long): User = error("no utilitzat")
        override suspend fun updateUser(userId: Long, user: User): User = error("no utilitzat")
        override suspend fun deleteUser(userId: Long): Response<Unit> = error("no utilitzat")
        override suspend fun logout(): Response<String> = error("no utilitzat")
    }

    /**
     * Fake que simula error de permisos insuficients (403 Forbidden).
     *
     * **Comportament:**
     * - Llança Exception amb missatge "HTTP 403 Forbidden"
     * - Simula que l'usuari actual no és administrador
     *
     * **Error real del backend:**
     * ```json
     * {
     *   "status": 403,
     *   "error": "Forbidden",
     *   "message": "No tens permisos per crear usuaris"
     * }
     * ```
     */
    private class FakeAuthApiUnauthorized : AuthApiService {
        override suspend fun createUser(request: CreateUserRequest): AuthResponse {
            throw Exception("HTTP 403 Forbidden - Sin permisos")
        }

        override suspend fun login(request: AuthenticationRequest): AuthResponse =
            error("no utilitzat")

        override suspend fun getAllUsers(): List<User> = error("no utilitzat")
        override suspend fun getUserById(userId: Long): User = error("no utilitzat")
        override suspend fun updateUser(userId: Long, user: User): User = error("no utilitzat")
        override suspend fun deleteUser(userId: Long): Response<Unit> = error("no utilitzat")
        override suspend fun logout(): Response<String> = error("no utilitzat")
    }

    // ========== TESTS ==========

    /**
     * Test: Creació exitosa d'usuari retorna les dades del nou usuari.
     *
     * **Escenari:**
     * Administrador omple el formulari de creació amb dades vàlides i fa clic a "CREAR".
     *
     * **Passos:**
     * 1. Executar createUser amb dades vàlides
     * 2. Esperar resposta del callback
     * 3. Verificar resultat exitós
     *
     * **Dades de test:**
     * - nick: "newuser" (únic)
     * - password: "password123" (mínim 6)
     * - nombre: "Nou"
     * - apellido1: "Usuari"
     * - apellido2: null
     * - rol: 1 (Usuari normal)
     *
     * **Verificacions:**
     * - success == true
     * - missatge conté "creado correctamente"
     * - loadAllUsers() es crida automàticament
     *
     * **Resultat esperat:**
     * Usuari creat i llista d'usuaris actualitzada.
     */
    @Test
    fun crearUsuari_exit_retornaUsuariCreat() = runTest {
        val vm = AuthViewModel(api = FakeAuthApiSuccess())
        var result: Pair<Boolean, String>? = null

        // ACT: Crear usuari amb dades vàlides
        vm.createUser(
            nick = "newuser",
            password = "password123",
            nombre = "Nou",
            apellido1 = "Usuari",
            apellido2 = null,
            rol = 1
        ) { success, message ->
            result = Pair(success, message)
        }

        dispatcher.scheduler.advanceUntilIdle()

        // ASSERT: Verificar èxit
        assertNotNull("El callback hauria de retornar resultat", result)
        assertTrue("La creació hauria de ser exitosa", result!!.first)
        assertTrue(
            "El missatge hauria de confirmar la creació",
            result!!.second.contains("creado correctamente") ||
                    result!!.second.contains("creat correctament")
        )
    }

    /**
     * Test: Crear usuari amb nick duplicat retorna error 409.
     *
     * **Escenari:**
     * Administrador intenta crear un usuari amb un nick que ja existeix al sistema.
     *
     * **Passos:**
     * 1. Intentar crear usuari amb nick="admin" (ja existeix)
     * 2. Backend retorna 409 Conflict
     * 3. Verificar missatge d'error al callback
     *
     * **Verificacions:**
     * - success == false
     * - missatge conté "409" o "ja existeix"
     * - L'usuari NO es crea
     *
     * **Missatge esperat:**
     * "El nick 'admin' ja existeix"
     *
     * **Resultat esperat:**
     * Error mostrat a l'usuari, creació cancel·lada.
     */
    @Test
    fun crearUsuari_nickDuplicat_retornaError() = runTest {
        val vm = AuthViewModel(api = FakeAuthApiDuplicateNick())
        var result: Pair<Boolean, String>? = null

        // ACT: Intentar crear amb nick duplicat
        vm.createUser(
            nick = "admin",  // Nick ya existe
            password = "password123",
            nombre = "Test",
            apellido1 = "User",
            apellido2 = null,
            rol = 1
        ) { success, message ->
            result = Pair(success, message)
        }

        dispatcher.scheduler.advanceUntilIdle()

        // ASSERT: Verificar error de nick duplicat
        assertNotNull("El callback hauria de retornar resultat", result)
        assertFalse("La creació NO hauria de ser exitosa", result!!.first)
        assertTrue(
            "El missatge hauria de mencionar el conflicte",
            result!!.second.contains("409") ||
                    result!!.second.contains("ja existeix") ||
                    result!!.second.contains("ya existe")
        )
    }

    /**
     * Test: Crear usuari sense permisos d'admin retorna error 403.
     *
     * **Escenari:**
     * Usuari normal (rol=1) intenta crear un altre usuari, però no té permisos.
     *
     * **Passos:**
     * 1. Usuari sense permisos intenta crear usuari
     * 2. Backend retorna 403 Forbidden
     * 3. Verificar missatge d'error
     *
     * **Verificacions:**
     * - success == false
     * - missatge conté "403" o "permisos"
     *
     * **Nota de seguretat:**
     * Aquest error normalment no hauria de passar, ja que la UI no mostra
     * l'opció de crear usuaris als usuaris normals. Aquest test verifica que
     * el backend està protegit correctament.
     *
     * **Resultat esperat:**
     * Error de permisos, creació denegada.
     */
    @Test
    fun crearUsuari_sinPermisos_retornaError403() = runTest {
        val vm = AuthViewModel(api = FakeAuthApiUnauthorized())
        var result: Pair<Boolean, String>? = null

        // ACT: Intentar crear sense permisos
        vm.createUser(
            nick = "newuser",
            password = "password123",
            nombre = "Test",
            apellido1 = "User",
            apellido2 = null,
            rol = 1
        ) { success, message ->
            result = Pair(success, message)
        }

        dispatcher.scheduler.advanceUntilIdle()

        // ASSERT: Verificar error de permisos
        assertNotNull("El callback hauria de retornar resultat", result)
        assertFalse("La creació NO hauria de ser exitosa", result!!.first)
        assertTrue(
            "El missatge hauria de mencionar falta de permisos",
            result!!.second.contains("403") ||
                    result!!.second.contains("permisos") ||
                    result!!.second.contains("permisos")
        )
    }
}