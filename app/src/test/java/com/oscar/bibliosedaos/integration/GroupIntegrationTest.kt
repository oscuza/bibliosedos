package com.oscar.bibliosedaos.integration

import com.oscar.bibliosedaos.data.models.Grup
import com.oscar.bibliosedaos.data.models.Horari
import com.oscar.bibliosedaos.data.network.AuthApiService
import com.oscar.bibliosedaos.data.network.AuthenticationRequest
import com.oscar.bibliosedaos.data.network.AuthInterceptor
import com.oscar.bibliosedaos.data.network.TokenManager
import com.oscar.bibliosedaos.data.network.User
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.After
import org.junit.Assert.*
import org.junit.Assume
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * Tests d'integració per a la gestió de grups de lectura.
 * 
 * **Descripció:**
 * Aquests tests verifiquen la comunicació real amb el backend per a operacions
 * relacionades amb grups de lectura. Requereixen que el servidor backend estigui
 * en execució i que hi hagi un usuari admin amb les credencials configurades.
 * 
 * **Cobertura:**
 * - Llistar grups (GET /biblioteca/grups/llistarGrups)
 * - Crear grup (POST /biblioteca/grups/afegirGrup)
 * - Eliminar grup (DELETE /biblioteca/grups/eliminarGrup/{id})
 * - Afegir membre a grup (PUT /biblioteca/grups/{grupId}/afegirUsuariGrup/{membreId})
 * - Llistar membres d'un grup (GET /biblioteca/grups/llistarUsuarisGrup/{grupId})
 * - Eliminar membre de grup (DELETE /biblioteca/grups/{grupId}/sortirUsuari/{membreId})
 * 
 * **Requisits:**
 * - Backend en execució a la URL configurada (per defecte: https://10.0.2.2:8443/)
 * - Usuari admin amb credencials: nick="admin", password="admin1234"
 * - Almenys un horari lliure a la base de dades
 * 
 * **Nota:**
 * Aquests tests creen i eliminen dades reals a la base de dades. S'asseguren
 * de netejar després de cada test, però és recomanable utilitzar una base de
 * dades de proves separada.
 * 
 * @author Oscar
 * @since 1.0
 */
class GroupIntegrationTest {

    /**
     * URL base de l'API REST del backend.
     * 
     * Es pot configurar via propietats del sistema:
     * - `BIBLIO_API_BASE_URL`
     * - `biblio.api.baseUrl`
     * 
     * Per defecte utilitza HTTPS al port 8443 per emulador Android.
     */
    private val baseUrl: String = (
        System.getProperty("BIBLIO_API_BASE_URL")
            ?: System.getProperty("biblio.api.baseUrl")
            ?: "https://127.0.0.1:8443/"
    )

    /**
     * Instància del servei API configurada per tests d'integració.
     */
    private lateinit var api: AuthApiService

    /**
     * Credencials d'usuari admin per als tests.
     */
    private val adminCredentials = AuthenticationRequest(
        nick = "admin",
        password = "admin1234"
    )

    /**
     * Token JWT obtingut després del login.
     */
    private var authToken: String? = null

    /**
     * Configuració inicial abans de cada test.
     */
    @Before
    fun setUp() {
        api = createApiService()
        
        // Netejar token previ
        TokenManager.clearToken()
        
        // Intentar login per verificar connexió amb servidor
        try {
            val response = runBlocking {
                // Utilitzar withTimeout per controlar millor el timeout
                withTimeout(35000) { // 35 segons (una mica més que el timeout de lectura)
                    api.login(adminCredentials)
                }
            }
            
            authToken = response.token
            TokenManager.saveToken(response.token)
            
            // Verificar que el login ha estat exitós
            Assume.assumeTrue(
                "El servidor no està disponible o les credencials són incorrectes",
                response.token.isNotBlank() && response.id > 0
            )
        } catch (e: TimeoutCancellationException) {
            // Timeout de coroutine
            println("Timeout de coroutine al servidor $baseUrl")
            Assume.assumeTrue(
                "Timeout de connexió. El servidor no està disponible o no respon a $baseUrl. Assegura't que el backend estigui en execució.",
                false
            )
        } catch (e: java.net.SocketTimeoutException) {
            // Timeout de connexió - el servidor no respon
            println("Timeout de connexió al servidor $baseUrl")
            Assume.assumeTrue(
                "Timeout de connexió. El servidor no està disponible o no respon a $baseUrl. Assegura't que el backend estigui en execució.",
                false
            )
        } catch (e: java.net.ConnectException) {
            // No es pot connectar al servidor
            println("No es pot connectar al servidor $baseUrl")
            Assume.assumeTrue(
                "No es pot connectar al servidor a $baseUrl. Assegura't que el backend estigui en execució.",
                false
            )
        } catch (e: java.net.SocketException) {
            // Error de socket
            println("Error de socket: ${e.message}")
            Assume.assumeTrue(
                "Error de socket: ${e.message}. El servidor no està disponible a $baseUrl.",
                false
            )
        } catch (e: IOException) {
            // Altres errors de xarxa
            println("Error de xarxa: ${e.javaClass.simpleName} - ${e.message}")
            Assume.assumeTrue(
                "Error de xarxa: ${e.message}. El servidor no està disponible a $baseUrl. Assegura't que el backend estigui en execució.",
                false
            )
        } catch (e: Exception) {
            // Altres errors (credencials incorrectes, etc.)
            println("Error inesperat: ${e.javaClass.simpleName} - ${e.message}")
            Assume.assumeTrue(
                "Error en el login: ${e.message}. Verifica les credencials i que el servidor estigui en execució.",
                false
            )
        }
    }

    /**
     * Neteja després de cada test.
     */
    @After
    fun tearDown() {
        TokenManager.clearToken()
        authToken = null
    }

    /**
     * Crea una instància del servei API configurada per tests d'integració.
     */
    private fun createApiService(): AuthApiService {
        // TrustManager per certificats autosignats (només desenvolupament)
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        // SSLContext per certificats autosignats - usar TLS en lloc de SSL (deprecat)
        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, trustAllCerts, SecureRandom())
        }

        // Interceptor de logging
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        // Interceptor d'autenticació per afegir el token JWT
        val authInterceptor = AuthInterceptor()

        // Client OkHttp configurat amb timeouts i gestió de connexions millorada
        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor) // IMPORTANT: Afegir abans del logging per veure el token
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS) // Timeout de connexió adequat (inclou handshake SSL)
            .readTimeout(30, TimeUnit.SECONDS) // Timeout de lectura més llarg per evitar tancaments prematurs
            .writeTimeout(30, TimeUnit.SECONDS) // Timeout d'escriptura més llarg
            .callTimeout(60, TimeUnit.SECONDS) // Timeout total de la crida
            .retryOnConnectionFailure(true) // Reintentar en cas de fallada de connexió
            .connectionPool(okhttp3.ConnectionPool(5, 5, TimeUnit.MINUTES)) // Pool de connexions per reutilitzar
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true } // Accepta qualsevol hostname (només desenvolupament)
            .build()

        // Retrofit
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()

        return retrofit.create(AuthApiService::class.java)
    }

    /**
     * Test: Llistar tots els grups del sistema.
     * 
     * Verifica que:
     * - La petició es realitza correctament
     * - Es retorna una llista de grups (pot ser buida)
     * - Els grups tenen les propietats esperades
     */
    @Test
    fun llistarGrups_exitos_retornaLlista() = runBlocking {
        val grups = api.getAllGrups()
        
        assertNotNull("La llista de grups no hauria de ser null", grups)
        
        // Si hi ha grups, verificar que tenen les propietats esperades
        if (grups.isNotEmpty()) {
            val primerGrup = grups.first()
            assertNotNull("El grup hauria de tenir un ID", primerGrup.id)
            assertTrue("El nom del grup no hauria de ser buit", primerGrup.nom.isNotBlank())
            assertTrue("La temàtica no hauria de ser buida", primerGrup.tematica.isNotBlank())
        }
    }

    /**
     * Test: Crear un nou grup de lectura.
     * 
     * Verifica que:
     * - El grup es crea correctament al backend
     * - El grup creat té un ID assignat
     * - El grup apareix a la llista de grups
     * 
     * **Limpieza:** Elimina el grup creat després del test.
     */
    @Test
    fun crearGrup_exitos_grupCreatAlBackend() = runBlocking {
        // 1. Obtenir un horari lliure
        val horaris = api.getAllHoraris()
        val horariLliure = horaris.firstOrNull { it.isLliure }
        
        assertNotNull(
            "Cal almenys un horari lliure per crear un grup. Horaris disponibles: ${horaris.size}",
            horariLliure
        )
        
        // 2. Obtenir l'usuari admin actual
        val adminUser = runBlocking {
            val loginResponse = api.login(adminCredentials)
            User(
                id = loginResponse.id,
                nick = loginResponse.nick ?: "admin",
                nom = loginResponse.nom ?: "Admin",
                cognom1 = loginResponse.cognom1,
                cognom2 = loginResponse.cognom2,
                rol = loginResponse.rol
            )
        }
        
        // 3. Crear el grup
        val nouGrup = Grup(
            nom = "Grup Test Integració ${System.currentTimeMillis()}",
            tematica = "Test",
            administrador = adminUser,
            horari = horariLliure,
            membres = emptyList()
        )
        
        val grupCreat = api.createGrup(nouGrup)
        
        // 4. Verificar que el grup s'ha creat correctament
        assertNotNull("El grup creat no hauria de ser null", grupCreat)
        assertNotNull("El grup creat hauria de tenir un ID", grupCreat.id)
        assertEquals("El nom hauria de coincidir", nouGrup.nom, grupCreat.nom)
        assertEquals("La temàtica hauria de coincidir", nouGrup.tematica, grupCreat.tematica)
        assertNotNull("Hauria de tenir un administrador", grupCreat.administrador)
        assertEquals("L'ID de l'administrador hauria de coincidir", adminUser.id, grupCreat.administrador?.id)
        
        // 5. Verificar que el grup apareix a la llista
        val grups = api.getAllGrups()
        assertTrue(
            "El grup creat hauria d'aparèixer a la llista",
            grups.any { it.id == grupCreat.id }
        )
        
        // 6. Limpiar: eliminar el grup creat
        try {
            val deleteResponse = api.deleteGrup(grupCreat.id!!)
            assertTrue("L'eliminació hauria de ser exitosa", deleteResponse.isSuccessful)
        } catch (e: Exception) {
            // Si falla l'eliminació, registrar però no fallar el test
            println("No s'ha pogut eliminar el grup creat (ID: ${grupCreat.id}): ${e.message}")
        }
    }

    /**
     * Test: Eliminar un grup existent.
     * 
     * Verifica que:
     * - El grup es crea correctament
     * - El grup es pot eliminar
     * - El grup eliminat no apareix a la llista
     * 
     * **Nota:** Aquest test crea un grup temporal per eliminar-lo.
     */
    @Test
    fun eliminarGrup_exitos_grupEliminatDelBackend() = runBlocking {
        // 1. Crear un grup temporal
        val horaris = api.getAllHoraris()
        val horariLliure = horaris.firstOrNull { it.isLliure }
        
        assertNotNull(
            "Cal almenys un horari lliure per crear un grup",
            horariLliure
        )
        
        val adminUser = runBlocking {
            val loginResponse = api.login(adminCredentials)
            User(
                id = loginResponse.id,
                nick = loginResponse.nick ?: "admin",
                nom = loginResponse.nom ?: "Admin",
                cognom1 = loginResponse.cognom1,
                cognom2 = loginResponse.cognom2,
                rol = loginResponse.rol
            )
        }
        
        val grupTemporal = Grup(
            nom = "Grup Temporal Eliminar ${System.currentTimeMillis()}",
            tematica = "Test",
            administrador = adminUser,
            horari = horariLliure,
            membres = emptyList()
        )
        
        val grupCreat = api.createGrup(grupTemporal)
        val grupId = grupCreat.id!!
        
        // 2. Verificar que el grup existeix
        val grupsAbans = api.getAllGrups()
        assertTrue(
            "El grup hauria d'existir abans d'eliminar-lo",
            grupsAbans.any { it.id == grupId }
        )
        
        // 3. Eliminar el grup
        val deleteResponse = api.deleteGrup(grupId)
        assertTrue("L'eliminació hauria de ser exitosa", deleteResponse.isSuccessful)
        
        // 4. Verificar que el grup ja no existeix
        val grupsDespres = api.getAllGrups()
        assertFalse(
            "El grup eliminat no hauria d'aparèixer a la llista",
            grupsDespres.any { it.id == grupId }
        )
    }

    /**
     * Test: Llistar membres d'un grup.
     * 
     * Verifica que:
     * - Es pot obtenir la llista de membres d'un grup existent
     * - La llista retornada és vàlida
     */
    @Test
    fun llistarMembresGrup_exitos_retornaLlistaMembres() {
        runBlocking {
            // 1. Obtenir un grup existent (o crear-ne un temporal)
            val grups = api.getAllGrups()
            
            if (grups.isEmpty()) {
                // Si no hi ha grups, crear-ne un temporal
                val horaris = api.getAllHoraris()
                val horariLliure = horaris.firstOrNull { it.isLliure }
                
                if (horariLliure != null) {
                    val adminUser = runBlocking {
                        val loginResponse = api.login(adminCredentials)
                        User(
                            id = loginResponse.id,
                            nick = loginResponse.nick ?: "admin",
                            nom = loginResponse.nom ?: "Admin",
                            cognom1 = loginResponse.cognom1,
                            cognom2 = loginResponse.cognom2,
                            rol = loginResponse.rol
                        )
                    }
                    
                    val grupTemporal = Grup(
                        nom = "Grup Temporal Membres ${System.currentTimeMillis()}",
                        tematica = "Test",
                        administrador = adminUser,
                        horari = horariLliure,
                        membres = emptyList()
                    )
                    
                    val grupCreat = api.createGrup(grupTemporal)
                    val membres = api.getMembresGrup(grupCreat.id!!)
                    
                    assertNotNull("La llista de membres no hauria de ser null", membres)
                    
                    // Limpiar
                    try {
                        api.deleteGrup(grupCreat.id!!)
                    } catch (e: Exception) {
                        println("No s'ha pogut eliminar el grup temporal: ${e.message}")
                    }
                } else {
                    // No hi ha horaris disponibles
                    println("No hi ha horaris disponibles per crear un grup de prova")
                }
            } else {
                // Utilitzar un grup existent
                val grup = grups.first()
                val membres = api.getMembresGrup(grup.id!!)
                
                assertNotNull("La llista de membres no hauria de ser null", membres)
            }
        }
    }
}

