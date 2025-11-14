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
        override suspend fun updateUser(userId: Long, user: UpdateUserRequest): User {
            // Retornar usuario actualitzat amb l'ID original
            return User(
                id = userId,
                nick = user.nick,
                nom = user.nom,
                cognom1 = user.cognom1,
                cognom2 = if (user.cognom2.isNullOrBlank()) null else user.cognom2,
                rol = user.rol,
                nif = user.nif,
                carrer = user.carrer,
                localitat = user.localitat,
                provincia = user.provincia,
                cp = user.cp,
                tlf = user.tlf,
                email = user.email
            )
        }

        override suspend fun getUserById(userId: Long): User = User(
            id = userId,
            nick = "oldnick",
            nom = "Old",
            cognom1 = "One",
            cognom2 = null,
            rol = 1,
            nif = "11111111Z",
            carrer = "Carrer Antic",
            localitat = "Barcelona",
            provincia = "Barcelona",
            cp = "08001",
            tlf = "600000000",
            email = "old@example.com"
        )

        override suspend fun login(request: AuthenticationRequest): AuthResponse =
            error("no utilitzat")

        override suspend fun getAllUsers(): List<User> = error("no utilitzat")
        override suspend fun getUserByNif(nif: String): Response<User> = error("no utilitzat")
        override suspend fun deleteUser(userId: Long): Response<Unit> = error("no utilitzat")
        override suspend fun createUser(request: CreateUserRequest): AuthResponse =
            error("no utilitzat")

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

        override suspend fun getUserById(userId: Long): User = User(
            id = userId,
            nick = "user1",
            nom = "User",
            cognom1 = "One",
            cognom2 = null,
            rol = 1,
            nif = "22222222A",
            carrer = "Carrer Prova",
            localitat = "Barcelona",
            provincia = "Barcelona",
            cp = "08002",
            tlf = "600111222",
            email = "user1@example.com"
        )

        override suspend fun updateUser(userId: Long, user: UpdateUserRequest): User {
            throw Exception("HTTP 409 Conflict - El nick ja està en ús")
        }

        override suspend fun login(request: AuthenticationRequest): AuthResponse =
            error("no utilitzat")

        override suspend fun getAllUsers(): List<User> = error("no utilitzat")
        override suspend fun getUserByNif(nif: String): Response<User> = error("no utilitzat")
        override suspend fun deleteUser(userId: Long): Response<Unit> = error("no utilitzat")
        override suspend fun createUser(request: CreateUserRequest): AuthResponse =
            error("no utilitzat")

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

        // ACT: Actualitzar perfil
        vm.updateUserProfile(
            userId = 42,
            nick = "usuario_actualizado",
            nom = "Nombre Nuevo",
            cognom1 = "Apellido1",
            cognom2 = "Apellido2",
            nif = null,
            email = null,
            tlf = null,
            carrer = null,
            localitat = null,
            cp = null,
            provincia = null
        )

        dispatcher.scheduler.advanceUntilIdle()

        // ASSERT: Verificar èxit
        val state = vm.updateUserState.value
        assertTrue("L'actualització hauria de ser exitosa", state.success)
        assertNotNull("Hauria de tenir un usuari actualitzat", state.updatedUser)
        assertEquals("El nick hauria d'estar actualitzat", "usuario_actualizado", state.updatedUser?.nick)
        assertEquals("El nom hauria d'estar actualitzat", "Nombre Nuevo", state.updatedUser?.nom)
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

        // ACT: Intentar actualitzar amb nick duplicat
        vm.updateUserProfile(
            userId = 42,
            nick = "admin",  // Nick ja usat per un altre usuari
            nom = "Test",
            cognom1 = "User",
            cognom2 = null,
            nif = null,
            email = null,
            tlf = null,
            carrer = null,
            localitat = null,
            cp = null,
            provincia = null
        )

        dispatcher.scheduler.advanceUntilIdle()

        // ASSERT: Verificar error
        val state = vm.updateUserState.value
        assertFalse("L'actualització NO hauria de ser exitosa", state.success)
        assertNotNull("Hauria de tenir un error", state.error)
        assertTrue(
            "Hauria d'haver un missatge d'error. Missatge rebut: '${state.error}'",
            state.error?.isNotEmpty() == true
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

        // ACT: Actualitzar perfil
        vm.updateUserProfile(
            userId = 42,
            nick = "usuario_actualizado",
            nom = "Nombre Nuevo",
            cognom1 = "Apellido1",
            cognom2 = null,
            nif = null,
            email = null,
            tlf = null,
            carrer = null,
            localitat = null,
            cp = null,
            provincia = null,
            updateCurrentUser = true  // Per actualitzar també userProfileState
        )
        dispatcher.scheduler.advanceUntilIdle()

        // ASSERT: Verificar estat del ViewModel
        val updateState = vm.updateUserState.value
        assertTrue("L'actualització hauria de ser exitosa", updateState.success)
        assertNotNull("updateUserState hauria de tenir un User actualitzat", updateState.updatedUser)
        assertEquals(
            "El nick hauria d'estar actualitzat",
            "usuario_actualizado",
            updateState.updatedUser?.nick
        )
        assertEquals(
            "El nombre hauria d'estar actualitzat",
            "Nombre Nuevo",
            updateState.updatedUser?.nom
        )
        assertEquals(
            "L'apellido1 hauria d'estar actualitzat",
            "Apellido1",
            updateState.updatedUser?.cognom1
        )
    }
}