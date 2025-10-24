package com.oscar.bibliosedaos.data.network

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Interfície Retrofit per a l'API REST de BibliotecaCloud
 * Petició d'autenticació per al login.
 *
 * Aquesta classe encapsula les credencials necessàries per autenticar
 * un usuari al sistema. S'envia al backend durant el procés de login.
 *
 * @property nick Nom d'usuari únic que identifica l'usuari al sistema
 * @property password Contrasenya de l'usuari en text pla (es xifra durant la transmissió HTTPS)
 *
 * @author Oscar
 * @since 1.0
 * @see AuthApiService.login
 */
data class AuthenticationRequest(
    val nick: String,
    val password: String,
)

/**
 * Resposta del servidor després d'un login exitós.
 *
 * Conté el token JWT per a autenticació de futures peticions i tota
 * la informació bàsica de l'usuari autenticat.
 *
 * @property token Token JWT per autenticació de futures peticions HTTP
 * @property id Identificador únic de l'usuari a la base de dades
 * @property rol Rol de l'usuari (1=Usuari Normal, 2=Administrador)
 * @property nick Nom d'usuari (pot ser null en algunes respostes)
 * @property nom Nom real de l'usuari (pot ser null)
 * @property cognom1 Primer cognom de l'usuari (pot ser null)
 * @property cognom2 Segon cognom de l'usuari (opcional, pot ser null)
 *
 * @author Oscar
 * @since 1.0
 * @see AuthApiService.login
 * @see AuthApiService.createUser
 */
data class AuthResponse(
    @SerializedName("token") val token: String,
    @SerializedName("id") val id: Long,
    @SerializedName("rol") val rol: Int,
    @SerializedName("nick") val nick: String?,
    @SerializedName("nom") val nom: String?,
    @SerializedName("cognom1") val cognom1: String?,
    @SerializedName("cognom2") val cognom2: String?,
)

/**
 * Model de dades que representa un usuari del sistema.
 *
 * Aquesta classe s'utilitza per representar usuaris ja existents al sistema.
 * Conté tota la informació personal i de rol d'un usuari.
 *
 * Inclou tant els camps bàsics com els addicionals que són obligatoris al backend.
 *
 * @property id Identificador únic de l'usuari
 * @property nick Nom d'usuari únic
 * @property nom Nom real
 * @property cognom1 Primer cognom
 * @property cognom2 Segon cognom (opcional)
 * @property rol Rol de l'usuari (1=Normal, 2=Administrador)
 * @property nif NIF/DNI de l'usuari
 * @property localitat Localitat de residència
 * @property carrer Adreça/Carrer
 * @property cp Codi postal
 * @property provincia Província
 * @property tlf Telèfon de contacte
 * @property email Correu electrònic
 *
 * @author Oscar
 * @since 1.0
 */
data class User(
    @SerializedName("id") val id: Long,
    @SerializedName("nick") val nick: String,
    @SerializedName("nom") val nom: String,
    @SerializedName("cognom1") val cognom1: String?,
    @SerializedName("cognom2") val cognom2: String?,
    @SerializedName("rol") val rol: Int,
    // Camps adicionals que requereix el backend
    @SerializedName("nif") val nif: String? = null,
    @SerializedName("localitat") val localitat: String? = null,
    @SerializedName("carrer") val carrer: String? = null,
    @SerializedName("cp") val cp: String? = null,
    @SerializedName("provincia") val provincia: String? = null,
    @SerializedName("tlf") val tlf: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("password") val password: String? = null
) {
    /**
     * Comprova si l'usuari és administrador.
     *
     * @return `true` si l'usuari té rol d'administrador (rol=2), `false` altrament
     */
    val isAdmin: Boolean get() = rol == 2

    /**
     * Comprova si l'usuari és un usuari normal.
     *
     * @return `true` si l'usuari té rol d'usuari normal (rol=1), `false` altrament
     */
    val isUser: Boolean get() = rol == 1

    /**
     * Retorna el nom del rol en format llegible.
     *
     * @return String amb el nom del rol: "Administrador", "Usuario" o "Desconocido"
     */
    val roleName: String
        get() = when (rol) {
            2 -> "Administrador"
            1 -> "Usuari"
            else -> "Desconegut"
        }
}

/**
 * Petició per crear un nou usuari amb TOTS els camps obligatoris del backend.
 *
 * IMPORTANT: Tots aquests camps són OBLIGATORIS al backend i han de tenir un valor.
 * Si no es proporcionen, el backend retornarà un error 400.
 *
 * @property nick Nom d'usuari únic (3-50 caràcters)
 * @property password Contrasenya (mínim 6 caràcters)
 * @property nom Nom real (mínim 2 caràcters)
 * @property cognom1 Primer cognom (mínim 2 caràcters)
 * @property rol Rol de l'usuari (1=Normal, 2=Admin)
 * @property nif NIF/DNI (OBLIGATORI al backend)
 * @property localitat Localitat (OBLIGATORI al backend)
 * @property carrer Adreça (OBLIGATORI al backend)
 * @property cp Codi postal (OBLIGATORI al backend)
 * @property provincia Província (OBLIGATORI al backend)
 * @property tlf Telèfon (OBLIGATORI al backend)
 * @property email Correu electrònic (OBLIGATORI al backend)
 * @property cognom2 Segon cognom (opcional)
 *
 * @author Oscar
 * @since 1.0
 * @see AuthApiService.createUser
 */
data class CreateUserRequest(
    @SerializedName("nick") val nick: String,
    @SerializedName("password") val password: String,
    @SerializedName("nom") val nom: String,
    @SerializedName("cognom1") val cognom1: String,
    @SerializedName("rol") val rol: Int,
    // Camps que són OBLIGATORIS al backend (no poden ser null)
    @SerializedName("nif") val nif: String,
    @SerializedName("localitat") val localitat: String,
    @SerializedName("carrer") val carrer: String,
    @SerializedName("cp") val cp: String,
    @SerializedName("provincia") val provincia: String,
    @SerializedName("tlf") val tlf: String,
    @SerializedName("email") val email: String,
    // Camp opcional
    @SerializedName("cognom2") val cognom2: String? = null

    )

/**
 * Petició per actualitzar un usuari existent amb TOTS els camps necessaris.
 *
 * IMPORTANT: El backend valida que tots aquests camps existeixin, per això
 * hem de mantenir els valors existents dels camps que no es volen modificar.
 *
 * @property id Identificador de l'usuari
 * @property nick Nom d'usuari
 * @property nom Nom real
 * @property cognom1 Primer cognom
 * @property rol Rol de l'usuari
 * @property nif NIF/DNI (mantenir l'existent si no es modifica)
 * @property localitat Localitat (mantenir l'existent si no es modifica)
 * @property carrer Adreça (mantenir l'existent si no es modifica)
 * @property cp Codi postal (mantenir l'existent si no es modifica)
 * @property provincia Província (mantenir l'existent si no es modifica)
 * @property tlf Telèfon (mantenir l'existent si no es modifica)
 * @property email Email (mantenir l'existent si no es modifica)
 * @property cognom2 Segon cognom (opcional)
 * @property password Contrasenya (no s'actualitza en edició normal)
 */
data class UpdateUserRequest(
    @SerializedName("id") val id: Long,
    @SerializedName("nick") val nick: String,
    @SerializedName("nom") val nom: String,
    @SerializedName("cognom1") val cognom1: String,
    @SerializedName("rol") val rol: Int,
    // Camps que el backend valida com obligatoris
    @SerializedName("nif") val nif: String,
    @SerializedName("localitat") val localitat: String,
    @SerializedName("carrer") val carrer: String,
    @SerializedName("cp") val cp: String,
    @SerializedName("provincia") val provincia: String,
    @SerializedName("tlf") val tlf: String,
    @SerializedName("email") val email: String,
    // Camps opcionals
    @SerializedName("cognom2") val cognom2: String? = null,
    @SerializedName("password") val password: String? = null
)
/**
 * Servei de API REST per a operacions d'autenticació i gestió d'usuaris.
 *
 * Defineix tots els endpoints disponibles per interactuar amb el backend.
 * Utilitza Retrofit per gestionar les peticions HTTP de forma asíncrona
 * amb coroutines de Kotlin (suspend functions).
 *
 * **Endpoints d'Autenticació:**
 * - [login]: Autenticar usuari
 * - [logout]: Tancar sessió
 * - [createUser]: Registrar nou usuari
 *
 * **Endpoints de Gestió d'Usuaris:**
 * - [getAllUsers]: Llistar tots els usuaris
 * - [getUserById]: Obtenir usuari per ID
 * - [updateUser]: Actualitzar usuari existent
 * - [deleteUser]: Eliminar usuari
 *
 * @author Oscar
 * @since 1.0
 * @see ApiClient
 */
interface AuthApiService {

    /**
     * Autentica un usuari al sistema.
     *
     * Envia les credencials (nick i password) al servidor per verificar-les.
     * Si l'autenticació és exitosa, retorna un token JWT i les dades de l'usuari.
     *
     * **Endpoint:** `POST /biblioteca/auth/login`
     *
     * **Requeriments:**
     * - No requereix autenticació prèvia
     * - Les credencials s'envien en el body de la petició
     *
     * @param request Credencials de login (nick i password)
     * @return [AuthResponse] amb el token JWT i dades de l'usuari autenticat
     * @throws retrofit2.HttpException si les credencials són incorrectes (401/403)
     * @throws java.io.IOException si hi ha problemes de xarxa
     *
     * @author Oscar
     * @since 1.0
     * @see AuthenticationRequest
     * @see AuthResponse
     */
    @POST("biblioteca/auth/login")
    suspend fun login(@Body request: AuthenticationRequest): AuthResponse

    /**
     * Obté la llista completa d'usuaris registrats al sistema.
     *
     * **Endpoint:** `GET /biblioteca/usuaris/llistarUsuaris`
     *
     * **Requeriments:**
     * - Requereix autenticació (token JWT)
     * - Només accessible per usuaris autenticats
     * - El token s'afegeix automàticament via [AuthInterceptor]
     *
     * @return Llista de [User] amb tots els usuaris del sistema
     * @throws retrofit2.HttpException si no està autenticat (401) o no té permisos (403)
     * @throws java.io.IOException si hi ha problemes de xarxa
     *
     * @author Oscar
     * @since 1.0
     * @see User
     */
    @GET("biblioteca/usuaris/llistarUsuaris")
    suspend fun getAllUsers(): List<User>

    /**
     * Obté les dades d'un usuari específic pel seu identificador.
     *
     * **Endpoint:** `GET /biblioteca/usuaris/trobarUsuariPerId/{id}`
     *
     * **Requeriments:**
     * - Requereix autenticació (token JWT)
     * - L'usuari ha d'existir al sistema
     *
     * @param userId Identificador únic de l'usuari a obtenir
     * @return [User] amb les dades de l'usuari sol·licitat
     * @throws retrofit2.HttpException si l'usuari no existeix (404) o no està autenticat (401)
     * @throws java.io.IOException si hi ha problemes de xarxa
     *
     * @author Oscar
     * @since 1.0
     * @see User
     */
    @GET("biblioteca/usuaris/trobarUsuariPerId/{id}")
    suspend fun getUserById(@Path("id") userId: Long): User

    /**
     * Actualitza les dades d'un usuari existent.
     *
     * Permet modificar la informació personal d'un usuari. Normalment
     * s'utilitza per actualitzar el perfil propi o per administradors
     * que gestionen altres usuaris.
     *
     * **Endpoint:** `PUT /biblioteca/usuaris/actualitzarUsuari/{id}`
     *
     * **Requeriments:**
     * - Requereix autenticació (token JWT)
     * - El nick ha de ser únic (no pot coincidir amb un altre usuari)
     *
     * @param userId Identificador de l'usuari a actualitzar
     * @param user Objecte [User] amb les dades actualitzades
     * @return [User] amb les dades ja actualitzades
     * @throws retrofit2.HttpException si el nick ja existeix (409) o no està autenticat (401)
     * @throws java.io.IOException si hi ha problemes de xarxa
     *
     * @author Oscar
     * @since 1.0
     * @see User
     */
    @PUT("biblioteca/usuaris/actualitzarUsuari/{id}")
    suspend fun updateUser(
        @Path("id") userId: Long,
        @Body user: UpdateUserRequest,
    ): User

    /**
     * Elimina un usuari del sistema de forma permanent.
     *
     * **Endpoint:** `DELETE /biblioteca/usuaris/eliminarUsuari/{id}`
     *
     * **Requeriments:**
     * - Requereix autenticació (token JWT)
     * - Requereix permisos d'administrador
     * - Un administrador NO pot eliminar-se a si mateix
     *
     * **Nota de Seguretat:**
     * Aquesta operació és irreversible. Totes les dades relacionades
     * amb l'usuari seran eliminades.
     *
     * @param userId Identificador de l'usuari a eliminar
     * @return [Response] amb el resultat de l'operació (success/error)
     * @throws retrofit2.HttpException si no té permisos (403) o l'usuari no existeix (404)
     * @throws java.io.IOException si hi ha problemes de xarxa
     *
     * @author Oscar
     * @since 1.0
     */
    @DELETE("biblioteca/usuaris/eliminarUsuari/{id}")
    suspend fun deleteUser(@Path("id") userId: Long): Response<Unit>

    /**
     * Crea un nou usuari al sistema.
     *
     * Permet a un administrador registrar un nou usuari amb tots els seus
     * camps obligatoris i opcionals.
     *
     * **Endpoint:** `POST /biblioteca/auth/afegirUsuari`
     *
     * **Requeriments:**
     * - Requereix autenticació (token JWT)
     * - Requereix permisos d'administrador
     * - El nick ha de ser únic al sistema
     * - Password mínim 6 caràcters
     *
     * **Camps Obligatoris:**
     * - nick, password, nombre, apellido1, rol
     *
     * @param request Dades del nou usuari ([CreateUserRequest])
     * @return [AuthResponse] amb els dades de l'usuari creat
     * @throws retrofit2.HttpException si el nick ja existeix (409) o no té permisos (403)
     * @throws java.io.IOException si hi ha problemes de xarxa
     *
     * @author Oscar
     * @since 1.0
     * @see CreateUserRequest
     * @see AuthResponse
     */
    @POST("biblioteca/auth/afegirUsuari")
    suspend fun createUser(@Body request: CreateUserRequest): AuthResponse

    /**
     * Tanca la sessió de l'usuari actual i revoca el token JWT.
     *
     * Notifica al servidor que l'usuari ha tancat sessió. El servidor
     * pot marcar el token com a invàlid o dur a terme operacions de neteja.
     *
     * **Endpoint:** `POST /biblioteca/auth/logout`
     *
     * **Requeriments:**
     * - Requereix autenticació (el token a revocar)
     * - S'ha de netejar el token localment després de cridar aquest endpoint
     *
     * **Comportament:**
     * Encara que falli la petició al servidor, el client sempre ha de
     * netejar el token local per seguretat.
     *
     * @return [Response] amb el resultat de l'operació
     * @throws java.io.IOException si hi ha problemes de xarxa (no crític)
     *
     * @author Oscar
     * @since 1.0
     * @see TokenManager.clearToken
     */
    @POST("biblioteca/auth/logout")
    suspend fun logout(): Response<String>
}