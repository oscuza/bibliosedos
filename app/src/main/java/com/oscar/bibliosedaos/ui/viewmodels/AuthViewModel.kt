package com.oscar.bibliosedaos.ui.viewmodels

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oscar.bibliosedaos.data.network.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
class AuthViewModel : ViewModel() {

    private val api = ApiClient.instance

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
     * Realitza el login de l'usuari.
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
                val errorMessage = when {
                    e.message?.contains("401") == true -> "Credencials incorrectes"
                    e.message?.contains("403") == true -> "Accés denegat"
                    e.message?.contains("404") == true -> "Servei no disponible"
                    e.message?.contains("Unable to resolve") == true -> "Error de connexió"
                    else -> "Error al iniciar sessió: ${e.message}"
                }

                _loginUiState.value = LoginUiState(
                    isLoading = false,
                    error = errorMessage
                )
            }
        }
    }

    /**
     * Realitza el logout de l'usuari.
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
     * Carrega el perfil d'un usuari.
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
                _userProfileState.value = UserProfileUiState(
                    error = "Error carregant perfil: ${e.message}",
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
        email: String?,
        tlf: String?
    ) {
        val user = _currentUser.value ?: return

        viewModelScope.launch {
            _updateUserState.value = UpdateUserUiState(isUpdating = true)

            try {
                // Mantenim els camps obligatoris existents si no es proporcionen
                val updateRequest = UpdateUserRequest(
                    id = user.id,
                    nick = nick,
                    nom = nom,
                    cognom1 = cognom1,
                    cognom2 = cognom2,
                    rol = user.rol,
                    nif = user.nif ?: "000000000",
                    localitat = user.localitat ?: "Desconeguda",
                    carrer = user.carrer ?: "Desconegut",
                    cp = user.cp ?: "00000",
                    provincia = user.provincia ?: "Desconeguda",
                    tlf = tlf ?: user.tlf ?: "000000000",
                    email = email ?: user.email ?: "unknown@example.com"
                )

                val updatedUser = api.updateUser(user.id, updateRequest)

                _currentUser.value = updatedUser
                _updateUserState.value = UpdateUserUiState(
                    success = true,
                    updatedUser = updatedUser
                )
            } catch (e: Exception) {
                _updateUserState.value = UpdateUserUiState(
                    error = "Error actualitzant perfil: ${e.message}"
                )
            }
        }
    }

    // ==================== NOU: FUNCIONS DE GESTIÓ D'USUARIS PER PRÉSTECS ====================

    /**
     * Carrega tots els usuaris del sistema.
     * Només accessible per administradors.
     * Utilitzat per seleccionar usuari en préstecs.
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
                val errorMessage = when {
                    e.message?.contains("403") == true ->
                        "No tens permisos per veure els usuaris"
                    e.message?.contains("401") == true ->
                        "Sessió expirada, torna a iniciar sessió"
                    else ->
                        "Error carregant usuaris: ${e.message}"
                }
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
                val errorMessage = when {
                    e.message?.contains("404") == true -> "No s'ha trobat cap usuari amb aquest ID"
                    e.message?.contains("401") == true -> "Sessió expirada, torna a iniciar sessió"
                    else -> "Error cercant usuari: ${e.message}"
                }
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
                val errorMessage = when {
                    e.message?.contains("404") == true -> "No s'ha trobat cap usuari amb aquest NIF"
                    e.message?.contains("401") == true -> "Sessió expirada, torna a iniciar sessió"
                    else -> "Error cercant usuari: ${e.message}"
                }
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
                val errorMessage = when {
                    e.message?.contains("409") == true -> "El nick o email ja existeix"
                    e.message?.contains("400") == true -> "Dades invàlides"
                    e.message?.contains("403") == true -> "No tens permisos per crear usuaris"
                    else -> "Error creant usuari: ${e.message}"
                }

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
                api.deleteUser(userId)
                // Recarregar llista d'usuaris
                loadAllUsers()
                _deleteUserState.value = DeleteUserUiState(
                    success = true,
                    deletedUserId = userId
                )
            } catch (e: Exception) {
                val errorMessage = when {
                    e.message?.contains("404") == true -> "Usuari no trobat"
                    e.message?.contains("403") == true -> "No tens permisos per eliminar usuaris"
                    else -> "Error eliminant usuari: ${e.message}"
                }
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
                val errorMessage = when {
                    e.message?.contains("401") == true -> "Contrasenya actual incorrecta"
                    else -> "Error canviant contrasenya: ${e.message}"
                }

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
                val errorMessage = when {
                    e.message?.contains("404") == true -> "Usuari no trobat"
                    e.message?.contains("403") == true -> "No tens permisos per restablir contrasenyes"
                    else -> "Error restablint contrasenya: ${e.message}"
                }
                
                _updateUserState.value = UpdateUserUiState(
                    error = errorMessage
                )
            }
        }
    }

    // ==================== FUNCIONS D'UTILITAT ====================

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