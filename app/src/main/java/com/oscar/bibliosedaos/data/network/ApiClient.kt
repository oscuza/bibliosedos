package com.oscar.bibliosedaos.data.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

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
     * **Configuració per Emulador Android:**
     * - Utilitza `10.0.2.2` per accedir al localhost del PC
     * - El port ha de coincidir amb el port del servidor backend
     *
     * **Configuració per Dispositiu Físic:**
     * - Cal utilitzar la IP real del PC a la xarxa local
     * - Exemple: `http://192.168.1.100:8080/`
     *
     * **Nota de Seguretat:**
     * En producció, sempre utilitzar HTTPS en lloc de HTTP.
     */
    private const val BASE_URL = "http://10.0.2.2:8080/"

    /**
     * Interceptor per registrar peticions i respostes HTTP.
     *
     * Configurat en mode BODY per veure tots els detalls:
     * - Headers de la petició i resposta
     * - Cos complet de la petició (JSON)
     * - Cos complet de la resposta (JSON)
     * - Temps de resposta
     *
     * **Nivells Disponibles:**
     * - NONE: Sense logging
     * - BASIC: URL, mètode, codi de resposta, temps
     * - HEADERS: BASIC + capçaleres
     * - BODY: HEADERS + cossos complets
     *
     * **Nota:**
     * En producció, considerar canviar a BASIC o NONE per rendiment.
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
     * Client OkHttp configurat amb els interceptors necessaris.
     *
     * **Ordre dels Interceptors:**
     * 1. [authInterceptor]: S'executa primer per afegir el token
     * 2. [loggingInterceptor]: S'executa després per registrar la petició completa
     *
     * **Configuracions Addicionals Possibles:**
     * - Timeouts de connexió i lectura
     * - Retry policies
     * - Certificate pinning (per seguretat)
     * - Cache de respostes
     */
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
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
     * **Ús:**
     * ```kotlin
     * val api = ApiClient.instance
     * val users = api.getAllUsers()
     * ```
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