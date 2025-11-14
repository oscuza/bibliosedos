package com.oscar.bibliosedaos.ui.viewmodels

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.oscar.bibliosedaos.data.network.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

/**
 * ViewModel per gestionar l'autenticació i usuaris de l'aplicació.
 *
 * Gestiona:
 * - Login i logout
 * - Perfils d'usuari
 * - CRUD d'usuaris (admin)
 * - Llista d'usuaris per préstecs
 *
 * @author Oscar
 * @version 2.0 - Afegida gestió d'usuaris per préstecs
 */
class AuthViewModel(
    private val api: AuthApiService = ApiClient.instance
) : ViewModel() {

    // ==================== ESTATS DE LOGIN ====================

    private val _loginUiState = MutableStateFlow(LoginUiState())
    val loginUiState: StateFlow<LoginUiState> = _loginUiState.asStateFlow()

    // ==================== ESTATS DE PERFIL ====================

    private val _userProfileState = MutableStateFlow(UserProfileUiState())
    val userProfileState: StateFlow<UserProfileUiState> = _userProfileState.asStateFlow()

    // ==================== ESTATS DE CREAR USUARI ====================

    private val _createUserState = MutableStateFlow(CreateUserUiState())
    val createUserState: StateFlow<CreateUserUiState> = _createUserState.asStateFlow()

    // ==================== ESTATS D'ACTUALITZAR USUARI ====================

    private val _updateUserState = MutableStateFlow(UpdateUserUiState())
    val updateUserState: StateFlow<UpdateUserUiState> = _updateUserState.asStateFlow()

    // ==================== ESTAT DEL USUARI ACTUAL ====================

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // ==================== NOU: ESTAT DE TOTS ELS USUARIS (per préstecs) ====================

    private val _allUsersState = MutableStateFlow(UsersUiState())
    val allUsersState: StateFlow<UsersUiState> = _allUsersState.asStateFlow()

    // ==================== ESTAT DE CERCA D'USUARIS ====================

    private val _userSearchState = MutableStateFlow(UserSearchUiState())
    val userSearchState: StateFlow<UserSearchUiState> = _userSearchState.asStateFlow()

    // ==================== ESTATS DE CANVI DE CONTRASENYA ====================

    private val _changePasswordState = MutableStateFlow(ChangePasswordUiState())
    val changePasswordState: StateFlow<ChangePasswordUiState> = _changePasswordState.asStateFlow()

    // ==================== FUNCIONS DE LOGIN ====================

    /**
     * Realitza l'autenticació de l'usuari al sistema.
     * 
     * Aquest mètode realitza una petició HTTP POST al servidor per autenticar
     * l'usuari amb les credencials proporcionades (nick i password).
     * 
     * **Processos Realitzats:**
     * 1. Envia les credencials al servidor
     * 2. Si l'autenticació és exitosa:
     *    - Guarda el token JWT a [TokenManager]
     *    - Guarda les dades de l'usuari actual
     *    - Actualitza l'estat de login
     * 3. Si l'autenticació falla:
     *    - Actualitza l'estat amb un missatge d'error
     * 
     * **Estat de Login:**
     * - Abans de la petició: `isLoading = true`
     * - Després de l'èxit: `isLoading = false`, `loginSuccess = true`, `authResponse = [resposta]`
     * - Després de l'error: `isLoading = false`, `error = [missatge d'error]`
     * 
     * **Errors Possibles:**
     * - Error 401: Credencials incorrectes
     * - Error 404: L'usuari no existeix
     * - Error de xarxa: Problemes de connexió amb el servidor
     * 
     * **Seguretat:**
     * El token JWT s'emmagatzema automàticament a [TokenManager] després
     * d'un login exitós. Aquest token s'utilitzarà per a totes les peticions
     * HTTP posteriors mitjançant [AuthInterceptor].
     * 
     * @param nick Nom d'usuari (nickname) per autenticar
     * @param password Contrasenya de l'usuari
     * 
     * @author Oscar
     * @since 1.0
     * @see LoginUiState
     * @see TokenManager.saveToken
     * @see AuthApiService.login
     */
    fun login(nick: String, password: String) {
        viewModelScope.launch {
            _loginUiState.value = LoginUiState(isLoading = true)

            try {
                val authResponse = api.login(AuthenticationRequest(nick, password))

                // Guardar el token
                TokenManager.saveToken(authResponse.token)

                // Guardar l'usuari actual
                _currentUser.value = User(
                    id = authResponse.id,
                    nick = authResponse.nick ?: nick,
                    nom = authResponse.nom ?: "",
                    cognom1 = authResponse.cognom1,
                    cognom2 = authResponse.cognom2,
                    rol = authResponse.rol
                )

                _loginUiState.value = LoginUiState(
                    isLoading = false,
                    loginSuccess = true,
                    authResponse = authResponse
                )
            } catch (e: Exception) {
                val errorMessage = handleHttpError(e, "Error al iniciar sessió")
                _loginUiState.value = LoginUiState(
                    isLoading = false,
                    error = errorMessage
                )
            }
        }
    }

    /**
     * Tanca la sessió de l'usuari al sistema.
     * 
     * Aquest mètode realitza una petició HTTP POST al servidor per tancar
     * la sessió i elimina el token JWT de la memòria local.
     * 
     * **Processos Realitzats:**
     * 1. Intenta notificar al servidor del logout (opcional)
     * 2. Neteja el token JWT de [TokenManager] (sempre)
     * 3. Elimina les dades de l'usuari actual
     * 4. Restableix l'estat de login
     * 
     * **Seguretat:**
     * El token sempre s'elimina localment, encara que falli la comunicació
     * amb el servidor. Això assegura que la sessió es tanqui completament
     * en el client.
     * 
     * **Nota:**
     * El logout al servidor és opcional i no bloqueja la neteja local del token.
     * Si hi ha problemes de connexió, el token es neteja igualment per seguretat.
     * 
     * **Efectes:**
     * - [TokenManager.clearToken]: Elimina el token JWT
     * - `_currentUser = null`: Elimina les dades de l'usuari actual
     * - `_loginUiState = LoginUiState()`: Restableix l'estat de login
     * 
     * @author Oscar
     * @since 1.0
     * @see TokenManager.clearToken
     * @see AuthApiService.logout
     */
    fun logout() {
        viewModelScope.launch {
            try {
                api.logout()
            } catch (e: Exception) {
                // Si falla el logout al servidor, no importa
            } finally {
                TokenManager.clearToken()
                _currentUser.value = null
                _loginUiState.value = LoginUiState()
            }
        }
    }

    // ==================== FUNCIONS DE PERFIL ====================

    /**
     * Carrega el perfil d'un usuari des del backend.
     * 
     * Aquest mètode realitza una petició HTTP GET al servidor per obtenir
     * les dades completes d'un usuari específic.
     * 
     * **Ús:**
     * - Veure el perfil propi
     * - Veure el perfil d'altres usuaris (administradors)
     * - Carregar dades d'usuari per editar
     * 
     * **Estat de Càrrega:**
     * - Abans de la petició: `isLoading = true`
     * - Després de l'èxit: `isLoading = false`, `user = [dades de l'usuari]`
     * - Després de l'error: `isLoading = false`, `error = [missatge d'error]`
     * 
     * **Permisos:**
     * - Usuari normal: Pot carregar només el seu propi perfil
     * - Administrador: Pot carregar el perfil de qualsevol usuari
     * 
     * **Errors Possibles:**
     * - Error 404: L'usuari no existeix
     * - Error 403: No tens permisos per veure aquest perfil
     * - Error de xarxa: Problemes de connexió amb el servidor
     * 
     * @param userId Identificador únic de l'usuari del qual carregar el perfil
     * 
     * @author Oscar
     * @since 1.0
     * @see UserProfileUiState
     * @see AuthApiService.getUserById
     */
    fun loadUserProfile(userId: Long) {
        viewModelScope.launch {
            _userProfileState.value = UserProfileUiState(isLoading = true)

            try {
                val user = api.getUserById(userId)
                _userProfileState.value = UserProfileUiState(
                    user = user,
                    isLoading = false
                )
            } catch (e: Exception) {
                val errorMessage = handleHttpError(e, "Error carregant perfil")
                _userProfileState.value = UserProfileUiState(
                    error = errorMessage,
                    isLoading = false
                )
            }
        }
    }

    /**
     * Actualitza el perfil de l'usuari actual.
     */
    fun updateCurrentUserProfile(
        nick: String,
        nom: String,
        cognom1: String,
        cognom2: String?,
        nif: String?,
        email: String?,
        tlf: String?,
        carrer: String?,
        localitat: String?,
        cp: String?,
        provincia: String?
    ) {
        val user = _currentUser.value ?: return
        updateUserProfile(
            userId = user.id,
            nick = nick,
            nom = nom,
            cognom1 = cognom1,
            cognom2 = cognom2,
            nif = nif,
            email = email,
            tlf = tlf,
            carrer = carrer,
            localitat = localitat,
            cp = cp,
            provincia = provincia,
            updateCurrentUser = true
        )
    }

    /**
     * Actualitza el perfil d'un usuari (pot ser l'actual o un altre).
     * Utilitzat per administradors per editar altres usuaris.
     *
     * @param userId ID de l'usuari a actualitzar
     * @param updateCurrentUser Si és true, actualitza també _currentUser (per edició del propi perfil)
     */
    fun updateUserProfile(
        userId: Long,
        nick: String,
        nom: String,
        cognom1: String,
        cognom2: String?,
        nif: String?,
        email: String?,
        tlf: String?,
        carrer: String?,
        localitat: String?,
        cp: String?,
        provincia: String?,
        updateCurrentUser: Boolean = false
    ) {
        viewModelScope.launch {
            _updateUserState.value = UpdateUserUiState(isUpdating = true)

            try {
                // Carregar les dades actuals de l'usuari que es vol actualitzar
                val user = api.getUserById(userId)

                // Utilitzem els valors proporcionats, o mantenim els existents si no es proporcionen
                val updateRequest = UpdateUserRequest(
                    id = user.id,
                    nick = nick,
                    nom = nom,
                    cognom1 = cognom1,
                    cognom2 = cognom2,
                    rol = user.rol,
                    nif = nif?.takeIf { it.isNotBlank() } ?: (user.nif ?: "000000000"),
                    localitat = localitat?.takeIf { it.isNotBlank() } ?: (user.localitat ?: "Desconeguda"),
                    carrer = carrer?.takeIf { it.isNotBlank() } ?: (user.carrer ?: "Desconegut"),
                    cp = cp?.takeIf { it.isNotBlank() } ?: (user.cp ?: "00000"),
                    provincia = provincia?.takeIf { it.isNotBlank() } ?: (user.provincia ?: "Desconeguda"),
                    tlf = tlf?.takeIf { it.isNotBlank() } ?: (user.tlf ?: "000000000"),
                    email = email?.takeIf { it.isNotBlank() } ?: (user.email ?: "unknown@example.com")
                )

                val updatedUser = api.updateUser(userId, updateRequest)

                // Si s'està actualitzant el propi perfil, actualitzar també _currentUser
                if (updateCurrentUser) {
                    _currentUser.value = updatedUser
                }

                _updateUserState.value = UpdateUserUiState(
                    success = true,
                    updatedUser = updatedUser
                )
            } catch (e: Exception) {
                val errorMessage = handleHttpError(e, "Error actualitzant perfil")
                _updateUserState.value = UpdateUserUiState(
                    error = errorMessage
                )
            }
        }
    }

    // ==================== NOU: FUNCIONS DE GESTIÓ D'USUARIS PER PRÉSTECS ====================

    /**
     * Carrega tots els usuaris del sistema des del backend.
     * 
     * Aquest mètode realitza una petició HTTP GET al servidor per obtenir
     * la llista completa d'usuaris registrats al sistema.
     * 
     * **Ús:**
     * - Llistar tots els usuaris (pantalla d'administració)
     * - Seleccionar un usuari per crear préstecs
     * - Gestió d'usuaris per administradors
     * 
     * **Estat de Càrrega:**
     * - Abans de la petició: `isLoading = true`
     * - Després de l'èxit: `isLoading = false`, `users = [llista d'usuaris]`
     * - Després de l'error: `isLoading = false`, `error = [missatge d'error]`
     * 
     * **Permisos:**
     * - Només administradors poden carregar tots els usuaris
     * - Els usuaris normals no tenen accés a aquesta funcionalitat
     * 
     * **Errors Possibles:**
     * - Error 403: No tens permisos d'administrador
     * - Error de xarxa: Problemes de connexió amb el servidor
     * 
     * @author Oscar
     * @since 2.0
     * @see UsersUiState
     * @see AuthApiService.getAllUsers
     */
    fun loadAllUsers() {
        viewModelScope.launch {
            _allUsersState.value = UsersUiState(isLoading = true)
            try {
                val users = api.getAllUsers()
                _allUsersState.value = UsersUiState(
                    users = users,
                    isLoading = false
                )
            } catch (e: Exception) {
                val errorMessage = handleHttpError(e, "Error carregant usuaris")
                _allUsersState.value = UsersUiState(
                    error = errorMessage,
                    isLoading = false
                )
            }
        }
    }

    /**
     * Cerca usuaris per nom, nick o email.
     * Filtra la llista actual d'usuaris.
     */
    fun searchUsers(query: String) {
        if (query.isEmpty()) {
            loadAllUsers()
            return
        }

        val filteredUsers = _allUsersState.value.users.filter { user ->
            user.nom.contains(query, ignoreCase = true) ||
                    user.nick.contains(query, ignoreCase = true) ||
                    user.email?.contains(query, ignoreCase = true) == true ||
                    user.cognom1?.contains(query, ignoreCase = true) == true ||
                    user.cognom2?.contains(query, ignoreCase = true) == true
        }

        _allUsersState.value = _allUsersState.value.copy(
            users = filteredUsers
        )
    }

    /**
     * Obté un usuari específic per ID.
     * Útil per obtenir informació d'un usuari en préstecs.
     */
    fun getUserById(userId: Long): User? {
        return _allUsersState.value.users.find { it.id == userId }
    }

    /**
     * Refresca la llista d'usuaris.
     */
    fun refreshUsers() {
        loadAllUsers()
    }

    // ==================== FUNCIONS DE CERCA D'USUARIS ====================

    /**
     * Cerca un usuari per ID.
     */
    fun searchUserById(userIdString: String) {
        viewModelScope.launch {
            _userSearchState.value = UserSearchUiState(isSearching = true)
            
            try {
                val userId = userIdString.toLongOrNull()
                if (userId == null) {
                    _userSearchState.value = UserSearchUiState(
                        error = "ID invàlid. Ha de ser un número."
                    )
                    return@launch
                }
                
                val user = api.getUserById(userId)
                _userSearchState.value = UserSearchUiState(
                    hasSearched = true,
                    searchResult = user
                )
            } catch (e: Exception) {
                val errorMessage = handleHttpError(e, "Error cercant usuari")
                _userSearchState.value = UserSearchUiState(
                    hasSearched = true,
                    error = errorMessage
                )
            }
        }
    }

    /**
     * Cerca un usuari per NIF.
     */
    fun searchUserByNif(nif: String) {
        viewModelScope.launch {
            _userSearchState.value = UserSearchUiState(isSearching = true)
            
            try {
                val response = api.getUserByNif(nif)
                if (response.isSuccessful && response.body() != null) {
                    _userSearchState.value = UserSearchUiState(
                        hasSearched = true,
                        searchResult = response.body()!!
                    )
                } else {
                    _userSearchState.value = UserSearchUiState(
                        hasSearched = true,
                        error = "No s'ha trobat cap usuari amb aquest NIF"
                    )
                }
            } catch (e: Exception) {
                val errorMessage = handleHttpError(e, "Error cercant usuari")
                _userSearchState.value = UserSearchUiState(
                    hasSearched = true,
                    error = errorMessage
                )
            }
        }
    }

    /**
     * Neteja l'estat de cerca.
     */
    fun clearSearch() {
        _userSearchState.value = UserSearchUiState()
    }

    // ==================== FUNCIONS DE CREAR USUARI ====================

    /**
     * Crea un nou usuari (només admin).
     */
    fun createUser(
        nick: String,
        password: String,
        nom: String,
        cognom1: String,
        cognom2: String?,
        rol: Int,
        nif: String,
        localitat: String,
        carrer: String,
        cp: String,
        provincia: String,
        tlf: String,
        email: String
    ) {
        viewModelScope.launch {
            _createUserState.value = CreateUserUiState(isCreating = true)

            try {
                val request = CreateUserRequest(
                    nick = nick,
                    password = password,
                    nom = nom,
                    cognom1 = cognom1,
                    cognom2 = cognom2,
                    rol = rol,
                    nif = nif,
                    localitat = localitat,
                    carrer = carrer,
                    cp = cp,
                    provincia = provincia,
                    tlf = tlf,
                    email = email
                )

                val response = api.createUser(request)

                _createUserState.value = CreateUserUiState(
                    success = true,
                    createdUser = response
                )

                // Recarregar llista d'usuaris
                loadAllUsers()

            } catch (e: Exception) {
                val errorMessage = handleHttpError(e, "Error creant usuari")
                _createUserState.value = CreateUserUiState(
                    error = errorMessage
                )
            }
        }
    }

    // ==================== FUNCIONS D'ELIMINAR USUARI ====================

    private val _deleteUserState = MutableStateFlow(DeleteUserUiState())
    val deleteUserState: StateFlow<DeleteUserUiState> = _deleteUserState.asStateFlow()

    /**
     * Elimina un usuari (només admin).
     */
    fun deleteUser(userId: Long) {
        viewModelScope.launch {
            _deleteUserState.value = DeleteUserUiState(isDeleting = true)
            
            try {
                val response = api.deleteUser(userId)
                if (response.isSuccessful) {
                    // Recarregar llista d'usuaris
                    loadAllUsers()
                    _deleteUserState.value = DeleteUserUiState(
                        success = true,
                        deletedUserId = userId
                    )
                } else {
                    // Si la resposta no és exitosa, llançar HttpException per gestionar l'error
                    throw retrofit2.HttpException(response)
                }
            } catch (e: Exception) {
                val errorMessage = handleHttpError(e, "Error eliminant usuari")
                _deleteUserState.value = DeleteUserUiState(
                    error = errorMessage
                )
            }
        }
    }

    // ==================== FUNCIONS DE CANVI DE CONTRASENYA ====================

    /**
     * Canvia la contrasenya de l'usuari actual.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun changePassword(currentPassword: String, newPassword: String) {
        val user = _currentUser.value ?: return

        viewModelScope.launch {
            _changePasswordState.value = ChangePasswordUiState(isChanging = true)

            try {
                // Primer verificar la contrasenya actual fent login
                val authRequest = AuthenticationRequest(user.nick, currentPassword)
                api.login(authRequest)

                // Si el login és exitós, actualitzar la contrasenya
                val updateRequest = UpdateUserRequest(
                    id = user.id,
                    nick = user.nick,
                    nom = user.nom,
                    cognom1 = user.cognom1 ?: "",
                    cognom2 = user.cognom2,
                    rol = user.rol,
                    nif = user.nif ?: "000000000",
                    localitat = user.localitat ?: "Desconeguda",
                    carrer = user.carrer ?: "Desconegut",
                    cp = user.cp ?: "00000",
                    provincia = user.provincia ?: "Desconeguda",
                    tlf = user.tlf ?: "000000000",
                    email = user.email ?: "unknown@example.com",
                    password = newPassword
                )

                api.updateUser(user.id, updateRequest)

                _changePasswordState.value = ChangePasswordUiState(
                    success = true
                )
            } catch (e: Exception) {
                val errorMessage = handleHttpError(e, "Error canviant contrasenya")
                _changePasswordState.value = ChangePasswordUiState(
                    error = errorMessage
                )
            }
        }
    }

    /**
     * Restableix la contrasenya d'un altre usuari (només admin).
     * Utilitzat per administradors per restablir contrasenyes d'altres usuaris.
     */
    fun resetUserPassword(userId: Long, newPassword: String) {
        viewModelScope.launch {
            _updateUserState.value = UpdateUserUiState(isUpdating = true)
            
            try {
                // Carregar les dades de l'usuari per obtenir tots els camps necessaris
                val user = api.getUserById(userId)
                
                // Crear UpdateUserRequest amb la nova contrasenya
                val updateRequest = UpdateUserRequest(
                    id = user.id,
                    nick = user.nick,
                    nom = user.nom,
                    cognom1 = user.cognom1 ?: "",
                    cognom2 = user.cognom2,
                    rol = user.rol,
                    nif = user.nif ?: "000000000",
                    localitat = user.localitat ?: "Desconeguda",
                    carrer = user.carrer ?: "Desconegut",
                    cp = user.cp ?: "00000",
                    provincia = user.provincia ?: "Desconeguda",
                    tlf = user.tlf ?: "000000000",
                    email = user.email ?: "unknown@example.com",
                    password = newPassword
                )
                
                val updatedUser = api.updateUser(userId, updateRequest)
                
                _updateUserState.value = UpdateUserUiState(
                    success = true,
                    updatedUser = updatedUser
                )
            } catch (e: Exception) {
                val errorMessage = handleHttpError(e, "Error restablint contrasenya")
                _updateUserState.value = UpdateUserUiState(
                    error = errorMessage
                )
            }
        }
    }

    // ==================== FUNCIONS D'UTILITAT ====================

    /**
     * Helper per gestionar errors HTTP de forma consistent.
     * Detecta el codi HTTP i intenta parsear el missatge d'error del backend.
     *
     * @param e L'excepció capturada
     * @param defaultMessage Missatge per defecte si no es pot determinar l'error
     * @return Missatge d'error amigable per mostrar a l'usuari
     */
    private fun handleHttpError(e: Exception, defaultMessage: String = "Error desconegut"): String {
        return when (e) {
            is HttpException -> {
                val code = e.code()
                val errorBody = try {
                    e.response()?.errorBody()?.string()
                } catch (ex: IOException) {
                    null
                }

                // Intentar parsear el errorBody com ErrorMessage
                val errorMessage = errorBody?.let { body ->
                    try {
                        val gson = Gson()
                        val error = gson.fromJson(body, ErrorMessage::class.java)
                        error.message
                    } catch (ex: Exception) {
                        null
                    }
                }

                // Si tenim un missatge del backend, l'utilitzem
                val backendMessage = errorMessage ?: errorBody

                when (code) {
                    409 -> {
                        // CONFLICT - Email/Nick duplicat
                        when {
                            backendMessage?.contains("llave duplicada", ignoreCase = true) == true ||
                            backendMessage?.contains("clau duplicada", ignoreCase = true) == true ||
                            backendMessage?.contains("ya existe", ignoreCase = true) == true ||
                            backendMessage?.contains("ja existeix", ignoreCase = true) == true -> {
                                "L'email o nick ja està en ús per un altre usuari"
                            }
                            backendMessage != null -> backendMessage
                            else -> "L'email o nick ja està en ús per un altre usuari"
                        }
                    }
                    400 -> {
                        // BAD REQUEST - Dades invàlides
                        backendMessage ?: "Dades invàlides. Si us plau, revisa els camps"
                    }
                    401 -> {
                        // UNAUTHORIZED - No autenticat
                        "Sessió expirada, torna a iniciar sessió"
                    }
                    403 -> {
                        // FORBIDDEN - Sense permisos
                        backendMessage ?: "No tens permisos per realitzar aquesta acció"
                    }
                    404 -> {
                        // NOT FOUND
                        backendMessage ?: "Recurs no trobat"
                    }
                    else -> {
                        backendMessage ?: "Error del servidor (codi $code)"
                    }
                }
            }
            is IOException -> {
                "Error de connexió. Comprova la teva connexió a Internet"
            }
            else -> {
                // Per altres tipus d'errors, buscar indicadors al missatge
                val message = e.message ?: ""
                when {
                    message.contains("409", ignoreCase = true) ||
                    message.contains("CONFLICT", ignoreCase = true) ||
                    message.contains("duplicada", ignoreCase = true) ||
                    message.contains("duplicado", ignoreCase = true) ||
                    message.contains("ya existe", ignoreCase = true) ||
                    message.contains("ja existeix", ignoreCase = true) -> {
                        "L'email o nick ja està en ús per un altre usuari"
                    }
                    message.contains("Unable to resolve", ignoreCase = true) ||
                    message.contains("Failed to connect", ignoreCase = true) -> {
                        "Error de connexió. Comprova la teva connexió a Internet"
                    }
                    else -> {
                        defaultMessage + (if (message.isNotEmpty()) ": $message" else "")
                    }
                }
            }
        }
    }

    /**
     * Neteja els errors de tots els estats.
     */
    fun clearErrors() {
        _loginUiState.value = _loginUiState.value.copy(error = null)
        _userProfileState.value = _userProfileState.value.copy(error = null)
        _createUserState.value = _createUserState.value.copy(error = null)
        _updateUserState.value = _updateUserState.value.copy(error = null)
        _allUsersState.value = _allUsersState.value.copy(error = null)
        _changePasswordState.value = _changePasswordState.value.copy(error = null)
    }

    /**
     * Comprova si l'usuari actual és administrador.
     */
    fun isAdmin(): Boolean {
        return _currentUser.value?.rol == 2
    }

    /**
     * Obté l'ID de l'usuari actual.
     */
    fun getCurrentUserId(): Long? {
        return _currentUser.value?.id
    }
}

// ==================== UI STATES ====================

/**
 * Estat del procés de login.
 */
data class LoginUiState(
    val isLoading: Boolean = false,
    val loginSuccess: Boolean = false,
    val error: String? = null,
    val authResponse: AuthResponse? = null
)

/**
 * Estat del perfil d'usuari.
 */
data class UserProfileUiState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * Estat de creació d'usuari.
 */
data class CreateUserUiState(
    val isCreating: Boolean = false,
    val success: Boolean = false,
    val createdUser: AuthResponse? = null,
    val error: String? = null
)

/**
 * Estat d'actualització d'usuari.
 */
data class UpdateUserUiState(
    val isUpdating: Boolean = false,
    val success: Boolean = false,
    val updatedUser: User? = null,
    val error: String? = null
)

/**
 * NOU: Estat de la llista d'usuaris (per préstecs).
 */
data class UsersUiState(
    val users: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * Estat de l'eliminació d'usuari.
 */
data class DeleteUserUiState(
    val isDeleting: Boolean = false,
    val success: Boolean = false,
    val deletedUserId: Long? = null,
    val error: String? = null
)

/**
 * Estat del canvi de contrasenya.
 */
data class ChangePasswordUiState(
    val isChanging: Boolean = false,
    val success: Boolean = false,
    val error: String? = null
)

/**
 * Estat de la cerca d'usuaris per ID o NIF.
 */
data class UserSearchUiState(
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false,
    val searchResult: User? = null,
    val error: String? = null
)