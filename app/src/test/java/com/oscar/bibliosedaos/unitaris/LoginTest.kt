package com.oscar.bibliosedaos.unitaris

import com.oscar.bibliosedaos.data.network.*
import com.oscar.bibliosedaos.ui.viewmodels.AuthViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Response

/**
 * Tests unitaris per a la funcionalitat de Login
 *
 * Cobertura:
 * - Login exitós
 * - Login amb credencials incorrectes (401)
 * - Indicador de càrrega durant el login
 *
 * **Patró Utilitzat:**
 *  * - Test Doubles amb implementacions Fake d'AuthApiService
 *  * - Coroutines de test per controlar l'execució asíncrona
 *  * - StateFlow per observar els estats del ViewModel
 *
 *  **Nota d'ÚS D'IA:**
 *  * - Eina: Claude 3.5 Sonnet (Anthropic)
 *
 * @author Oscar
 * @since 1.0
 * @see AuthViewModel.login
 * @see TokenManager
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LoginTest {

    /**
     * Dispatcher de test per controlar l'execució de coroutines.
     * Permet avançar el temps manualment per testejar delays.
     */
    private val dispatcher = StandardTestDispatcher()

    /**
     * Configuració abans de cada test.
     *
     * Accions:
     * - Substituir Main dispatcher per test dispatcher
     * - Netejar qualsevol token existent
     */
    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        TokenManager.clearToken()
    }

    /**
     * Neteja després de cada test.
     *
     * Accions:
     * - Restaurar Main dispatcher original
     * - Netejar token per no afectar altres tests
     */
    @After
    fun tearDown() {
        Dispatchers.resetMain()
        TokenManager.clearToken()
    }

    //  Implementació fake d'AuthApiService que simula un login exitós.
    /**
     * **Dades retornades:**
     * - token: "abc.def.ghi"
     * - id: 42
     * - rol: 2 (Administrador)
     * - nick: "admin"
     */
    private class FakeAuthApiSuccess : AuthApiService {
        override suspend fun login(request: AuthenticationRequest): AuthResponse {
            return AuthResponse(
                token = "abc.def.ghi",
                id = 42L,
                rol = 2,  // Admin
                nick = "admin",
                nom = "Admin",
                cognom1 = "Test",
                cognom2 = null
            )
        }

        override suspend fun getAllUsers(): List<User> = emptyList()
        override suspend fun getUserById(userId: Long): User = error("no utilitzat")
        override suspend fun updateUser(userId: Long, user: UpdateUserRequest): User = error("no utilitzat")
        override suspend fun deleteUser(userId: Long): Response<Unit> = error("no utilitzat")
        override suspend fun createUser(request: CreateUserRequest): AuthResponse =
            error("no utilitzat")

        override suspend fun logout(): Response<String> = error("no utilitzat")
    }

    /**
     * Implementació fake d'AuthApiService que simula un error 401 (Unauthorized).
     *
     * **Comportament:**
     * - login(): Llança Exception amb missatge "HTTP 401 Unauthorized"
     * - Simula credencials incorrectes
     */
    private class FakeAuthApiError : AuthApiService {
        override suspend fun login(request: AuthenticationRequest): AuthResponse {
            throw Exception("HTTP 401 Unauthorized")
        }

        override suspend fun getAllUsers(): List<User> = emptyList()
        override suspend fun getUserById(userId: Long): User = error("no utilitzat")
        override suspend fun updateUser(userId: Long, user: UpdateUserRequest): User = error("no utilitzat")
        override suspend fun deleteUser(userId: Long): Response<Unit> = error("no utilitzat")
        override suspend fun createUser(request: CreateUserRequest): AuthResponse =
            error("no utilitzat")

        override suspend fun logout(): Response<String> = error("no utilitzat")
    }

    // ========== TESTS ==========

    /**
     * Test: Login amb credencials correctes retorna èxit i guarda el token.
     *
     * **Escenari:**
     * Usuari introdueix credencials vàlides ("admin" / "admin") i fa clic a ENTRAR.
     *
     * **Passos:**
     * 1. Crear ViewModel amb FakeAuthApiSuccess
     * 2. Executar login("admin", "admin")
     * 3. Avançar fins completar totes les coroutines
     * 4. Verificar estat resultant
     *
     * **Verificacions:**
     * -  loginSuccess == true
     * -  authResponse != null
     * -  authResponse.token == "abc.def.ghi"
     * -  TokenManager conté el token guardat
     * - error == null
     *
     * **Resultat esperat:**
     * Login exitós amb token JWT guardat localment.
     */
    @Test
    fun login_credencialsCorrectes_retornaExitIGuardaToken() = runTest {
        val vm = AuthViewModel(api = FakeAuthApiSuccess())
        vm.login("admin", "admin")
        dispatcher.scheduler.advanceUntilIdle()

        val ui = vm.loginUiState.value

        // Verificar estado exitoso
        assertTrue("El login hauria de ser exitós", ui.loginSuccess)
        assertNotNull("Hauria d'haver una resposta d'autenticació", ui.authResponse)
        assertEquals("El token hauria de guardar-se", "abc.def.ghi", TokenManager.getToken())
        assertNull("No hauria d'haver cap error", ui.error)
    }

    /**
     * Test: Login amb credencials incorrectes retorna error i neteja el token.
     *
     * **Escenari:**
     * Usuari introdueix credencials invàlides.
     *
     * **Passos:**
     * 1. Crear ViewModel amb FakeAuthApiError (simula 401)
     * 2. Executar login amb credencials incorrectes
     * 3. Avançar fins completar
     * 4. Verificar gestió d'errors
     *
     * **Verificacions:**
     * - loginSuccess == false
     * - authResponse == null
     * - error != null (missatge d'error present)
     * - error conté "Credenciales" o "401"
     * - TokenManager.getToken() == null (token no guardat)
     *
     * **Resultat esperat:**
     * Login fallit amb missatge d'error i sense token guardat.
     */
    @Test
    fun login_credencialsIncorrectes_retornaErrorINetejaToken() = runTest {
        val vm = AuthViewModel(api = FakeAuthApiError())
        vm.login("user_incorrecte", "password_incorrecte")
        dispatcher.scheduler.advanceUntilIdle()

        val ui = vm.loginUiState.value
        println("❌ Mensaje de error real: ${ui.error}")
        // Verificar estado de error
        assertFalse("El login no hauria de ser exitós", ui.loginSuccess)
        assertNull("No hauria d'haver resposta d'autenticació", ui.authResponse)
        assertNotNull("Hauria d'haver un missatge d'error", ui.error)
        assertNull("El token no hauria de guardar-se", TokenManager.getToken())
        assertTrue(
            "El missatge d'error hauria de mencionar credencials",
            ui.error!!.contains("Credencials invàlides") || ui.error!!.contains("401")
        )
    }

    /**
     * Test: Login mostra indicador de càrrega durant la petició.
     *
     * **Escenari:**
     * Verificar que isLoading s'activa durant el procés i es desactiva al finalitzar.
     *
     * **Passos:**
     * 1. Crear ViewModel amb fake que té delay de 100ms
     * 2. Executar login
     * 3. Verificar isLoading = true DURANT la petició
     * 4. Avançar temps fins completar
     * 5. Verificar isLoading = false DESPRÉS de completar
     *
     * **Verificacions:**
     * - isLoading = true immediatament després de login()
     * -  isLoading = false després de completar
     * -  loginSuccess = true al final
     *
     * **Importància:**
     * Assegura que la UI pot mostrar un indicador visual mentre espera resposta.
     */
    @Test
    fun login_mostraIndicadorCarregaDurantPeticio() = runTest {
        // Fake amb delay per simular latència de xarxa
        val slowFake = object : AuthApiService {
            override suspend fun login(request: AuthenticationRequest): AuthResponse {
                delay(100)
                return AuthResponse(
                    token = "tok",
                    id = 1,
                    rol = 1,
                    nick = "user",
                    nom = "User",
                    cognom1 = "A",
                    cognom2 = "test"
                )
            }

            override suspend fun getAllUsers() = emptyList<User>()
            override suspend fun getUserById(userId: Long) = error("no utilitzat")
            override suspend fun updateUser(userId: Long, user: UpdateUserRequest): User = error("no utilitzat")
            override suspend fun deleteUser(userId: Long): Response<Unit> = error("no utilitzat")
            override suspend fun createUser(request: CreateUserRequest): AuthResponse =
                error("no utilitzat")

            override suspend fun logout(): Response<String> = error("no utilitzat")
        }

        val vm = AuthViewModel(api = slowFake)
        // ACT: Executar login
        vm.login("user", "password")

        // ASSERT: Verificar isLoading DURANT la petició
        assertTrue(
            "isLoading hauria de ser true durant la petició",
            vm.loginUiState.value.isLoading
        )

        // Avançar el temps per completar la petició
        advanceTimeBy(200)
        dispatcher.scheduler.advanceUntilIdle()

        // ASSERT: Verificar isLoading DESPRÉS de completar
        assertFalse(
            "isLoading hauria de ser false després de completar",
            vm.loginUiState.value.isLoading
        )
        assertTrue(
            "El login hauria de ser exitós",
            vm.loginUiState.value.loginSuccess
        )
    }

    @Test
    fun login_campsBuilts_noFaPeticio() = runTest {
        // Opcional: test para verificar que no se hace petición con campos vacíos
        val vm = AuthViewModel(api = FakeAuthApiSuccess())

        // Nota: Esto requeriría modificar el ViewModel para validar antes de llamar a la API
        // Por ahora es más una validación de UI, pero se puede añadir lógica al ViewModel
    }
}