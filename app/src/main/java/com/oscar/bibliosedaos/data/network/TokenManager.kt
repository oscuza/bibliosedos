package com.oscar.bibliosedaos.data.network

/**
 * Gestor centralitzat de tokens JWT en memòria.
 *
 * Aquesta classe singleton proporciona una gestió simple i eficient del token
 * d'autenticació JWT utilitzat per les peticions HTTP.
 *
 * **Característiques:**
 * - ✅ Thread-safe (object singleton de Kotlin)
 * - ✅ Accés centralitzat des de qualsevol part de l'app
 * - ✅ API simple i clara
 * - ❌ **SENSEs persistència:** El token es perd en tancar l'app
 *
 * **Funcionament:**
 * ```
 * Login Exitós
 *     │
 *     ▼
 * TokenManager.saveToken("eyJhbGciOiJIUzI1...")
 *     │
 *     ▼
 * [Token guardat en RAM]
 *     │
 *     ├─→ Peticions HTTP: AuthInterceptor usa TokenManager.getToken()
 *     │
 *     ├─→ Verificacions: if (TokenManager.hasToken()) { ... }
 *     │
 *     └─→ Logout o Error 401: TokenManager.clearToken()
 *         │
 *         ▼
 *     [Token eliminat de RAM]
 * ```
 *
 * **Seguretat:**
 * - El token només existeix en RAM (no es guarda en disc)
 * - Es perd automàticament en tancar l'aplicació
 * - Logout automàtic en cada reinici de l'app
 *
 * **Alternativa amb Persistència:**
 * Per mantenir la sessió després de tancar l'app, considerar:
 * - DataStore (recomanat per Jetpack)
 * - SharedPreferences (manera clàssica)
 * - Encrypted SharedPreferences (més segur)
 *
 * **Exemple d'Ús:**
 * ```kotlin
 * // Després del login
 * TokenManager.saveToken(authResponse.token)
 *
 * // Comprovar si hi ha sessió activa
 * if (TokenManager.hasToken()) {
 *     // Usuario autenticado
 *     navigateToHome()
 * } else {
 *     // Usuario sin autenticar
 *     navigateToLogin()
 * }
 *
 * // Durant el logout
 * TokenManager.clearToken()
 * ```
 *
 * @property currentToken Token JWT actual, null si no hi ha sessió activa
 *
 * @author Oscar
 * @since 1.0
 * @see AuthInterceptor
 * @see AuthViewModel.login
 * @see AuthViewModel.logout
 */
object TokenManager {
    /**
     * Token JWT actual emmagatzemat en memòria.
     *
     * **Visibilitat:** Private per evitar modificacions directes
     * **Valor Inicial:** null (no hi ha sessió activa)
     * **Cicle de Vida:** Dura mentre l'app estigui en memòria
     *
     * @see saveToken
     * @see getToken
     * @see clearToken
     */
    private var currentToken: String? = null

    /**
     * Guarda el token JWT en memòria.
     *
     * Aquest mètode s'ha de cridar després d'un login exitós per emmagatzemar
     * el token que el servidor ha retornat.
     *
     * **Quan Utilitzar:**
     * - Després d'un login exitós
     * - Després de refrescar el token (si implementat)
     *
     * **Exemple:**
     * ```kotlin
     * // En el ViewModel després del login
     * val response = api.login(request)
     * TokenManager.saveToken(response.token)
     * ```
     *
     * @param token Token JWT rebut del servidor (format: "eyJhbGciOiJIUzI1...")
     *
     * @author Oscar
     * @since 1.0
     */
    fun saveToken(token: String) {
        currentToken = token
    }

    /**
     * Obté el token JWT actual.
     *
     * Retorna el token si existeix o null si no hi ha sessió activa.
     * Aquest mètode és utilitzat principalment per [AuthInterceptor]
     * per afegir el token a les peticions HTTP.
     *
     * **Quan Utilitzar:**
     * - Per comprovar si hi ha token (juntament amb [hasToken])
     * - Per obtenir el token per peticions manuals
     * - Internament per [AuthInterceptor]
     *
     * **Exemple:**
     * ```kotlin
     * val token = TokenManager.getToken()
     * if (token != null) {
     *     // Hay sesión activa
     *     makeAuthenticatedRequest()
     * } else {
     *     // No hay sesión
     *     redirectToLogin()
     * }
     * ```
     *
     * @return Token JWT actual o null si no existeix
     *
     * @author Oscar
     * @since 1.0
     */
    fun getToken(): String? = currentToken

    /**
     * Neteja el token JWT de la memòria.
     *
     * Elimina el token actual, tancant efectivament la sessió de l'usuari.
     * Aquest mètode s'ha de cridar en les següents situacions:
     *
     * **Situacions d'Ús:**
     * 1. **Logout Manual:** L'usuari fa clic en "Cerrar Sesión"
     * 2. **Logout Automàtic:** L'app es tanca o es destrueix
     * 3. **Error d'Autenticació:** El servidor retorna 401/403
     * 4. **Token Expirat:** Es detecta que el token ja no és vàlid
     *
     * **Efectes:**
     * - El token es perd permanentment de la memòria
     * - Les següents peticions HTTP NO tindran token
     * - [hasToken] retornarà false
     * - [getToken] retornarà null
     *
     * **Exemple:**
     * ```kotlin
     * // Logout manual
     * fun logout() {
     *     TokenManager.clearToken()
     *     navigateToLogin()
     * }
     *
     * // Detecció d'error 401
     * if (response.code == 401) {
     *     TokenManager.clearToken()
     *     showLoginScreen()
     * }
     * ```
     *
     * @author Oscar
     * @since 1.0
     */
    fun clearToken() {
        currentToken = null
    }

    /**
     * Comprova si existeix un token JWT actiu.
     *
     * Mètode d'utilitat per verificar ràpidament si hi ha una sessió activa
     * sense necessitat de comprovar si [getToken] retorna null.
     *
     * **Quan Utilitzar:**
     * - Per decidir quina pantalla mostrar a l'inici (Login vs Home)
     * - Per habilitar/deshabilitar funcionalitats que requereixin autenticació
     * - Per verificar abans de fer peticions autenticades
     *
     * **Exemple:**
     * ```kotlin
     * // Decidir pantalla inicial
     * val startDestination = if (TokenManager.hasToken()) {
     *     AppScreens.HomeScreen.route
     * } else {
     *     AppScreens.LoginScreen.route
     * }
     *
     * // Verificar abans de petició
     * if (TokenManager.hasToken()) {
     *     loadUserProfile()
     * } else {
     *     showLoginDialog()
     * }
     * ```
     *
     * @return `true` si existeix un token actiu, `false` si no n'hi ha
     *
     * @author Oscar
     * @since 1.0
     */
    fun hasToken(): Boolean = currentToken != null
}