package com.oscar.bibliosedaos.unitaris

import com.oscar.bibliosedaos.data.network.*
import com.oscar.bibliosedaos.ui.viewmodels.AuthViewModel
import com.oscar.bibliosedaos.ui.viewmodels.LoginUiState
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
 * Tests unitaris per al tancament de sessió (logout).
 * **Nota d'ÚS D'IA:**
 *  * - Eina: Claude 3.5 Sonnet (Anthropic)
 *
 * **Objectiu:**
 * Verificar que el procés de logout funciona correctament en diferents escenaris:
 * - Intenta notificar al servidor
 * - Neteja SEMPRE l'estat local independentment de si el servidor respon
 * - Reseteja tots els estats del ViewModel
 *
 * **Casos de Test:**
 * 1. Logout exitós amb servidor disponible
 * 2. Logout local quan el servidor no respon
 * 3. Reset complet de tots els estats del ViewModel
 *
 * **Importància:**
 * El logout ha de funcionar fins i tot si:
 * - No hi ha connexió a Internet
 * - El servidor està caigut
 * - El token ja ha expirat
 *
 *
 * @author Oscar
 * @since 1.0
 * @see AuthViewModel.logout
 * @see TokenManager.clearToken
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LogoutTest {

    /**
     * Dispatcher de test per controlar coroutines.
     */
    private val dispatcher = StandardTestDispatcher()

    /**
     * Configuració abans de cada test.
     */
    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        TokenManager.clearToken()
    }

    /**
     * Neteja després de cada test.
     */
    @After
    fun tearDown() {
        Dispatchers.resetMain()
        TokenManager.clearToken()
    }

    // ========== FAKE IMPLEMENTATIONS ==========

    /**
     * Fake que simula un logout exitós al servidor.
     *
     * **Comportament:**
     * - logout(): Retorna Response.success amb missatge de confirmació
     * - login(): Retorna dades mock per tests auxiliars
     */
    private class FakeAuthApiSuccess : AuthApiService {
        override suspend fun logout(): Response<String> {
            return Response.success("Sessió tancada")
        }

        override suspend fun login(request: AuthenticationRequest): AuthResponse =
            AuthResponse("token", 1, 1, "user", "User", "Test", null)

        override suspend fun getAllUsers(): List<User> = error("no utilitzat")
        override suspend fun getUserById(userId: Long): User = error("no utilitzat")
        override suspend fun updateUser(userId: Long, user: UpdateUserRequest): User = error("no utilitzat")
        override suspend fun deleteUser(userId: Long): Response<Unit> = error("no utilitzat")
        override suspend fun createUser(request: CreateUserRequest): AuthResponse =
            error("no utilitzat")
    }

    /**
     * Fake que simula un error de xarxa durant el logout.
     *
     * **Comportament:**
     * - logout(): Llança Exception simulant error de connexió
     * - Permet testejar el patró "best effort" (neteja local igualment)
     */
    private class FakeAuthApiError : AuthApiService {
        override suspend fun logout(): Response<String> {
            throw Exception("Network error")
        }

        override suspend fun login(request: AuthenticationRequest): AuthResponse =
            AuthResponse("token", 1, 1, "user", "User", "Test", null)

        override suspend fun getAllUsers(): List<User> = error("no utilitzat")
        override suspend fun getUserById(userId: Long): User = error("no utilitzat")
        override suspend fun updateUser(userId: Long, user: UpdateUserRequest): User = error("no utilitzat")
        override suspend fun deleteUser(userId: Long): Response<Unit> = error("no utilitzat")
        override suspend fun createUser(request: CreateUserRequest): AuthResponse =
            error("no utilitzat")
    }

    // ========== TESTS ==========

    /**
     * Test: Logout exitós neteja tot l'estat local.
     *
     * **Escenari:**
     * Usuari ha fet login i després fa clic a "Logout" amb servidor disponible.
     *
     * **Passos:**
     * 1. Simular login previ (guardar token)
     * 2. Executar logout amb servidor funcionant
     * 3. Verificar que tot l'estat s'ha netejat
     *
     * **Verificacions:**
     * -  TokenManager.getToken() == null
     * -  loginUiState.loginSuccess == false
     * -  loginUiState.authResponse == null
     *
     * **Resultat esperat:**
     * Estat completament resetejat, sessió tancada.
     */
    @Test
    fun logout_exit_netejaTotElEstat() = runTest {
        // ARRANGE: Simular sessió activa
        val vm = AuthViewModel(api = FakeAuthApiSuccess())
        TokenManager.saveToken("test_token")

        // ACT: Executar logout
        vm.logout()
        dispatcher.scheduler.advanceUntilIdle()

        // ASSERT: Verificar neteja completa
        assertNull("Token hauria d'estar eliminat", TokenManager.getToken())
        assertFalse("loginSuccess hauria de ser false", vm.loginUiState.value.loginSuccess)
        assertNull("authResponse hauria de ser null", vm.loginUiState.value.authResponse)
    }

    /**
     * Test: Logout neteja l'estat local encara que el servidor falli.
     *
     * **Escenari:**
     * Usuari fa logout però el servidor no respon (no hi ha Internet, servidor caigut, etc).
     *
     * **Passos:**
     * 1. Simular login previ
     * 2. Executar logout amb servidor que llança error
     * 3. Verificar que la neteja local s'ha fet igualment
     *
     * **Verificacions:**
     * - ✅ TokenManager.getToken() == null (netejat localment)
     * - ✅ loginSuccess == false
     * - ✅ No es mostra error a l'usuari (patró best effort)
     *
     * **Importància:**
     * Aquest test verifica el patró "best effort": encara que falli la comunicació
     * amb el servidor, l'usuari ha de poder tancar sessió localment per seguretat.
     *
     * **Resultat esperat:**
     * Estat local netejat independentment de l'error del servidor.
     */
    @Test
    fun logout_errorServidor_netejaDeTotesManeres() = runTest {
        // ARRANGE: Simular sessió activa amb servidor que fallarà
        val vm = AuthViewModel(api = FakeAuthApiError())
        TokenManager.saveToken("test_token")

        // ACT: Executar logout (servidor llançarà error)
        vm.logout()
        dispatcher.scheduler.advanceUntilIdle()

        // ASSERT: Verificar neteja local malgrat l'error
        assertNull(
            "Token hauria d'estar eliminat encara que el servidor hagi fallat",
            TokenManager.getToken()
        )
        assertFalse(
            "loginSuccess hauria de ser false",
            vm.loginUiState.value.loginSuccess
        )
    }

    /**
     * Test: Logout reseteja completament tots els estats del ViewModel.
     *
     * **Escenari:**
     * Verificar que TOTS els camps de LoginUiState tornen al seu valor inicial.
     *
     * **Passos:**
     * 1. Fer login complet (estat amb dades)
     * 2. Executar logout
     * 3. Verificar cada camp de l'estat
     *
     * **Verificacions:**
     * - ✅ TokenManager.getToken() == null
     * - ✅ isLoading == false
     * - ✅ loginSuccess == false
     * - ✅ authResponse == null
     * - ✅ error == null
     *
     * **Importància:**
     * Assegura que no queda cap "rastre" de la sessió anterior després del logout.
     * Això és crític per seguretat i per evitar bugs en futures sessions.
     *
     * **Resultat esperat:**
     * LoginUiState === LoginUiState() (estat inicial)
     */
    @Test
    fun logout_netejaTotElEstatDelViewModel() = runTest {
        // ARRANGE: Crear sessió completa amb dades
        val vm = AuthViewModel(api = FakeAuthApiSuccess())
        vm.login("user", "pass")
        dispatcher.scheduler.advanceUntilIdle()
        TokenManager.saveToken("test_token")

        // ACT: Executar logout
        vm.logout()
        dispatcher.scheduler.advanceUntilIdle()

        // ASSERT: Verificar que TOTS els camps estan resetejats
        val state = vm.loginUiState.value

        assertNull("Token eliminat", TokenManager.getToken())
        assertFalse("isLoading = false", state.isLoading)
        assertFalse("loginSuccess = false", state.loginSuccess)
        assertNull("authResponse = null", state.authResponse)
        assertNull("error = null", state.error)

        // Verificar que l'estat és equivalent a l'estat inicial
        assertEquals(
            "L'estat hauria de ser equivalent a LoginUiState()",
            LoginUiState(),
            state
        )
    }
}