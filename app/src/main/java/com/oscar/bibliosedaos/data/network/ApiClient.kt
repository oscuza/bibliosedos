package com.oscar.bibliosedaos.data.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * Client HTTP singleton per a la comunicació amb l'API REST del backend.
 *
 * Aquesta classe configura i proporciona una instància única (singleton) de
 * Retrofit amb tots els interceptors i configuracions necessàries per a
 * les peticions HTTP.
 *
 * **Configuracions Incloses:**
 * - [AuthInterceptor]: Afegeix automàticament el token JWT a les capçaleres
 * - [HttpLoggingInterceptor]: Registra les peticions i respostes per debugging
 * - [GsonConverterFactory]: Serialitza/deserialitza JSON automàticament
 *
 * **URL Base:**
 * Utilitza `10.0.2.2` que és l'adreça especial per accedir al localhost
 * del PC des d'un emulador Android.
 *
 * **Per a Dispositius Físics:**
 * Cal canviar BASE_URL a la IP real del PC, per exemple:
 * - `http://192.168.1.100:8080/` (IP de la xarxa local)
 *
 * @property BASE_URL Adreça base de l'API REST
 * @property loggingInterceptor Interceptor per logging de peticions HTTP
 * @property authInterceptor Interceptor per afegir el token JWT
 * @property okHttpClient Client OkHttp configurat amb interceptors
 * @property instance Instància singleton d'[AuthApiService]
 *
 * @author Oscar
 * @since 1.0
 * @see AuthApiService
 * @see AuthInterceptor
 */
object ApiClient {

    /**
     * URL base de l'API REST del backend.
     *
     * Utilitza `10.0.2.2` per accedir al localhost del PC des d'un emulador Android.
     * Per a dispositius físics, canvia a la IP real del PC a la xarxa local.
     */
    private const val BASE_URL = "https://10.0.2.2:8443/"

    /**
     * Interceptor per registrar peticions i respostes HTTP.
     *
     * Configurat en mode BODY per veure tots els detalls de les peticions i respostes.
     */
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    /**
     * Interceptor que afegeix automàticament el token JWT a les peticions.
     *
     * Aquest interceptor s'executa abans de cada petició HTTP i:
     * 1. Obté el token actual de [TokenManager]
     * 2. L'afegeix a la capçalera `Authorization` amb format `Bearer {token}`
     * 3. Detecta errors d'autenticació (401/403) i neteja el token si cal
     *
     * @see AuthInterceptor
     * @see TokenManager
     */
    private val authInterceptor = AuthInterceptor()

    /**
     * TrustManager que confia en certificats autosignats del servidor.
     */
    private val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    })

    /**
     * SSLContext configurat per confiar en certificats autosignats.
     */
    private val sslContext = SSLContext.getInstance("TLS").apply {
        init(null, trustAllCerts, SecureRandom())
    }

    /**
     * Client OkHttp configurat amb els interceptors necessaris.
     *
     * Configuracions:
     * - Timeouts de connexió i lectura (30 segons)
     * - SSL/TLS configurat per acceptar certificats autosignats
     * - Logging de peticions i respostes
     */
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
        .hostnameVerifier { _, _ -> true }
        .build()

    /**
     * Instància singleton d'[AuthApiService].
     *
     * S'inicialitza de forma lazy (peresosa) la primera vegada que s'accedeix.
     * Això assegura que:
     * - Només es crea una instància durant tota l'execució de l'app
     * - No es crea fins que realment es necessita
     * - És thread-safe (segur per múltiples threads)
     *
     * @return Instància singleton d'[AuthApiService] configurada amb Retrofit
     */
    val instance: AuthApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
        retrofit.create(AuthApiService::class.java)
    }
}