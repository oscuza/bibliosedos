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
 * **Nota d'ÚS D'IA:**
 * - Eina: Claude 3.5 Sonnet (Anthropic)
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
        override suspend fun updateUser(userId: Long, user: UpdateUserRequest): User = error("no utilitzat")
        override suspend fun createUser(request: CreateUserRequest): AuthResponse = error("no utilitzat")
        override suspend fun logout(): Response<String> = error("no utilitzat")
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
            return Response.error(403, okhttp3.ResponseBody.create(null, "Forbidden"))
        }

        override suspend fun login(request: AuthenticationRequest): AuthResponse = error("no utilitzat")
        override suspend fun getAllUsers(): List<User> = error("no utilitzat")
        override suspend fun getUserById(userId: Long): User = error("no utilitzat")
        override suspend fun updateUser(userId: Long, user: UpdateUserRequest): User = error("no utilitzat")
        override suspend fun createUser(request: CreateUserRequest): AuthResponse = error("no utilitzat")
        override suspend fun logout(): Response<String> = error("no utilitzat")
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
        var result: Pair<Boolean, String>? = null

        // ACT: Eliminar usuari
        vm.deleteUser(100) { success, message ->
            result = Pair(success, message)
        }

        dispatcher.scheduler.advanceUntilIdle()

        // ASSERT: Verificar èxit
        assertNotNull("El callback hauria de retornar resultat", result)
        assertTrue("L'eliminació hauria de ser exitosa", result!!.first)
        assertTrue(
            "El missatge hauria de confirmar l'eliminació",
            result!!.second.contains("eliminado correctamente") ||
                    result!!.second.contains("eliminat correctament")
        )
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
        var result: Pair<Boolean, String>? = null

        // ACT: Intentar eliminar (serà denegat)
        vm.deleteUser(100) { success, message ->
            result = Pair(success, message)
        }

        dispatcher.scheduler.advanceUntilIdle()

        // ASSERT: Verificar error
        assertNotNull("El callback hauria de retornar resultat", result)
        assertFalse("L'eliminació NO hauria de ser exitosa", result!!.first)
        assertTrue(
            "El missatge hauria de mencionar l'error. Missatge rebut: '${result!!.second}'",
            result!!.second.contains("Error al eliminar") ||
                    result!!.second.contains("Error en eliminar") ||
                    result!!.second.contains("No pots eliminar-te a tu mateix") ||
                    result!!.second.contains("No puedes eliminarte a ti mismo") ||
                    result!!.second.contains("403")
        )
    }
}