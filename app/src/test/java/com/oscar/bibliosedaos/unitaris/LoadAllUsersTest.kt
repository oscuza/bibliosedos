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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response

/**
 * Tests unitaris per a la càrrega de la llista d'usuaris (funcionalitat admin).
 *
 * **Objectiu:**
 * Verificar que la càrrega de la llista d'usuaris funciona correctament en
 * diferents escenaris:
 * - Llista amb múltiples usuaris
 * - Llista buida (sistema nou)
 * - Error de xarxa o permisos
 *
 * **Casos de Test:**
 * 1. Càrrega exitosa amb múltiples usuaris
 * 2. Càrrega exitosa amb llista buida
 * 3. Error de xarxa o permisos (403)
 *
 * **Flux de Càrrega:**
 * ```
 * AdminHomeScreen.onCreate()
 *   ↓
 * AuthViewModel.loadAllUsers()
 *   ↓
 * allUsersState.isLoading = true
 *   ↓
 * AuthApiService.getAllUsers()
 *   ↓
 * Si èxit:
 *   allUsersState.users = llista
 *   allUsersState.isLoading = false
 *   allUsersState.error = null
 *
 * Si error:
 *   allUsersState.users = emptyList
 *   allUsersState.isLoading = false
 *   allUsersState.error = missatge
 * ```
 *
 * **Estats del UsersUiState:**
 * - isLoading: Boolean (indicador de càrrega)
 * - users: List<User> (llista d'usuaris)
 * - error: String? (missatge d'error si falla)
 *
 * @author Oscar
 * @since 1.0
 * @see AuthViewModel.loadAllUsers
 * @see UsersUiState
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LoadAllUsersTest {

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
     * Fake que simula una llista amb múltiples usuaris.
     *
     * **Comportament:**
     * - Retorna llista amb 3 usuaris:
     *   1. Admin (id=1, rol=2)
     *   2. User1 (id=2, rol=1)
     *   3. User2 (id=3, rol=1, amb segon cognom)
     *
     * **Utilitat:**
     * Verifica que el ViewModel pot processar i mostrar múltiples usuaris
     * amb diferents configuracions (amb/sense segon cognom, diferents rols).
     */
    private class FakeAuthApiSuccess : AuthApiService {
        override suspend fun getAllUsers(): List<User> = listOf(
            User(1, "admin", "Admin", "Test", null, 2),
            User(2, "user1", "Usuario", "Uno", null, 1),
            User(3, "user2", "Usuario", "Dos", "Apellido", 1)
        )

        override suspend fun login(request: AuthenticationRequest): AuthResponse = error("no utilitzat")
        override suspend fun getUserById(userId: Long): User = error("no utilitzat")
        override suspend fun getUserByNif(nif: String): Response<User> = error("no utilitzat")
        override suspend fun updateUser(userId: Long, user: UpdateUserRequest): User = error("no utilitzat")
        override suspend fun deleteUser(userId: Long): Response<Unit> = error("no utilitzat")
        override suspend fun createUser(request: CreateUserRequest): AuthResponse = error("no utilitzat")
        override suspend fun logout(): Response<String> = error("no utilitzat")
        
        // Mètodes de llibres, autors, exemplars i préstecs (no utilitzats en aquests tests)
        override suspend fun getAllLlibres(): List<com.oscar.bibliosedaos.data.models.Llibre> = error("no utilitzat")
        override suspend fun addLlibre(llibre: com.oscar.bibliosedaos.data.models.Llibre): com.oscar.bibliosedaos.data.models.Llibre = error("no utilitzat")
        override suspend fun updateLlibre(id: Long, llibre: com.oscar.bibliosedaos.data.models.Llibre): com.oscar.bibliosedaos.data.models.Llibre = error("no utilitzat")
        override suspend fun deleteLlibre(id: Long): Response<ResponseBody> = error("no utilitzat")
        override suspend fun getLlibreById(id: Long): com.oscar.bibliosedaos.data.models.Llibre = error("no utilitzat")
        override suspend fun getAllAutors(): List<com.oscar.bibliosedaos.data.models.Autor> = error("no utilitzat")
        override suspend fun addAutor(autor: com.oscar.bibliosedaos.data.models.Autor): com.oscar.bibliosedaos.data.models.Autor = error("no utilitzat")
        override suspend fun deleteAutor(id: Long): Response<ResponseBody> = error("no utilitzat")
        override suspend fun getAllExemplars(): List<com.oscar.bibliosedaos.data.models.Exemplar> = error("no utilitzat")
        override suspend fun getExemplarsLliures(titol: String?, autor: String?): List<com.oscar.bibliosedaos.data.models.Exemplar> = error("no utilitzat")
        override suspend fun addExemplar(exemplar: com.oscar.bibliosedaos.data.models.Exemplar): com.oscar.bibliosedaos.data.models.Exemplar = error("no utilitzat")
        override suspend fun updateExemplar(id: Long, exemplar: com.oscar.bibliosedaos.data.models.Exemplar): com.oscar.bibliosedaos.data.models.Exemplar = error("no utilitzat")
        override suspend fun deleteExemplar(id: Long): Response<ResponseBody> = error("no utilitzat")
        override suspend fun getExemplarById(id: Long): com.oscar.bibliosedaos.data.models.Exemplar = error("no utilitzat")
        override suspend fun getPrestecsActius(usuariId: Long?): List<com.oscar.bibliosedaos.data.models.Prestec> = error("no utilitzat")
        override suspend fun getAllPrestecs(usuariId: Long?): List<com.oscar.bibliosedaos.data.models.Prestec> = error("no utilitzat")
        override suspend fun createPrestec(prestec: com.oscar.bibliosedaos.data.models.CreatePrestecRequest): com.oscar.bibliosedaos.data.models.Prestec = error("no utilitzat")
        override suspend fun retornarPrestec(prestecId: Long?): Response<okhttp3.ResponseBody> = error("no utilitzat")
        
        // Mètodes de grups i horaris (no utilitzats en aquests tests)
        override suspend fun getAllHoraris(): List<com.oscar.bibliosedaos.data.models.Horari> = error("no utilitzat")
        override suspend fun createHorari(horari: com.oscar.bibliosedaos.data.models.Horari): com.oscar.bibliosedaos.data.models.Horari = error("no utilitzat")
        override suspend fun getAllGrups(): List<com.oscar.bibliosedaos.data.models.Grup> = error("no utilitzat")
        override suspend fun createGrup(grup: com.oscar.bibliosedaos.data.models.Grup): com.oscar.bibliosedaos.data.models.Grup = error("no utilitzat")
        override suspend fun deleteGrup(id: Long): Response<okhttp3.ResponseBody> = error("no utilitzat")
        override suspend fun addMemberToGrup(grupId: Long, membreId: Long): com.oscar.bibliosedaos.data.models.Grup = error("no utilitzat")
        override suspend fun getMembresGrup(grupId: Long): List<com.oscar.bibliosedaos.data.network.User> = error("no utilitzat")
        override suspend fun removeMemberFromGrup(grupId: Long, membreId: Long): Response<okhttp3.ResponseBody> = error("no utilitzat")
    }

    /**
     * Fake que simula un sistema sense usuaris (llista buida).
     *
     * **Comportament:**
     * - Retorna emptyList()
     *
     * **Utilitat:**
     * Verifica que el ViewModel gestiona correctament el cas de llista buida
     * sense llançar errors (null safety).
     *
     * **Cas real:**
     * Això normalment no passaria (sempre hi ha almenys 1 admin), però
     * és important verificar que no peta si passa.
     */
    private class FakeAuthApiEmpty : AuthApiService {
        override suspend fun getAllUsers(): List<User> = emptyList()

        override suspend fun login(request: AuthenticationRequest): AuthResponse = error("no utilitzat")
        override suspend fun getUserById(userId: Long): User = error("no utilitzat")
        override suspend fun getUserByNif(nif: String): Response<User> = error("no utilitzat")
        override suspend fun updateUser(userId: Long, user: UpdateUserRequest): User = error("no utilitzat")
        override suspend fun deleteUser(userId: Long): Response<Unit> = error("no utilitzat")
        override suspend fun createUser(request: CreateUserRequest): AuthResponse = error("no utilitzat")
        override suspend fun logout(): Response<String> = error("no utilitzat")
        
        // Mètodes de llibres, autors, exemplars i préstecs (no utilitzats en aquests tests)
        override suspend fun getAllLlibres(): List<com.oscar.bibliosedaos.data.models.Llibre> = error("no utilitzat")
        override suspend fun addLlibre(llibre: com.oscar.bibliosedaos.data.models.Llibre): com.oscar.bibliosedaos.data.models.Llibre = error("no utilitzat")
        override suspend fun updateLlibre(id: Long, llibre: com.oscar.bibliosedaos.data.models.Llibre): com.oscar.bibliosedaos.data.models.Llibre = error("no utilitzat")
        override suspend fun deleteLlibre(id: Long): Response<ResponseBody> = error("no utilitzat")
        override suspend fun getLlibreById(id: Long): com.oscar.bibliosedaos.data.models.Llibre = error("no utilitzat")
        override suspend fun getAllAutors(): List<com.oscar.bibliosedaos.data.models.Autor> = error("no utilitzat")
        override suspend fun addAutor(autor: com.oscar.bibliosedaos.data.models.Autor): com.oscar.bibliosedaos.data.models.Autor = error("no utilitzat")
        override suspend fun deleteAutor(id: Long): Response<ResponseBody> = error("no utilitzat")
        override suspend fun getAllExemplars(): List<com.oscar.bibliosedaos.data.models.Exemplar> = error("no utilitzat")
        override suspend fun getExemplarsLliures(titol: String?, autor: String?): List<com.oscar.bibliosedaos.data.models.Exemplar> = error("no utilitzat")
        override suspend fun addExemplar(exemplar: com.oscar.bibliosedaos.data.models.Exemplar): com.oscar.bibliosedaos.data.models.Exemplar = error("no utilitzat")
        override suspend fun updateExemplar(id: Long, exemplar: com.oscar.bibliosedaos.data.models.Exemplar): com.oscar.bibliosedaos.data.models.Exemplar = error("no utilitzat")
        override suspend fun deleteExemplar(id: Long): Response<ResponseBody> = error("no utilitzat")
        override suspend fun getExemplarById(id: Long): com.oscar.bibliosedaos.data.models.Exemplar = error("no utilitzat")
        override suspend fun getPrestecsActius(usuariId: Long?): List<com.oscar.bibliosedaos.data.models.Prestec> = error("no utilitzat")
        override suspend fun getAllPrestecs(usuariId: Long?): List<com.oscar.bibliosedaos.data.models.Prestec> = error("no utilitzat")
        override suspend fun createPrestec(prestec: com.oscar.bibliosedaos.data.models.CreatePrestecRequest): com.oscar.bibliosedaos.data.models.Prestec = error("no utilitzat")
        override suspend fun retornarPrestec(prestecId: Long?): Response<okhttp3.ResponseBody> = error("no utilitzat")
        
        // Mètodes de grups i horaris (no utilitzats en aquests tests)
        override suspend fun getAllHoraris(): List<com.oscar.bibliosedaos.data.models.Horari> = error("no utilitzat")
        override suspend fun createHorari(horari: com.oscar.bibliosedaos.data.models.Horari): com.oscar.bibliosedaos.data.models.Horari = error("no utilitzat")
        override suspend fun getAllGrups(): List<com.oscar.bibliosedaos.data.models.Grup> = error("no utilitzat")
        override suspend fun createGrup(grup: com.oscar.bibliosedaos.data.models.Grup): com.oscar.bibliosedaos.data.models.Grup = error("no utilitzat")
        override suspend fun deleteGrup(id: Long): Response<okhttp3.ResponseBody> = error("no utilitzat")
        override suspend fun addMemberToGrup(grupId: Long, membreId: Long): com.oscar.bibliosedaos.data.models.Grup = error("no utilitzat")
        override suspend fun getMembresGrup(grupId: Long): List<com.oscar.bibliosedaos.data.network.User> = error("no utilitzat")
        override suspend fun removeMemberFromGrup(grupId: Long, membreId: Long): Response<okhttp3.ResponseBody> = error("no utilitzat")
    }

    /**
     * Fake que simula un error de xarxa o permisos.
     *
     * **Comportament:**
     * - Llança Exception("HTTP 403 Forbidden")
     *
     * **Possibles errors reals:**
     * - 401 Unauthorized (token expirat)
     * - 403 Forbidden (usuari no és admin)
     * - 500 Internal Server Error (error del servidor)
     * - IOException (no hi ha connexió)
     */
    private class FakeAuthApiError : AuthApiService {
        override suspend fun getAllUsers(): List<User> {
            throw Exception("HTTP 403 Forbidden")
        }

        override suspend fun login(request: AuthenticationRequest): AuthResponse = error("no utilitzat")
        override suspend fun getUserById(userId: Long): User = error("no utilitzat")
        override suspend fun getUserByNif(nif: String): Response<User> = error("no utilitzat")
        override suspend fun updateUser(userId: Long, user: UpdateUserRequest): User = error("no utilitzat")
        override suspend fun deleteUser(userId: Long): Response<Unit> = error("no utilitzat")
        override suspend fun createUser(request: CreateUserRequest): AuthResponse = error("no utilitzat")
        override suspend fun logout(): Response<String> = error("no utilitzat")
        
        // Mètodes de llibres, autors, exemplars i préstecs (no utilitzats en aquests tests)
        override suspend fun getAllLlibres(): List<com.oscar.bibliosedaos.data.models.Llibre> = error("no utilitzat")
        override suspend fun addLlibre(llibre: com.oscar.bibliosedaos.data.models.Llibre): com.oscar.bibliosedaos.data.models.Llibre = error("no utilitzat")
        override suspend fun updateLlibre(id: Long, llibre: com.oscar.bibliosedaos.data.models.Llibre): com.oscar.bibliosedaos.data.models.Llibre = error("no utilitzat")
        override suspend fun deleteLlibre(id: Long): Response<ResponseBody> = error("no utilitzat")
        override suspend fun getLlibreById(id: Long): com.oscar.bibliosedaos.data.models.Llibre = error("no utilitzat")
        override suspend fun getAllAutors(): List<com.oscar.bibliosedaos.data.models.Autor> = error("no utilitzat")
        override suspend fun addAutor(autor: com.oscar.bibliosedaos.data.models.Autor): com.oscar.bibliosedaos.data.models.Autor = error("no utilitzat")
        override suspend fun deleteAutor(id: Long): Response<ResponseBody> = error("no utilitzat")
        override suspend fun getAllExemplars(): List<com.oscar.bibliosedaos.data.models.Exemplar> = error("no utilitzat")
        override suspend fun getExemplarsLliures(titol: String?, autor: String?): List<com.oscar.bibliosedaos.data.models.Exemplar> = error("no utilitzat")
        override suspend fun addExemplar(exemplar: com.oscar.bibliosedaos.data.models.Exemplar): com.oscar.bibliosedaos.data.models.Exemplar = error("no utilitzat")
        override suspend fun updateExemplar(id: Long, exemplar: com.oscar.bibliosedaos.data.models.Exemplar): com.oscar.bibliosedaos.data.models.Exemplar = error("no utilitzat")
        override suspend fun deleteExemplar(id: Long): Response<ResponseBody> = error("no utilitzat")
        override suspend fun getExemplarById(id: Long): com.oscar.bibliosedaos.data.models.Exemplar = error("no utilitzat")
        override suspend fun getPrestecsActius(usuariId: Long?): List<com.oscar.bibliosedaos.data.models.Prestec> = error("no utilitzat")
        override suspend fun getAllPrestecs(usuariId: Long?): List<com.oscar.bibliosedaos.data.models.Prestec> = error("no utilitzat")
        override suspend fun createPrestec(prestec: com.oscar.bibliosedaos.data.models.CreatePrestecRequest): com.oscar.bibliosedaos.data.models.Prestec = error("no utilitzat")
        override suspend fun retornarPrestec(prestecId: Long?): Response<okhttp3.ResponseBody> = error("no utilitzat")
        
        // Mètodes de grups i horaris (no utilitzats en aquests tests)
        override suspend fun getAllHoraris(): List<com.oscar.bibliosedaos.data.models.Horari> = error("no utilitzat")
        override suspend fun createHorari(horari: com.oscar.bibliosedaos.data.models.Horari): com.oscar.bibliosedaos.data.models.Horari = error("no utilitzat")
        override suspend fun getAllGrups(): List<com.oscar.bibliosedaos.data.models.Grup> = error("no utilitzat")
        override suspend fun createGrup(grup: com.oscar.bibliosedaos.data.models.Grup): com.oscar.bibliosedaos.data.models.Grup = error("no utilitzat")
        override suspend fun deleteGrup(id: Long): Response<okhttp3.ResponseBody> = error("no utilitzat")
        override suspend fun addMemberToGrup(grupId: Long, membreId: Long): com.oscar.bibliosedaos.data.models.Grup = error("no utilitzat")
        override suspend fun getMembresGrup(grupId: Long): List<com.oscar.bibliosedaos.data.network.User> = error("no utilitzat")
        override suspend fun removeMemberFromGrup(grupId: Long, membreId: Long): Response<okhttp3.ResponseBody> = error("no utilitzat")
    }

    // ========== TESTS ==========

    /**
     * Test: Càrrega exitosa retorna llista amb múltiples usuaris.
     *
     * **Escenari:**
     * Administrador obre la pantalla d'administració i es carrega la llista d'usuaris.
     *
     * **Passos:**
     * 1. Executar loadAllUsers()
     * 2. Avançar fins completar coroutines
     * 3. Verificar estat resultant
     *
     * **Verificacions:**
     * - isLoading == false (càrrega completada)
     * - users.size == 3 (3 usuaris retornats)
     * - error == null (sense errors)
     * - users conté les dades esperades
     *
     * **Resultat esperat:**
     * Llista d'usuaris carregada i mostrada a la UI.
     */
    @Test
    fun carregarUsuaris_exit_retornaLlista() = runTest {
        // ARRANGE
        val vm = AuthViewModel(api = FakeAuthApiSuccess())

        // ACT: Carregar llista d'usuaris
        vm.loadAllUsers()
        dispatcher.scheduler.advanceUntilIdle()

        // ASSERT: Verificar estat
        val state = vm.allUsersState.value

        assertFalse("isLoading hauria de ser false", state.isLoading)
        assertEquals("Hauria d'haver 3 usuaris", 3, state.users.size)
        assertNull("No hauria d'haver error", state.error)

        // Verificar contingut de la llista
        assertEquals("Primer usuari: admin", "admin", state.users[0].nick)
        assertEquals("Segon usuari: user1", "user1", state.users[1].nick)
        assertEquals("Tercer usuari: user2", "user2", state.users[2].nick)
    }

    /**
     * Test: Càrrega amb llista buida retorna emptyList sense errors.
     *
     * **Escenari:**
     * Sistema nou o tots els usuaris han estat eliminats (teòric).
     *
     * **Passos:**
     * 1. Executar loadAllUsers()
     * 2. Backend retorna emptyList()
     * 3. Verificar que no es llança error
     *
     * **Verificacions:**
     * - isLoading == false
     * - users.isEmpty() == true
     * - error == null
     *
     * **Importància:**
     * Verifica null safety: emptyList() no causa crashes.
     *
     * **Resultat esperat:**
     * Pantalla mostra "No hay usuarios" sense errors.
     */
    @Test
    fun carregarUsuaris_llistaBuida_retornaLlistaBuida() = runTest {
        // ARRANGE
        val vm = AuthViewModel(api = FakeAuthApiEmpty())

        // ACT: Carregar (retornarà buida)
        vm.loadAllUsers()
        dispatcher.scheduler.advanceUntilIdle()

        // ASSERT: Verificar llista buida sense errors
        val state = vm.allUsersState.value

        assertFalse("isLoading hauria de ser false", state.isLoading)
        assertTrue("La llista hauria d'estar buida", state.users.isEmpty())
        assertNull("No hauria d'haver error", state.error)
    }

    /**
     * Test: Error de xarxa o permisos mostra missatge d'error.
     *
     * **Escenari:**
     * - Token expirat (401)
     * - Usuari no és admin (403)
     * - No hi ha connexió a Internet
     * - Servidor caigut (500)
     *
     * **Passos:**
     * 1. Executar loadAllUsers()
     * 2. Backend llança Exception
     * 3. Verificar gestió d'error
     *
     * **Verificacions:**
     * - isLoading == false
     * - users.isEmpty() == true (no s'han carregat)
     * - error != null (missatge d'error present)
     * - error conté "Error al cargar usuarios"
     *
     * **Resultat esperat:**
     * Missatge d'error mostrat a la UI, llista buida.
     */
    @Test
    fun carregarUsuaris_error_mostraError() = runTest {
        // ARRANGE
        val vm = AuthViewModel(api = FakeAuthApiError())

        // ACT: Intentar carregar (fallarà)
        vm.loadAllUsers()
        dispatcher.scheduler.advanceUntilIdle()

        // ASSERT: Verificar gestió d'error
        val state = vm.allUsersState.value

        assertFalse("isLoading hauria de ser false", state.isLoading)
        assertTrue("La llista hauria d'estar buida", state.users.isEmpty())
        assertNotNull("Hauria d'haver un missatge d'error", state.error)
        assertTrue(
            "Hauria d'haver un missatge d'error. Missatge rebut: '${state.error}'",
            state.error!!.isNotEmpty()
        )
    }
}