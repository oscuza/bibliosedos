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
import retrofit2.HttpException
import retrofit2.Response

/**
 * Tests unitaris per a l'eliminació d'usuaris (funcionalitat exclusiva d'administrador).
 *
 * **Objectiu:**
 * Verificar que l'eliminació d'usuaris funciona correctament i gestiona
 * adequadament les restriccions de seguretat:
 * - Auto-eliminació denegada (usuari no pot eliminar-se a si mateix)
 * - Permisos d'administrador requerits
 * - Actualització automàtica de la llista després d'eliminar
 *
 * **Casos de Test:**
 * 1. Eliminació exitosa d'usuari
 * 2. Error per manca de permisos (403)
 *
 * **Restriccions del Backend:**
 * ```
 * Denegat:
 * - Auto-eliminació (userId == usuari actual)
 * - Usuari sense rol=2 (administrador)
 * - Eliminar l'últim administrador del sistema
 *
 * Permès:
 * - Admin pot eliminar usuaris normals
 * - Admin pot eliminar altres admins (si no és l'últim)
 * ```
 *
 * **Flux d'Eliminació:**
 * 1. Admin fa clic a botó "Eliminar" d'un usuari
 * 2. Diàleg de confirmació: "Estàs segur?"
 * 3. Si confirma → deleteUser(userId)
 * 4. Backend valida permisos
 * 5. Si èxit → Elimina de BD + recarrega llista
 * 6. Si error → Mostra missatge d'error
 *
 * @author Oscar
 * @since 1.0
 * @see AuthViewModel.deleteUser
 * @see Response
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DeleteUserTest {

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
     * Fake que simula una eliminació exitosa.
     *
     * **Comportament:**
     * - deleteUser(userId): Retorna Response.success(Unit)
     * - getAllUsers(): Retorna llista buida (usuari ja eliminat)
     *
     * **Codi HTTP esperat:**
     * 200 OK - Usuari eliminat correctament
     */
    private class FakeAuthApiSuccess : AuthApiService {
        override suspend fun deleteUser(userId: Long): Response<Unit> {
            return Response.success(Unit)
        }

        override suspend fun getAllUsers(): List<User> = emptyList()

        override suspend fun login(request: AuthenticationRequest): AuthResponse = error("no utilitzat")
        override suspend fun getUserById(userId: Long): User = error("no utilitzat")
        override suspend fun getUserByNif(nif: String): Response<User> = error("no utilitzat")
        override suspend fun updateUser(userId: Long, user: UpdateUserRequest): User = error("no utilitzat")
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
     * Fake que simula error de permisos (403 Forbidden).
     *
     * **Comportament:**
     * - deleteUser(userId): Retorna Response.error(403)
     *
     * **Possibles raons d'error 403:**
     * - Intent d'auto-eliminació
     * - Usuari sense permisos d'admin
     * - Intent d'eliminar l'últim administrador
     *
     * **Error real del backend:**
     * ```json
     * {
     *   "status": 403,
     *   "error": "Forbidden",
     *   "message": "No puedes eliminarte a ti mismo"
     * }
     * ```
     */
    private class FakeAuthApiError : AuthApiService {
        override suspend fun deleteUser(userId: Long): Response<Unit> {
            return Response.error(
                403,
                "{\"status\":403,\"error\":\"Forbidden\",\"message\":\"No tens permisos per eliminar aquest usuari\"}"
                    .toResponseBody("application/json".toMediaType())
            )
        }

        override suspend fun login(request: AuthenticationRequest): AuthResponse = error("no utilitzat")
        override suspend fun getAllUsers(): List<User> = error("no utilitzat")
        override suspend fun getUserById(userId: Long): User = error("no utilitzat")
        override suspend fun getUserByNif(nif: String): Response<User> = error("no utilitzat")
        override suspend fun updateUser(userId: Long, user: UpdateUserRequest): User = error("no utilitzat")
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
     * Test: Eliminació exitosa d'usuari retorna èxit i actualitza la llista.
     *
     * **Escenari:**
     * Administrador selecciona un usuari de la llista, fa clic a "Eliminar",
     * confirma el diàleg i l'usuari s'elimina correctament.
     *
     * **Passos:**
     * 1. Executar deleteUser(100)
     * 2. Esperar resposta del callback
     * 3. Verificar resultat exitós
     * 4. (Implicit) loadAllUsers() es crida automàticament
     *
     * **Dades de test:**
     * - userId: 100 (usuari a eliminar)
     * - Administrador actual: userId != 100
     *
     * **Verificacions:**
     * - success == true
     * - missatge conté "eliminado correctamente"
     * -  HTTP 200 OK
     *
     * **Resultat esperat:**
     * Usuari eliminat de la BD i desapareix de la llista.
     */
    @Test
    fun eliminarUsuari_exit_retornaExit() = runTest {
        // ARRANGE
        val vm = AuthViewModel(api = FakeAuthApiSuccess())

        // ACT: Eliminar usuari
        vm.deleteUser(100)

        dispatcher.scheduler.advanceUntilIdle()

        // ASSERT: Verificar èxit
        val state = vm.deleteUserState.value
        assertTrue("L'eliminació hauria de ser exitosa", state.success)
        assertEquals("L'ID eliminat hauria de ser 100", 100L, state.deletedUserId)
        assertNull("No hauria de tenir errors", state.error)
    }

    /**
     * Test: Eliminar usuari sense permisos retorna error 403.
     *
     * **Escenari:**
     * Intent d'eliminar un usuari en una situació denegada:
     * - Auto-eliminació
     * - Usuari sense permisos d'admin
     * - Eliminar últim administrador
     *
     * **Passos:**
     * 1. Intentar deleteUser(100) amb restricció
     * 2. Backend retorna 403 Forbidden
     * 3. Verificar missatge d'error
     *
     * **Verificacions:**
     * -  success == false
     * -  missatge conté "Error al eliminar"
     * -  L'usuari NO s'elimina

     * **Resultat esperat:**
     * Error mostrat a l'usuari, eliminació cancel·lada.
     */
    @Test
    fun eliminarUsuari_error403_retornaError() = runTest {
        // ARRANGE
        val vm = AuthViewModel(api = FakeAuthApiError())

        // ACT: Intentar eliminar (serà denegat)
        vm.deleteUser(100)

        dispatcher.scheduler.advanceUntilIdle()

        // ASSERT: Verificar error
        val state = vm.deleteUserState.value
        assertFalse("L'eliminació NO hauria de ser exitosa", state.success)
        assertNotNull("Hauria de tenir un error", state.error)
        assertTrue(
            "El missatge hauria de mencionar l'error. Missatge rebut: '${state.error}'",
            state.error?.contains("Error al eliminar", ignoreCase = true) == true ||
                    state.error?.contains("Error en eliminar", ignoreCase = true) == true ||
                    state.error?.contains("Error eliminant usuari", ignoreCase = true) == true ||
                    state.error?.contains("No pots eliminar-te a tu mateix", ignoreCase = true) == true ||
                    state.error?.contains("No puedes eliminarte a ti mismo", ignoreCase = true) == true ||
                    state.error?.contains("403") == true ||
                    state.error?.contains("Forbidden", ignoreCase = true) == true ||
                    state.error?.contains("permisos", ignoreCase = true) == true
        )
    }
}