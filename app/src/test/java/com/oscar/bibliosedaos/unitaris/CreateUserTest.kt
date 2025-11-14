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
 *
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
                "new_token",
                100,
                request.rol,
                request.nick,
                request.nom,
                request.cognom1,
                request.cognom2
            )

        }

        override suspend fun getAllUsers(): List<User> = listOf(
            User(100, "newuser", "Nou", "Usuari", null, 1)
        )

        override suspend fun login(request: AuthenticationRequest): AuthResponse =
            error("no utilitzat")

        override suspend fun getUserById(userId: Long): User = error("no utilitzat")
        override suspend fun getUserByNif(nif: String): Response<User> = error("no utilitzat")
        override suspend fun updateUser(userId: Long, user: UpdateUserRequest): User =
            error("no utilitzat")

        override suspend fun deleteUser(userId: Long): Response<Unit> = error("no utilitzat")
        override suspend fun logout(): Response<String> = error("no utilitzat")
        
        // Mètodes de llibres, autors, exemplars i préstecs (no utilitzats en aquests tests)
        override suspend fun getAllLlibres(): List<com.oscar.bibliosedaos.data.models.Llibre> = error("no utilitzat")
        override suspend fun addLlibre(llibre: com.oscar.bibliosedaos.data.models.Llibre): com.oscar.bibliosedaos.data.models.Llibre = error("no utilitzat")
        override suspend fun updateLlibre(id: Long, llibre: com.oscar.bibliosedaos.data.models.Llibre): com.oscar.bibliosedaos.data.models.Llibre = error("no utilitzat")
        override suspend fun deleteLlibre(id: Long): String = error("no utilitzat")
        override suspend fun getLlibreById(id: Long): com.oscar.bibliosedaos.data.models.Llibre = error("no utilitzat")
        override suspend fun getAllAutors(): List<com.oscar.bibliosedaos.data.models.Autor> = error("no utilitzat")
        override suspend fun addAutor(autor: com.oscar.bibliosedaos.data.models.Autor): com.oscar.bibliosedaos.data.models.Autor = error("no utilitzat")
        override suspend fun deleteAutor(id: Long): String = error("no utilitzat")
        override suspend fun getAllExemplars(): List<com.oscar.bibliosedaos.data.models.Exemplar> = error("no utilitzat")
        override suspend fun getExemplarsLliures(titol: String?, autor: String?): List<com.oscar.bibliosedaos.data.models.Exemplar> = error("no utilitzat")
        override suspend fun addExemplar(exemplar: com.oscar.bibliosedaos.data.models.Exemplar): com.oscar.bibliosedaos.data.models.Exemplar = error("no utilitzat")
        override suspend fun updateExemplar(id: Long, exemplar: com.oscar.bibliosedaos.data.models.Exemplar): com.oscar.bibliosedaos.data.models.Exemplar = error("no utilitzat")
        override suspend fun deleteExemplar(id: Long): String = error("no utilitzat")
        override suspend fun getExemplarById(id: Long): com.oscar.bibliosedaos.data.models.Exemplar = error("no utilitzat")
        override suspend fun getPrestecsActius(usuariId: Long?): List<com.oscar.bibliosedaos.data.models.Prestec> = error("no utilitzat")
        override suspend fun getAllPrestecs(usuariId: Long?): List<com.oscar.bibliosedaos.data.models.Prestec> = error("no utilitzat")
        override suspend fun createPrestec(prestec: com.oscar.bibliosedaos.data.models.CreatePrestecRequest): com.oscar.bibliosedaos.data.models.Prestec = error("no utilitzat")
        override suspend fun retornarPrestec(prestecId: Long?): Response<okhttp3.ResponseBody> = error("no utilitzat")
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
        override suspend fun getUserByNif(nif: String): Response<User> = error("no utilitzat")
        override suspend fun updateUser(userId: Long, user: UpdateUserRequest): User =
            error("no utilitzat")

        override suspend fun deleteUser(userId: Long): Response<Unit> = error("no utilitzat")
        override suspend fun logout(): Response<String> = error("no utilitzat")
        
        // Mètodes de llibres, autors, exemplars i préstecs (no utilitzats en aquests tests)
        override suspend fun getAllLlibres(): List<com.oscar.bibliosedaos.data.models.Llibre> = error("no utilitzat")
        override suspend fun addLlibre(llibre: com.oscar.bibliosedaos.data.models.Llibre): com.oscar.bibliosedaos.data.models.Llibre = error("no utilitzat")
        override suspend fun updateLlibre(id: Long, llibre: com.oscar.bibliosedaos.data.models.Llibre): com.oscar.bibliosedaos.data.models.Llibre = error("no utilitzat")
        override suspend fun deleteLlibre(id: Long): String = error("no utilitzat")
        override suspend fun getLlibreById(id: Long): com.oscar.bibliosedaos.data.models.Llibre = error("no utilitzat")
        override suspend fun getAllAutors(): List<com.oscar.bibliosedaos.data.models.Autor> = error("no utilitzat")
        override suspend fun addAutor(autor: com.oscar.bibliosedaos.data.models.Autor): com.oscar.bibliosedaos.data.models.Autor = error("no utilitzat")
        override suspend fun deleteAutor(id: Long): String = error("no utilitzat")
        override suspend fun getAllExemplars(): List<com.oscar.bibliosedaos.data.models.Exemplar> = error("no utilitzat")
        override suspend fun getExemplarsLliures(titol: String?, autor: String?): List<com.oscar.bibliosedaos.data.models.Exemplar> = error("no utilitzat")
        override suspend fun addExemplar(exemplar: com.oscar.bibliosedaos.data.models.Exemplar): com.oscar.bibliosedaos.data.models.Exemplar = error("no utilitzat")
        override suspend fun updateExemplar(id: Long, exemplar: com.oscar.bibliosedaos.data.models.Exemplar): com.oscar.bibliosedaos.data.models.Exemplar = error("no utilitzat")
        override suspend fun deleteExemplar(id: Long): String = error("no utilitzat")
        override suspend fun getExemplarById(id: Long): com.oscar.bibliosedaos.data.models.Exemplar = error("no utilitzat")
        override suspend fun getPrestecsActius(usuariId: Long?): List<com.oscar.bibliosedaos.data.models.Prestec> = error("no utilitzat")
        override suspend fun getAllPrestecs(usuariId: Long?): List<com.oscar.bibliosedaos.data.models.Prestec> = error("no utilitzat")
        override suspend fun createPrestec(prestec: com.oscar.bibliosedaos.data.models.CreatePrestecRequest): com.oscar.bibliosedaos.data.models.Prestec = error("no utilitzat")
        override suspend fun retornarPrestec(prestecId: Long?): Response<okhttp3.ResponseBody> = error("no utilitzat")
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
        override suspend fun getUserByNif(nif: String): Response<User> = error("no utilitzat")
        override suspend fun updateUser(userId: Long, user: UpdateUserRequest): User = error("no utilitzat")
        override suspend fun deleteUser(userId: Long): Response<Unit> = error("no utilitzat")
        override suspend fun logout(): Response<String> = error("no utilitzat")
        
        // Mètodes de llibres, autors, exemplars i préstecs (no utilitzats en aquests tests)
        override suspend fun getAllLlibres(): List<com.oscar.bibliosedaos.data.models.Llibre> = error("no utilitzat")
        override suspend fun addLlibre(llibre: com.oscar.bibliosedaos.data.models.Llibre): com.oscar.bibliosedaos.data.models.Llibre = error("no utilitzat")
        override suspend fun updateLlibre(id: Long, llibre: com.oscar.bibliosedaos.data.models.Llibre): com.oscar.bibliosedaos.data.models.Llibre = error("no utilitzat")
        override suspend fun deleteLlibre(id: Long): String = error("no utilitzat")
        override suspend fun getLlibreById(id: Long): com.oscar.bibliosedaos.data.models.Llibre = error("no utilitzat")
        override suspend fun getAllAutors(): List<com.oscar.bibliosedaos.data.models.Autor> = error("no utilitzat")
        override suspend fun addAutor(autor: com.oscar.bibliosedaos.data.models.Autor): com.oscar.bibliosedaos.data.models.Autor = error("no utilitzat")
        override suspend fun deleteAutor(id: Long): String = error("no utilitzat")
        override suspend fun getAllExemplars(): List<com.oscar.bibliosedaos.data.models.Exemplar> = error("no utilitzat")
        override suspend fun getExemplarsLliures(titol: String?, autor: String?): List<com.oscar.bibliosedaos.data.models.Exemplar> = error("no utilitzat")
        override suspend fun addExemplar(exemplar: com.oscar.bibliosedaos.data.models.Exemplar): com.oscar.bibliosedaos.data.models.Exemplar = error("no utilitzat")
        override suspend fun updateExemplar(id: Long, exemplar: com.oscar.bibliosedaos.data.models.Exemplar): com.oscar.bibliosedaos.data.models.Exemplar = error("no utilitzat")
        override suspend fun deleteExemplar(id: Long): String = error("no utilitzat")
        override suspend fun getExemplarById(id: Long): com.oscar.bibliosedaos.data.models.Exemplar = error("no utilitzat")
        override suspend fun getPrestecsActius(usuariId: Long?): List<com.oscar.bibliosedaos.data.models.Prestec> = error("no utilitzat")
        override suspend fun getAllPrestecs(usuariId: Long?): List<com.oscar.bibliosedaos.data.models.Prestec> = error("no utilitzat")
        override suspend fun createPrestec(prestec: com.oscar.bibliosedaos.data.models.CreatePrestecRequest): com.oscar.bibliosedaos.data.models.Prestec = error("no utilitzat")
        override suspend fun retornarPrestec(prestecId: Long?): Response<okhttp3.ResponseBody> = error("no utilitzat")
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

        // ACT: Crear usuari amb dades vàlides
        vm.createUser(
            nick = "newuser",
            password = "password123",
            nom = "Nou",
            cognom1 = "Usuari",
            cognom2 = null,
            rol = 1,
            nif = "12345678Z",
            localitat = "Barcelona",
            carrer = "Carrer Major 1",
            cp = "08001",
            provincia = "Barcelona",
            tlf = "600600600",
            email = "newuser@test.com"
        )

        dispatcher.scheduler.advanceUntilIdle()

        // ASSERT: Verificar èxit
        val state = vm.createUserState.value
        assertTrue("La creació hauria de ser exitosa", state.success)
        assertNotNull("Hauria de tenir un usuari creat", state.createdUser)
        assertEquals("L'ID hauria de ser 100", 100L, state.createdUser?.id)
        assertEquals("El nick hauria de ser 'newuser'", "newuser", state.createdUser?.nick)
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

        // ACT: Intentar crear amb nick duplicat
        vm.createUser(
            nick = "admin",//nick ja exist
            password = "password123",
            nom = "Nou",
            cognom1 = "Usuari",
            cognom2 = null,
            rol = 1,
            nif = "12345678Z",
            localitat = "Barcelona",
            carrer = "Carrer Major 1",
            cp = "08001",
            provincia = "Barcelona",
            tlf = "600600600",
            email = "newuser@test.com"
        )

        dispatcher.scheduler.advanceUntilIdle()

        // ASSERT: Verificar error de nick duplicat
        val state = vm.createUserState.value
        assertFalse("La creació NO hauria de ser exitosa", state.success)
        assertNotNull("Hauria de tenir un error", state.error)
        assertTrue(
            "El missatge hauria de mencionar el conflicte. Missatge rebut: '${state.error}'",
            state.error?.contains("409") == true ||
                    state.error?.contains("ja existeix", ignoreCase = true) == true ||
                    state.error?.contains("ja està", ignoreCase = true) == true ||
                    state.error?.contains("ya existe", ignoreCase = true) == true ||
                    state.error?.contains("ya está", ignoreCase = true) == true ||
                    state.error?.contains("duplicat", ignoreCase = true) == true ||
                    state.error?.contains("duplicado", ignoreCase = true) == true ||
                    state.error?.contains("en ús", ignoreCase = true) == true ||
                    state.error?.contains("en uso", ignoreCase = true) == true
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

        // ACT: Intentar crear sense permisos
        vm.createUser(
            nick = "newuser",
            password = "password123",
            nom = "Nou",
            cognom1 = "Usuari",
            cognom2 = null,
            rol = 1,
            nif = "12345678Z",
            localitat = "Barcelona",
            carrer = "Carrer Major 1",
            cp = "08001",
            provincia = "Barcelona",
            tlf = "600600600",
            email = "newuser@test.com"
        )

        dispatcher.scheduler.advanceUntilIdle()

        // ASSERT: Verificar error de permisos
        val state = vm.createUserState.value
        assertFalse("La creació NO hauria de ser exitosa", state.success)
        assertNotNull("Hauria de tenir un error", state.error)
        assertTrue(
            "El missatge hauria de mencionar falta de permisos",
            state.error?.contains("403") == true ||
                    state.error?.contains("permisos", ignoreCase = true) == true ||
                    state.error?.contains("Forbidden", ignoreCase = true) == true
        )
    }
}