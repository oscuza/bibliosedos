package com.oscar.bibliosedaos.integration

import com.oscar.bibliosedaos.data.models.Horari
import com.oscar.bibliosedaos.data.network.AuthApiService
import com.oscar.bibliosedaos.data.network.AuthenticationRequest
import com.oscar.bibliosedaos.data.network.AuthInterceptor
import com.oscar.bibliosedaos.data.network.TokenManager
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.After
import org.junit.Assert.*
import org.junit.Assume
import org.junit.Before
import org.junit.Test
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
 * Tests d'integració per a la gestió d'horaris de sales.
 * 
 * **Descripció:**
 * Aquests tests verifiquen la comunicació real amb el backend per a operacions
 * relacionades amb horaris de sales. Requereixen que el servidor backend estigui
 * en execució i que hi hagi un usuari admin amb les credencials configurades.
 * 
 * **Cobertura:**
 * - Llistar horaris (GET /biblioteca/horaris/llistarHorarisSales)
 * - Crear horari (POST /biblioteca/horaris/afegirHorari)
 * - Filtrat d'horaris lliures (client-side)
 * 
 * **Requisits:**
 * - Backend en execució a la URL configurada (per defecte: https://10.0.2.2:8443/)
 * - Usuari admin amb credencials: nick="admin", password="admin1234"
 * 
 * **Nota:**
 * Aquests tests creen horaris reals a la base de dades. S'asseguren de netejar
 * després de cada test quan sigui possible, però és recomanable utilitzar una
 * base de dades de proves separada.
 * 
 * **Limitació:**
 * El backend no implementa l'endpoint DELETE per eliminar horaris, per tant
 * els horaris creats durant els tests queden a la base de dades. Això és acceptable
 * per a tests d'integració, però cal tenir-ho en compte.
 * 
 * @author Oscar
 * @since 1.0
 */
class HorariIntegrationTest {

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
            ?: "https://localhost:8443/"
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
                api.login(adminCredentials)
            }
            
            authToken = response.token
            TokenManager.saveToken(response.token)
            
            // Verificar que el login ha estat exitós
            Assume.assumeTrue(
                "El servidor no està disponible o les credencials són incorrectes",
                response.token.isNotBlank() && response.id > 0
            )
        } catch (e: java.net.SocketTimeoutException) {
            // Timeout de connexió - el servidor no respon
            Assume.assumeTrue(
                "Timeout de connexió. El servidor no està disponible o no respon a $baseUrl. Assegura't que el backend estigui en execució.",
                false
            )
        } catch (e: java.net.ConnectException) {
            // No es pot connectar al servidor
            Assume.assumeTrue(
                "No es pot connectar al servidor a $baseUrl. Assegura't que el backend estigui en execució.",
                false
            )
        } catch (e: IOException) {
            // Altres errors de xarxa
            Assume.assumeTrue(
                "Error de xarxa: ${e.message}. El servidor no està disponible a $baseUrl. Assegura't que el backend estigui en execució.",
                false
            )
        } catch (e: Exception) {
            // Altres errors (credencials incorrectes, etc.)
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
     * Test: Llistar tots els horaris del sistema.
     * 
     * Verifica que:
     * - La petició es realitza correctament
     * - Es retorna una llista d'horaris (pot ser buida)
     * - Els horaris tenen les propietats esperades
     */
    @Test
    fun llistarHoraris_exitos_retornaLlista() = runBlocking {
        val horaris = api.getAllHoraris()
        
        assertNotNull("La llista d'horaris no hauria de ser null", horaris)
        
        // Si hi ha horaris, verificar que tenen les propietats esperades
        if (horaris.isNotEmpty()) {
            val primerHorari = horaris.first()
            assertNotNull("L'horari hauria de tenir un ID", primerHorari.id)
            assertTrue("La sala no hauria de ser buida", primerHorari.sala.isNotBlank())
            assertTrue("El dia no hauria de ser buit", primerHorari.dia.isNotBlank())
            assertTrue("L'hora no hauria de ser buida", primerHorari.hora.isNotBlank())
        }
    }

    /**
     * Test: Crear un nou horari de sala.
     * 
     * Verifica que:
     * - L'horari es crea correctament al backend
     * - L'horari creat té un ID assignat
     * - L'horari apareix a la llista d'horaris
     * 
     * **Nota:** L'horari creat queda a la base de dades ja que no hi ha endpoint DELETE.
     */
    @Test
    fun crearHorari_exitos_horariCreatAlBackend() = runBlocking {
        // 1. Crear un horari amb dades úniques (utilitzant timestamp)
        val timestamp = System.currentTimeMillis()
        val nouHorari = Horari(
            sala = "Sala Test $timestamp",
            dia = "Dimecres",
            hora = "15:00",
            estat = "lliure"
        )
        
        val horariCreat = api.createHorari(nouHorari)
        
        // 2. Verificar que l'horari s'ha creat correctament
        assertNotNull("L'horari creat no hauria de ser null", horariCreat)
        assertNotNull("L'horari creat hauria de tenir un ID", horariCreat.id)
        assertEquals("La sala hauria de coincidir", nouHorari.sala, horariCreat.sala)
        assertEquals("El dia hauria de coincidir", nouHorari.dia, horariCreat.dia)
        assertEquals("L'hora hauria de coincidir", nouHorari.hora, horariCreat.hora)
        assertEquals("L'estat hauria de coincidir", nouHorari.estat, horariCreat.estat)
        
        // 3. Verificar que l'horari apareix a la llista
        val horaris = api.getAllHoraris()
        assertTrue(
            "L'horari creat hauria d'aparèixer a la llista",
            horaris.any { it.id == horariCreat.id }
        )
        
        // 4. Verificar que l'horari creat està a la llista amb les dades correctes
        val horariTrobat = horaris.find { it.id == horariCreat.id }
        assertNotNull("L'horari creat hauria de trobar-se a la llista", horariTrobat)
        assertEquals("La sala hauria de coincidir", horariCreat.sala, horariTrobat?.sala)
        assertEquals("El dia hauria de coincidir", horariCreat.dia, horariTrobat?.dia)
        assertEquals("L'hora hauria de coincidir", horariCreat.hora, horariTrobat?.hora)
    }

    /**
     * Test: Filtrar horaris lliures (client-side).
     * 
     * Verifica que:
     * - Es pot obtenir la llista d'horaris
     * - Es poden filtrar els horaris lliures correctament
     * - Els horaris lliures tenen l'estat correcte
     */
    @Test
    fun filtrarHorarisLliures_exitos_retornaHorarisLliures() = runBlocking {
        // 1. Obtenir tots els horaris
        val totsHoraris = api.getAllHoraris()
        
        assertNotNull("La llista d'horaris no hauria de ser null", totsHoraris)
        
        // 2. Filtrar horaris lliures (client-side)
        val horarisLliures = totsHoraris.filter { it.isLliure }
        
        // 3. Verificar que tots els horaris lliures tenen l'estat correcte
        horarisLliures.forEach { horari ->
            assertTrue(
                "L'horari amb ID ${horari.id} hauria de ser lliure",
                horari.isLliure
            )
            // L'estat pot ser null o "lliure"
            assertTrue(
                "L'estat hauria de ser null o 'lliure'",
                horari.estat == null || horari.estat.lowercase() == "lliure"
            )
        }
        
        // 4. Verificar que els horaris no lliures no estan a la llista filtrada
        val horarisReservats = totsHoraris.filter { !it.isLliure }
        horarisReservats.forEach { horariReservat ->
            assertFalse(
                "L'horari reservat amb ID ${horariReservat.id} no hauria d'estar a la llista de lliures",
                horarisLliures.any { it.id == horariReservat.id }
            )
        }
    }

    /**
     * Test: Crear horari amb estat reservat.
     * 
     * Verifica que:
     * - Es pot crear un horari amb estat "reservat"
     * - L'horari creat té l'estat correcte
     * - L'horari no apareix a la llista d'horaris lliures
     */
    @Test
    fun crearHorariReservat_exitos_estatCorrecte() = runBlocking {
        // 1. Crear un horari amb estat reservat
        val timestamp = System.currentTimeMillis()
        val nouHorari = Horari(
            sala = "Sala Reservada $timestamp",
            dia = "Divendres",
            hora = "18:00",
            estat = "reservat"
        )
        
        val horariCreat = api.createHorari(nouHorari)
        
        // 2. Verificar que l'horari s'ha creat amb l'estat correcte
        assertNotNull("L'horari creat no hauria de ser null", horariCreat)
        assertEquals("L'estat hauria de ser 'reservat'", "reservat", horariCreat.estat)
        assertFalse("L'horari no hauria de ser lliure", horariCreat.isLliure)
        
        // 3. Verificar que l'horari no apareix a la llista d'horaris lliures
        val totsHoraris = api.getAllHoraris()
        val horarisLliures = totsHoraris.filter { it.isLliure }
        
        assertFalse(
            "L'horari reservat no hauria d'aparèixer a la llista de lliures",
            horarisLliures.any { it.id == horariCreat.id }
        )
        
        // 4. Verificar que l'horari apareix a la llista completa
        assertTrue(
            "L'horari reservat hauria d'aparèixer a la llista completa",
            totsHoraris.any { it.id == horariCreat.id }
        )
    }

    /**
     * Test: Validar propietats d'horaris existents.
     * 
     * Verifica que:
     * - Tots els horaris tenen les propietats obligatòries
     * - Les propietats tenen valors vàlids
     */
    @Test
    fun validarPropietatsHoraris_exitos_propietatsCorrectes() = runBlocking {
        val horaris = api.getAllHoraris()
        
        horaris.forEach { horari ->
            // Verificar propietats obligatòries
            assertNotNull("L'horari hauria de tenir un ID", horari.id)
            assertTrue("La sala no hauria de ser buida", horari.sala.isNotBlank())
            assertTrue("El dia no hauria de ser buit", horari.dia.isNotBlank())
            assertTrue("L'hora no hauria de ser buida", horari.hora.isNotBlank())
            
            // Verificar format de l'hora (opcional, però útil)
            val horaPattern = Regex("^\\d{1,2}:\\d{2}$")
            assertTrue(
                "L'hora hauria de tenir format HH:MM (ex: 10:00, 18:30)",
                horaPattern.matches(horari.hora)
            )
            
            // Verificar que l'estat és vàlid si existeix
            if (horari.estat != null) {
                val estatValid = horari.estat.lowercase() in listOf("lliure", "reservat", "ocupat")
                assertTrue(
                    "L'estat hauria de ser 'lliure', 'reservat' o 'ocupat'",
                    estatValid
                )
            }
        }
    }
}

