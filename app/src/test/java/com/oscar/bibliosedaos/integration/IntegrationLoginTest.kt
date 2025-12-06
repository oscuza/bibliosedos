package com.oscar.bibliosedaos.integration

import com.oscar.bibliosedaos.data.network.AuthApiService
import com.oscar.bibliosedaos.data.network.AuthenticationRequest
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.Assert.*
import org.junit.Assume
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Proves d'integració de xarxa (IOC):
 * - Demostra que amb el servidor ENGEGAT el login funciona.
 * - Demostra que amb el servidor ATURAT el test falla (exemple d'error de connexió).
 *
 * Executa el test 2 vegades:
 *  1) Servidor en marxa -> test 'login_OK_server_up' ha de passar.
 *  2) Servidor apagat -> test 'login_FAIL_server_down' ha de fallar amb IOException.
 *
 * BASE_URL configurable via propietats Gradle:
 *   ./gradlew test -Pbiblio.api.baseUrl=http://localhost:8080/
 */
class IntegrationLoginTest {

    private val baseUrl: String = (System.getProperty("BIBLIO_API_BASE_URL")
        ?: System.getProperty("biblio.api.baseUrl"))
        ?: "http://localhost:8080/"

    private fun service(): AuthApiService {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()

        return retrofit.create(AuthApiService::class.java)
    }
    /**
     * Aquest test requereix que el servidor estigui ENGAT..
     */
    @Test
    fun login_OK_server_up() = runBlocking {
        // REQUEREIX servidor en marxa a baseUrl
        val api = service()
        val req = AuthenticationRequest(nick = "admin", password = "admin1234")
        val resp = api.login(req)

        assertNotNull(resp)
        assertTrue(resp.token.isNotBlank())
        assertTrue(resp.id > 0)
    }

    /**
     * Aquest test requereix que el servidor estigui APAGAT.
     */
    @Test
    fun login_FAIL_server_down() = runBlocking {
        // Amb el servidor APAGAT, la crida ha de llençar IOException
        val api = service()
        val req = AuthenticationRequest(nick = "admin", password = "admin")

        try {
            api.login(req)
            println("El servidor està actiu; es salta la prova d'error de connexió")
            Assume.assumeTrue("Aquest test només s'executa amb el servidor apagat", false)
        } catch (e: IOException) {
            // Test passa - això és l'esperat
            assertTrue(
                e.message?.contains("Failed to connect") == true ||
                        e.message?.contains("timeout") == true ||
                        e.message?.contains("Connection refused") == true
            )
        } catch (e: Exception) {
            fail("S'esperava IOException, però s'ha obtingut: ${e::class.simpleName}")
        }
    }
}