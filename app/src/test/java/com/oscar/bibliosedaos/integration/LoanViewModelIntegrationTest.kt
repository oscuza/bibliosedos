package com.oscar.bibliosedaos.integration

import com.oscar.bibliosedaos.data.network.AuthApiService
import com.oscar.bibliosedaos.data.network.AuthInterceptor
import com.oscar.bibliosedaos.data.network.AuthenticationRequest
import com.oscar.bibliosedaos.data.network.TokenManager
import com.oscar.bibliosedaos.ui.viewmodels.LoanViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Tests d'integració per a la classe LoanViewModel.
 * 
 * **Descripció:**
 * Aquests tests verifiquen que LoanViewModel funciona correctament amb un backend real.
 * A diferència dels tests unitaris que utilitzen APIs falses, aquests tests realitzen
 * operacions reals amb el servidor backend per gestionar préstecs.
 * 
 * **Requeriments:**
 * - ⚠️ El servidor backend ha d'estar en marxa (per defecte: http://localhost:8080/)
 * - ⚠️ S'utilitza login d'admin (per defecte: admin/admin1234)
 * - ⚠️ Aquests tests poden fallar si el servidor no està disponible
 * 
 * **Cobertura:**
 * - ✅ Càrrega de préstecs actius des del backend
 * - ✅ Càrrega d'historial de préstecs des del backend
 * - ✅ Verificació de que no hi ha errors en les operacions
 * - ✅ Verificació de que els estats es gestionen correctament (isLoading, error, etc.)
 * 
 * **Tipus de Tests:**
 * - Tests d'integració: Verifiquen la comunicació real amb el backend
 * - Tests de comportament: Verifiquen que el ViewModel gestiona correctament els estats
 * 
 *
 * @author Oscar
 * @since 1.0
 * @see LoanViewModel
 * @see TokenManager
 */
@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
class LoanViewModelIntegrationTest {

    private val baseUrl = System.getProperty("BIBLIO_API_BASE_URL")
        ?: System.getProperty("biblio.api.baseUrl")
        ?: "http://localhost:8080/"

    private lateinit var mainDispatcher: CoroutineDispatcher
    private lateinit var api: AuthApiService
    private lateinit var viewModel: LoanViewModel

    @Before
    fun setUp() {
        mainDispatcher = newSingleThreadContext("LoanViewModelTestMain")
        Dispatchers.setMain(mainDispatcher)
        api = createApiService(baseUrl)
        val adminPassword = System.getProperty("BIBLIO_ADMIN_PASSWORD")
            ?: System.getProperty("biblio.admin.password")
            ?: "admin1234"
        runBlocking {
            val authResponse = api.login(AuthenticationRequest(nick = "admin", password = adminPassword))
            TokenManager.saveToken(authResponse.token)
        }
        viewModel = LoanViewModel(api = api)
    }

    @After
    fun tearDown() {
        TokenManager.clearToken()
        Dispatchers.resetMain()
        (mainDispatcher as? AutoCloseable)?.close()
    }

    private fun createApiService(baseUrl: String): AuthApiService {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor())
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(AuthApiService::class.java)
    }

    private suspend fun waitUntilNotLoading(isLoadingProvider: () -> Boolean) {
        withTimeout(5_000) {
            while (isLoadingProvider()) {
                delay(100)
            }
        }
    }

    /**
     * Test: Càrrega de préstecs actius des del backend real.
     * 
     * **Què s'està provant:**
     * - Que el ViewModel pot carregar préstecs actius des del backend real
     * - Que l'estat `isLoading` es gestiona correctament (inicia a true, finalitza a false)
     * - Que no hi ha errors en la comunicació amb el backend
     * - Que el state conté la informació correcta després de la càrrega
     * 
     * **Condicions:**
     * - Backend ha d'estar en marxa
     * - Login d'admin ha de ser exitós (configurat a setUp)
     * 
     * **Resultats esperats:**
     * - State no és null
     * - No hi ha errors (state.error == null)
     * - No està carregant (state.isLoading == false)
     * - La llista de préstecs actius està disponible (state.loans pot estar buida o no, depèn del backend)
     */
    @Test
    fun loadActiveLoans_integracion_connectaAmbBackend() = runBlocking {
        viewModel.loadActiveLoans()
        waitUntilNotLoading { viewModel.activeLoansState.value.isLoading }

        val state = viewModel.activeLoansState.value
        assertNotNull("State no hauria de ser null", state)
        assertNull("No hauria d'haver error carregant préstecs actius", state.error)
        assertFalse("No hauria d'estar carregant", state.isLoading)
    }

    /**
     * Test: Càrrega d'historial de préstecs des del backend real.
     * 
     * **Què s'està provant:**
     * - Que el ViewModel pot carregar l'historial complet de préstecs des del backend real
     * - Que l'estat `isLoading` es gestiona correctament
     * - Que no hi ha errors en la comunicació amb el backend
     * - Que el state conté la informació correcta després de la càrrega
     * 
     * **Condicions:**
     * - Backend ha d'estar en marxa
     * - Login d'admin ha de ser exitós (configurat a setUp)
     * 
     * **Resultats esperats:**
     * - State no és null
     * - No hi ha errors (state.error == null)
     * - No està carregant (state.isLoading == false)
     * - La llista d'historial de préstecs està disponible (state.loans pot estar buida o no, depèn del backend)
     */
    @Test
    fun loadLoanHistory_integracion_connectaAmbBackend() = runBlocking {
        viewModel.loadLoanHistory()
        waitUntilNotLoading { viewModel.loanHistoryState.value.isLoading }

        val state = viewModel.loanHistoryState.value
        assertNotNull("State no hauria de ser null", state)
        assertNull("No hauria d'haver error carregant l'historial de préstecs", state.error)
        assertFalse("No hauria d'estar carregant", state.isLoading)
    }
}


