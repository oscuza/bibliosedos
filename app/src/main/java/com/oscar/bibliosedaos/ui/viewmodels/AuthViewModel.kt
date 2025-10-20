package com.oscar.bibliosedaos.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oscar.bibliosedaos.data.network.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Estat de la interfície d'usuari per a la pantalla de login.
 * Data class immutable que encapsula l'estat complet del procés de login.
 * Segueix el patró d'arquitectura MVVM amb estats immutables.
 *
 * @property isLoading Indica si hi ha una operació de login en curs
 * @property loginSuccess Indica si el login ha estat exitós
 * @property error Missatge d'error en cas de fallada
 * @property authResponse Resposta del servidor amb dades de l'usuari autenticat
 *
 * @author Oscar
 * @since 1.0
 * @see AuthViewModel.login
 */
data class LoginUiState(
    val isLoading: Boolean = false,
    val loginSuccess: Boolean = false,
    val error: String? = null,
    val authResponse: AuthResponse? = null,
)

/**
 * Estat per a la llista d'usuaris en la vista d'administrador.
 * Data class que encapsula l'estat de la càrrega i visualització
 * de la llista completa d'usuaris del sistema.
 *
 * @property isLoading Indica si s'estan carregant els usuaris
 * @property users Llista d'usuaris carregats
 * @property error Missatge d'error en cas de fallada
 *
 * @author Oscar*
 * @since 1.0
 * @see AuthViewModel.login
 */
data class UserListState(
    val isLoading: Boolean = false,
    val users: List<User> = emptyList(),
    val error: String? = null,
)

/**
 * Estat per al perfil d'un usuari individual.
 * Data class que encapsula l'estat de la càrrega i visualització
 * del perfil d'un usuari específic.
 *
 * @property isLoading Indica si s'està carregant el perfil
 * @property user Dades de l'usuari carregat
 * @property error Missatge d'error en cas de fallada
 *
 * @author Oscar
 * @since 1.0
 * @see AuthViewModel.loadUserProfile
 */
data class UserProfileState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val error: String? = null,
)

/**
 * ViewModel principal per gestionar l'autenticació i operacions CRUD d'usuaris.
 *
 * Implementa el patró MVVM i proporciona funcions per:
 * - Autenticació (login/logout)
 * - Gestió d'usuaris (crear, llegir, actualitzar, eliminar)
 * - Manteniment d'estats observables amb StateFlow
 *
 * Utilitza el patró Observer per notificar canvis a la UI.
 *
 * @property api Instància del servei API per comunicació amb el servidor
 *
 *  @constructor Crea una instància del ViewModel amb el servei API
 *  @param api Servei API (per defecte: ApiClient.instance)
 *
 * @author Oscar
 * @since 1.0
 * @see AuthViewModel.login
 */
class AuthViewModel(
    private val api: AuthApiService = ApiClient.instance,
) : ViewModel() {

    /** DNI de prova per desenvolupament */
    private val _midni = MutableStateFlow("4634734N")
    val midni: StateFlow<String> = _midni.asStateFlow()

    /**
     * Estat observable del procés de login.
     * Utilitzat per la UI per mostrar l'estat actual de l'autenticació.
     */
    private val _loginUiState = MutableStateFlow(LoginUiState())
    val loginUiState: StateFlow<LoginUiState> = _loginUiState.asStateFlow()

    /**
     * Estat observable de la llista d'usuaris.
     * Utilitzat per AdminHomeScreen per mostrar tots els usuaris.
     */
    private val _userListState = MutableStateFlow(UserListState())
    val userListState: StateFlow<UserListState> = _userListState.asStateFlow()

    /**
     * Estat observable del perfil d'usuari.
     * Utilitzat per ProfileScreen i EditProfileScreen.
     */
    private val _userProfileState = MutableStateFlow(UserProfileState())
    val userProfileState: StateFlow<UserProfileState> = _userProfileState.asStateFlow()

    /**
     * Inicia sessió al sistema amb les credencials proporcionades.
     *
     * Procés:
     * 1. Envia credencials al servidor
     * 2. Si és exitós, guarda el token JWT
     * 3. Actualitza l'estat de la UI
     * 4. En cas d'error, neteja el token i mostra missatge
     *
     * @param nick Nom d'usuari únic
     * @param password Contrasenya de l'usuari
     *
     * @author Oscar
     * @since 1.0
     * @see TokenManager.saveToken
     * @see AuthApiService.login
     */
    fun login(nick: String, password: String) {
        _loginUiState.value = LoginUiState(isLoading = true)
        viewModelScope.launch {
            try {
                val request = AuthenticationRequest(nick, password)
                val response = api.login(request)
                TokenManager.saveToken(response.token)

                // Petit delay per assegurar que el token està llest
                delay(100)

                _loginUiState.value = LoginUiState(
                    isLoading = false,
                    loginSuccess = true,
                    authResponse = response
                )
            } catch (e: Exception) {
                TokenManager.clearToken()

                // Millora de missatges d'error segons codi HTTP
                val errorMessage = when {
                    e.message?.contains("403") == true -> "Usuari o contrasenya incorrectes"
                    e.message?.contains("401") == true -> "Credencials invàlides"
                    e.message?.contains("404") == true -> "Servei no disponible"
                    e.message?.contains("500") == true -> "Error al servidor. Intenta-ho més tard"
                    e.message?.contains("timeout") == true -> "Temps d'espera esgotat. Verifica la connexió"
                    e.message?.contains("Unable to resolve host") == true -> "Sense connexió al servidor"
                    e.message?.contains("Failed to connect") == true -> "No s'ha pogut connectar amb el servidor"
                    else -> "Error de connexió. Verifica la xarxa i intenta-ho de nou"
                }

                _loginUiState.value = LoginUiState(
                    isLoading = false,
                    error = errorMessage
                )
            }
        }
    }

    /**
     * Carrega la llista completa d'usuaris del sistema.
     * Funció destinada a administradors per veure tots els usuaris registrats.
     *
     * Actualitza userListState amb el resultat de l'operació.
     *
     * @author Oscar
     * @since 1.0
     * @see AuthApiService.getAllUsers
     * @see AdminHomeScreen
     */
    fun loadAllUsers() {
        viewModelScope.launch {
            _userListState.value = UserListState(isLoading = true)
            try {
                val users = api.getAllUsers()
                _userListState.value = UserListState(isLoading = false, users = users)
            } catch (e: Exception) {
                _userListState.value = UserListState(
                    isLoading = false,
                    error = "Error al carregar usuaris: ${e.message}"
                )
            }
        }
    }

    /**
     * Carrega el perfil d'un usuari específic.
     *
     * Utilitzat per mostrar les dades d'un usuari a ProfileScreen.
     * Actualitza userProfileState amb el resultat.
     *
     * @param userId Identificador únic de l'usuari a carregar
     *
     * @author Oscar
     * @since 1.0
     * @see AuthApiService.getUserById
     * @see ProfileScreen
     * @see EditProfileScreen
     */
    fun loadUserProfile(userId: Long) {
        viewModelScope.launch {
            _userProfileState.value = _userProfileState.value.copy(isLoading = true, error = null)

            try {
                val userProfile = api.getUserById(userId)
                _userProfileState.value = UserProfileState(isLoading = false, user = userProfile)
            } catch (e: Exception) {
                _userProfileState.value = UserProfileState(
                    isLoading = false,
                    error = "Error al carregar el perfil: ${e.message}"
                )
            }
        }
    }

    /**
     * Actualitza el perfil d'un usuari existent.
     *
     * Permet modificar les dades d'un usuari (nom, cognoms, etc.)
     * excepte el rol i l'ID que són immutables.
     *
     * @param userId ID de l'usuari a actualitzar
     * @param updatedUser Objecte User amb les dades actualitzades
     * @param onResult Callback amb el resultat (èxit: Boolean, missatge: String)
     *
     * @author Oscar
     *  @since 1.0
     *  @see AuthApiService.updateUser
     *  @see EditProfileScreen
     */
    fun updateUserProfile(userId: Long, updatedUser: User, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                // 1. Enviar actualització al servidor
                val response = api.updateUser(userId, updatedUser)
                // 2a. Actualitzar estat local amb dades noves
                _userProfileState.value = _userProfileState.value.copy(user = response)
                // 3. Notificar èxit
                onResult(true, "Perfil actualitzat correctament")
            } catch (e: Exception) {
                // 2b. Notificar error
                onResult(false, "Error al actualitzar: ${e.message}")
            }
        }
    }

    /**
     * Elimina un usuari del sistema.
     *
     * Funció només disponible per administradors.
     * No permet l'auto-eliminació (un admin no pot eliminar-se a si mateix).
     * Si l'operació és exitosa, recarrega la llista d'usuaris.
     *
     * @param userId ID de l'usuari a eliminar
     * @param onResult Callback amb el resultat de l'operació
     *
     * @author Oscar
     * @since 1.0
     * @see AuthApiService.deleteUser
     * @see AdminHomeScreen
     */
    fun deleteUser(userId: Long, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val response = api.deleteUser(userId)
                if (response.isSuccessful) {
                    loadAllUsers()
                    onResult(true, "Usuari eliminat correctament")
                } else {
                    onResult(false, "Error al eliminar: ${response.code()}")
                }
            } catch (e: Exception) {
                onResult(false, "Error de connexió: ${e.message}")
            }
        }
    }

    /**
     * Crea un nou usuari al sistema.
     *
     * Funció exclusiva per administradors.
     * Valida que el nick sigui únic i que tots els camps obligatoris estiguin presents.
     * Si l'operació és exitosa, recarrega la llista d'usuaris.
     *
     * @param nick Nom d'usuari únic
     * @param password Contrasenya (mínim 6 caràcters)
     * @param nombre Nom real de l'usuari
     * @param apellido1 Primer cognom
     * @param apellido2 Segon cognom (opcional)
     * @param rol Rol de l'usuari (1=Usuari normal, 2=Administrador)
     * @param onResult Callback amb el resultat de l'operació
     *
     * @author Oscar
     * @since 1.0
     * @see AuthApiService.createUser
     * @see CreateUserRequest
     * @see AddUserScreen
     */
    fun createUser(
        nick: String,
        password: String,
        nombre: String,
        apellido1: String,
        apellido2: String?,
        rol: Int,
        onResult: (Boolean, String) -> Unit,
    ) {
        viewModelScope.launch {
            try {
                val request = CreateUserRequest(
                    nick = nick,
                    password = password,
                    nombre = nombre,
                    apellido1 = apellido1,
                    rol = rol,
                    apellido2 = apellido2
                )

                val response = api.createUser(request)

                // Refrescar la llista si estem a la vista d'admin
                loadAllUsers()

                onResult(true, "Usuari '$nick' creat correctament")
            } catch (e: Exception) {
                val errorMessage = when {
                    e.message?.contains("409") == true -> "El nick '$nick' ja existeix"
                    e.message?.contains("400") == true -> "Dades invàlides"
                    e.message?.contains("401") == true -> "No autoritzat"
                    e.message?.contains("403") == true -> "Sense permisos per crear usuaris"
                    else -> "Error al crear usuari: ${e.message}"
                }
                onResult(false, errorMessage)
            }
        }
    }

    /**
     * Tanca la sessió de l'usuari actual.
     *
     * Implementa l'Opció 2.1 del disseny:
     * - Notifica al servidor del tancament
     * - Neteja el token local
     * - Reseteja tots els estats del ViewModel
     *
     * El mètode és "best effort" - si falla la comunicació amb el servidor,
     * igualment neteja l'estat local.
     *
     * @author Oscar
     * @since 1.0
     * @see TokenManager.clearToken
     * @see AuthApiService.logout
     * @see MainActivity.onDestroy
     */
    fun logout() {
        viewModelScope.launch {
            try {
                // Notificar al servidor
                api.logout()
            } catch (e: Exception) {
                // Log de l'error però continuar
                android.util.Log.e("AuthViewModel", "Error en logout: ${e.message}")
            } finally {
                // Netejar estat local sempre
                TokenManager.clearToken()
                _loginUiState.value = LoginUiState()
                _userListState.value = UserListState()
                _userProfileState.value = UserProfileState()
            }
        }
    }
}