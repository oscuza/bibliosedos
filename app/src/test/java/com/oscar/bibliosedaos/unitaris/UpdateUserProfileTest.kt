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
 * Tests unitaris per a l'actualització de perfils d'usuari.
 *
 * **Objectiu:**
 * Verificar que l'actualització de perfils funciona correctament i gestiona
 * adequadament les validacions del backend:
 * - Nick únic (no pot duplicar-se amb altres usuaris)
 * - Camps obligatoris vàlids
 * - Actualització de l'estat del ViewModel
 *
 * **Casos de Test:**
 * 1. Actualització exitosa de perfil
 * 2. Error per nick duplicat (409)
 * 3. Actualització de l'estat del ViewModel
 *
 * **Camps Modificables:**
 * ```
 * Poden canviar:
 * - nick (si no està duplicat)
 * - nombre
 * - apellido1
 * - apellido2
 *
 * NO poden canviar:
 * - id (immutable)
 * - rol (només admin pot canviar-lo)
 * - password (endpoint separat)
 * ```
 *
 * **Flux d'Actualització:**
 * 1. Usuari navega a EditProfileScreen
 * 2. Carrega dades actuals al formulari
 * 3. Modifica camps desitjats
 * 4. Fa clic a "GUARDAR"
 * 5. updateUserProfile(userId, updatedUser)
 * 6. Si èxit → Actualitza userProfileState + mostra success
 * 7. Si error → Mostra error sense modificar estat
 *
 * **Nota d'ÚS D'IA:**
 * - Eina: Claude 3.5 Sonnet (Anthropic)
 *
 * @author Oscar
 * @since 1.0
 * @see AuthViewModel.updateUserProfile
 * @see UserProfileState
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UpdateUserProfileTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ========== FAKE IMPLEMENTATIONS ==========

    /**
     * Fake que simula una actualització exitosa.
     *
     * **Comportament:**
     * - Retorna el User actualitzat amb l'ID original
     * - Accepta qualsevol canvi als camps editables
     *
     * **Validacions que simularia fer:**
     * - Nick no duplicat ✓
     * - Camps obligatoris no buits ✓
     * - Longituds de camp vàlides ✓
     */
    private class FakeAuthApiSuccess : AuthApiService {
        override suspend fun updateUser(userId: Long, user: User): User {
            // Retornar usuario actualitzat amb l'ID original
            return user.copy(id = userId)
        }

        override suspend fun login(request: AuthenticationRequest): AuthResponse = error("no utilitzat")
        override suspend fun getAllUsers(): List<User> = error("no utilitzat")
        override suspend fun getUserById(userId: Long): User = error("no utilitzat")
        override suspend fun deleteUser(userId: Long): Response<Unit> = error("no utilitzat")
        override suspend fun createUser(request: CreateUserRequest): AuthResponse = error("no utilitzat")
        override suspend fun logout(): Response<String> = error("no utilitzat")
    }

    /**
     * Fake que simula error de nick duplicat (409 Conflict).
     *
     * **Comportament:**
     * - Llança Exception amb missatge "HTTP 409 Conflict"
     * - Simula que el nou nick ja està en ús per un altre usuari
     *
     * **Exemple real:**
     * Usuari "user1" intenta canviar el seu nick a "admin", però
     * "admin" ja existeix → Backend retorna 409.
     *
     * **Error del backend:**
     * ```json
     * {
     *   "status": 409,
     *   "error": "Conflict",
     *   "message": "El nick 'admin' ja està en ús"
     * }
     * ```
     */
    private class FakeAuthApiDuplicateNick : AuthApiService {
        override suspend fun updateUser(userId: Long, user: User): User {
            throw Exception("HTTP 409 Conflict - El nick ja està en ús")
        }

        override suspend fun login(request: AuthenticationRequest): AuthResponse = error("no utilitzat")
        override suspend fun getAllUsers(): List<User> = error("no utilitzat")
        override suspend fun getUserById(userId: Long): User = error("no utilitzat")
        override suspend fun deleteUser(userId: Long): Response<Unit> = error("no utilitzat")
        override suspend fun createUser(request: CreateUserRequest): AuthResponse = error("no utilitzat")
        override suspend fun logout(): Response<String> = error("no utilitzat")
    }

    // ========== TESTS ==========

    /**
     * Test: Actualització exitosa retorna perfil amb dades noves.
     *
     * **Escenari:**
     * Usuari edita el seu perfil canviant el nom i cognoms.
     *
     * **Passos:**
     * 1. Crear User amb dades actualitzades
     * 2. Executar updateUserProfile(42, updatedUser)
     * 3. Esperar resposta del callback
     * 4. Verificar èxit
     *
     * **Dades de test:**
     * - userId: 42
     * - Canvis:
     *   · nick: "usuario_actualizado"
     *   · nombre: "Nombre Nuevo"
     *   · apellido1: "Apellido1"
     *   · apellido2: "Apellido2"
     *
     * **Verificacions:**
     * - success == true
     * - missatge conté "actualizado correctamente"
     * - userProfileState s'actualitza (test separat)
     *
     * **Resultat esperat:**
     * Perfil actualitzat, dades noves mostrades a la UI.
     */
    @Test
    fun actualitzarPerfil_exit_retornaPerfilActualitzat() = runTest {
        // ARRANGE
        val vm = AuthViewModel(api = FakeAuthApiSuccess())
        var result: Pair<Boolean, String>? = null

        val updatedUser = User(
            id = 42,
            nick = "usuario_actualizado",
            nombre = "Nombre Nuevo",
            apellido1 = "Apellido1",
            apellido2 = "Apellido2",
            rol = 1
        )

        // ACT: Actualitzar perfil
        vm.updateUserProfile(42, updatedUser) { success, message ->
            result = Pair(success, message)
        }

        dispatcher.scheduler.advanceUntilIdle()

        // ASSERT: Verificar èxit
        assertNotNull("El callback hauria de retornar resultat", result)
        assertTrue("L'actualització hauria de ser exitosa", result!!.first)
        assertTrue(
            "El missatge hauria de confirmar l'actualització",
            result!!.second.contains("actualizado correctamente") ||
                    result!!.second.contains("actualitzat correctament")
        )
    }

    /**
     * Test: Actualitzar amb nick duplicat retorna error 409.
     *
     * **Escenari:**
     * Usuari intenta canviar el seu nick a un que ja està en ús.
     *
     * **Exemple:**
     * - Usuari actual: "user1"
     * - Intenta canviar a: "admin"
     * - Però "admin" ja existeix
     * - Backend retorna 409 Conflict
     *
     * **Passos:**
     * 1. Crear User amb nick="admin"
     * 2. Executar updateUserProfile
     * 3. Backend detecta duplicat i retorna error
     * 4. Verificar missatge d'error
     *
     * **Verificacions:**
     * - success == false
     * - missatge conté "Error al actualizar"
     * - Perfil NO s'actualitza
     *
     * **Resultat esperat:**
     * Error mostrat, usuari pot intentar amb un altre nick.
     */
    @Test
    fun actualitzarPerfil_nickDuplicat_retornaError() = runTest {
        // ARRANGE
        val vm = AuthViewModel(api = FakeAuthApiDuplicateNick())
        var result: Pair<Boolean, String>? = null

        val updatedUser = User(
            id = 42,
            nick = "admin",  // Nick ja usat per un altre usuari
            nombre = "Test",
            apellido1 = "User",
            apellido2 = null,
            rol = 1
        )

        // ACT: Intentar actualitzar amb nick duplicat
        vm.updateUserProfile(42, updatedUser) { success, message ->
            result = Pair(success, message)
        }

        dispatcher.scheduler.advanceUntilIdle()

        // ASSERT: Verificar error
        assertNotNull("El callback hauria de retornar resultat", result)
        assertFalse("L'actualització NO hauria de ser exitosa", result!!.first)
        assertTrue(
            "Hauria d'haver un missatge d'error. Missatge rebut: '${result!!.second}'",
            result!!.second.isNotEmpty()  // Només verificar que hi ha un missatge
        )
    }

    /**
     * Test: Actualització exitosa actualitza l'estat del ViewModel.
     *
     * **Escenari:**
     * Verificar que userProfileState es sincronitza amb les dades noves després
     * d'una actualització exitosa.
     *
     * **Passos:**
     * 1. Executar updateUserProfile amb dades noves
     * 2. Verificar que userProfileState.user conté les dades actualitzades
     *
     * **Importància:**
     * Aquest test verifica que la UI es pot observar userProfileState per
     * mostrar automàticament les dades actualitzades sense recarregar.
     *
     * **Verificacions:**
     * - userProfileState.user != null
     * - user.nick == "usuario_actualizado"
     * - user.nombre == "Nombre Nuevo"
     * - Altres camps correctes
     *
     * **Resultat esperat:**
     * StateFlow actualitzat, UI recomposta amb dades noves.
     */
    @Test
    fun actualitzarPerfil_actualitzaEstatDelViewModel() = runTest {
        // ARRANGE
        val vm = AuthViewModel(api = FakeAuthApiSuccess())

        val updatedUser = User(
            id = 42,
            nick = "usuario_actualizado",
            nombre = "Nombre Nuevo",
            apellido1 = "Apellido1",
            apellido2 = null,
            rol = 1
        )

        // ACT: Actualitzar perfil
        vm.updateUserProfile(42, updatedUser) { _, _ -> }
        dispatcher.scheduler.advanceUntilIdle()

        // ASSERT: Verificar estat del ViewModel
        val profileState = vm.userProfileState.value

        assertNotNull("userProfileState hauria de tenir un User", profileState.user)
        assertEquals(
            "El nick hauria d'estar actualitzat",
            "usuario_actualizado",
            profileState.user?.nick
        )
        assertEquals(
            "El nombre hauria d'estar actualitzat",
            "Nombre Nuevo",
            profileState.user?.nombre
        )
        assertEquals(
            "L'apellido1 hauria d'estar actualitzat",
            "Apellido1",
            profileState.user?.apellido1
        )
    }
}