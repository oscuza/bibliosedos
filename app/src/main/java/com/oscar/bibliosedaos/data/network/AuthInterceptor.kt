package com.oscar.bibliosedaos.data.network

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Interceptor d'OkHttp que afegeix automàticament el token d'autenticació
 * a totes les peticions HTTP sortints.
 *
 * Aquest interceptor s'executa abans de cada petició HTTP i realitza les
 * següents accions:
 *
 * **Funcions Principals:**
 * 1. **Injecció de Token:** Afegeix el token JWT a la capçalera `Authorization`
 * 2. **Detecció d'Errors:** Detecta errors 401/403 i neteja el token automàticament
 * 3. **Exclusió de Logout:** No neteja el token durant la petició de logout
 *
 * **Format del Token:**
 * ```
 * Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
 * ```
 *
 * **Flux d'Execució:**
 * ```
 * Petició Original
 *     │
 *     ▼
 * AuthInterceptor.intercept()
 *     │
 *     ├─→ Token existeix?
 *     │   ├─→ SÍ: Afegir header "Authorization: Bearer {token}"
 *     │   └─→ NO: Deixar petició sense modificar
 *     │
 *     ▼
 * Enviar petició al servidor
 *     │
 *     ▼
 * Rebre resposta
 *     │
 *     ├─→ Codi 401 o 403? (i no és logout)
 *     │   └─→ SÍ: TokenManager.clearToken()
 *     │
 *     ▼
 * Retornar resposta
 * ```
 *
 * **Errors Gestionats:**
 * - **401 Unauthorized:** Token invàlid o expirat
 * - **403 Forbidden:** Token vàlid però sense permisos
 *
 * @author Oscar
 * @since 1.0
 * @see TokenManager
 * @see ApiClient
 */
class AuthInterceptor : Interceptor {

    /**
     * Intercepta la petició HTTP i afegeix el header d'autorització si existeix token.
     *
     * Aquest mètode és cridat automàticament per OkHttp abans d'enviar qualsevol
     * petició HTTP. Modifica la petició original afegint el token JWT si està disponible.
     *
     * **Processos Realitzats:**
     * 1. Obté la petició HTTP original
     * 2. Consulta el token actual a [TokenManager]
     * 3. Si existeix token:
     *    - Crea una nova petició amb header `Authorization: Bearer {token}`
     * 4. Si no existeix token:
     *    - Deixa la petició sense modificar
     * 5. Envia la petició al servidor
     * 6. Analitza la resposta:
     *    - Si és 401/403 i NO és logout: neteja el token
     * 7. Retorna la resposta al client
     *
     * **Exemples de Headers Afegits:**
     * ```
     * GET /biblioteca/usuaris/llistarUsuaris HTTP/1.1
     * Host: 10.0.2.2:8080
     * Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
     * Content-Type: application/json
     * ```
     *
     * **Gestió d'Errors d'Autenticació:**
     * Quan el servidor retorna 401 o 403, normalment significa que:
     * - El token ha expirat
     * - El token ha estat revocat
     * - L'usuari no té permisos suficients
     *
     * En aquests casos, el token local es neteja automàticament per forçar
     * un nou login.
     *
     * **Excepció per Logout:**
     * Durant la petició de logout, NO es neteja el token automàticament
     * encara que rebi 401/403, perquè és el comportament esperat.
     *
     * @param chain Cadena d'interceptors d'OkHttp. Conté la petició original
     *              i permet continuar amb la següent operació
     * @return [Response] Resposta HTTP del servidor
     *
     * @author Oscar
     * @since 1.0
     * @see TokenManager.getToken
     * @see TokenManager.clearToken
     */
    override fun intercept(chain: Interceptor.Chain): Response {
        // 1. Obtenir la petició original
        val originalRequest = chain.request()
        val path = originalRequest.url.encodedPath

        val isAuthLogin = path.contains("/biblioteca/auth/login")
        val isLogout = path.contains("/biblioteca/auth/logout")

        // 2. Consultar si existeix token al TokenManager
        val token = TokenManager.getToken()

        // 3. Crear nova petició amb o sense token
        val request = if (token != null && !isAuthLogin) {
            // Token existeix: afegir header d'autorització
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            // Token no existeix: utilitzar petició original
            originalRequest
        }

        // 4. Enviar la petició i obtenir resposta
        val response = chain.proceed(request)

        // 5. Gestió d'errors d'autenticació
        // Si rebem 401 (Unauthorized) o 403 (Forbidden)
        // i NO és la petició de logout

        if ((response.code == 401 || response.code == 403) &&
            !isAuthLogin && !isLogout) {

            // Registrar l'error per debugging
            android.util.Log.e(
                "AuthInterceptor",
                "Error ${response.code} - Limpiando token"
            )

            // Netejar el token per forçar nou login
            TokenManager.clearToken()
        }

        // 6. Retornar la resposta
        return response
    }
}