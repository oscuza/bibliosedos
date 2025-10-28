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
    val isLoading: Boolean = true,
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
 * Estat per a la cerca d'usuaris.
 * Data class que encapsula l'estat de la cerca d'usuaris per ID o NIF.
 *
 * @property isSearching Indica si s'està realitzant una cerca
 * @property searchResult Usuari trobat (null si no s'ha trobat)
 * @property error Missatge d'error en cas de fallada
 * @property hasSearched Indica si s'ha realitzat almenys una cerca
 *
 * @author Oscar
 * @since 1.0
 * @see AuthViewModel.searchUserById
 * @see AuthViewModel.searchUserByNif
 */
data class UserSearchState(
    val isSearching: Boolean = false,
    val searchResult: User? = null,
    val error: String? = null,
    val hasSearched: Boolean = false
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
     * Estat observable de la cerca d'usuaris.
     * Utilitzat per UserSearchScreen per mostrar resultats de cerca.
     */
    private val _userSearchState = MutableStateFlow(UserSearchState())
    val userSearchState: StateFlow<UserSearchState> = _userSearchState.asStateFlow()

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
     * Aquest mètode gestiona automàticament els camps obligatoris del backend:
     * 1. Primer obté l'usuari actual amb TOTS els seus camps
     * 2. Manté els valors existents dels camps no editables (nif, localitat, etc.)
     * 3. Només actualitza els camps que l'usuari pot modificar
     * 4. Envia TOTS els camps al backend per complir les validacions
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
                // PAS 1: Obtenir l'usuari actual amb TOTS els seus camps
                val currentUser = api.getUserById(userId)

                // PAS 2: Crear el request amb els camps actualitzats + els camps existents
                val updateRequest = UpdateUserRequest(
                    id = userId,
                    nick = updatedUser.nick,
                    nom = updatedUser.nom,
                    cognom1 = updatedUser.cognom1 ?: currentUser.cognom1 ?: "",
                    cognom2 = when {
                        updatedUser.cognom2.isNullOrEmpty() -> " "  // DESPRÉS
                        else -> updatedUser.cognom2
                    },
                    rol = updatedUser.rol,
                    nif = updatedUser.nif ?: currentUser.nif ?: "",
                    carrer = updatedUser.carrer ?: currentUser.carrer ?: "",
                    localitat = updatedUser.localitat ?: currentUser.localitat ?: "",
                    provincia = updatedUser.provincia ?: currentUser.provincia ?: "",
                    cp = updatedUser.cp ?: currentUser.cp ?: "",
                    tlf = updatedUser.tlf ?: currentUser.tlf ?: "",
                    email = updatedUser.email ?: currentUser.email ?: ""
                )

                // PAS 3: Enviar actualització al servidor
                val response = api.updateUser(userId, updateRequest)

                // PAS 4: Actualitzar estat local amb la resposta
                _userProfileState.value = _userProfileState.value.copy(user = response)
                onResult(true, "Perfil actualitzat correctament")

            } catch (e: Exception) {
                // Gestió d'errors amb missatges específics
                val errorMessage = when {
                    e.message?.contains("409") == true -> {
                        when {
                            e.message?.contains("nick") == true -> "El nick ja existeix"
                            e.message?.contains("nif") == true -> "El NIF ja està en ús"
                            e.message?.contains("email") == true -> "L'email ja està en ús"
                            else -> "Dades duplicades"
                        }
                    }
                    e.message?.contains("400") == true -> "Dades invàlides. Verifica tots els camps"
                    e.message?.contains("403") == true -> "No tens permisos per actualitzar aquest usuari"
                    e.message?.contains("404") == true -> "Usuari no trobat"
                    else -> "Error al actualitzar: ${e.message}"
                }
                onResult(false, errorMessage)
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
                    // Refrescar la llista després d'eliminar
                    loadAllUsers()
                    clearUserProfileState()
                    onResult(true, "Usuari eliminat correctament")
                } else {
                    // Error del servidor
                    val errorMessage = when (response.code()) {
                        403 -> "No pots eliminar-te a tu mateix"
                        404 -> "Usuari no trobat"
                        else -> "Error al eliminar: ${response.code()}"
                    }
                    onResult(false, errorMessage)
                }
            } catch (e: Exception) {
                // Error de connexió
                onResult(false, "Error de connexió: ${e.message}")
            }
        }
    }

    /**
     * Crea un nou usuari al sistema.
     *
     * Funció exclusiva per administradors.
     * Aquest mètode gestiona automàticament els camps obligatoris:
     *  - Envia valors per defecte per als camps que el backend marca com obligatoris
     *  - Aquests valors es poden actualitzar més endavant
     *
     * @param nick Nom d'usuari únic
     * @param password Contrasenya (mínim 6 caràcters)
     * @param nom Nom real de l'usuari
     * @param cognom1 Primer cognom
     * @param cognom2 Segon cognom (opcional)
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
        cognom1: String,
        cognom2: String?,
        rol: Int,
        nif: String,
        email: String,
        tlf: String,
        carrer: String,
        localitat: String,
        cp: String,
        provincia: String,
        onResult: (Boolean, String) -> Unit,
    ) {
        viewModelScope.launch {
            try {
                // Crear request amb TOTS els camps obligatoris
                val request = CreateUserRequest(
                    nick = nick,
                    password = password,
                    nom = nombre,
                    cognom1 = cognom1,
                    cognom2 = cognom2,
                    rol = rol,
                    // CAMPS OBLIGATORIS AMB VALORS PER DEFECTE
                    // Aquests valors són temporals i es poden actualitzar més endavant
                    nif = nif,
                    carrer = carrer,
                    localitat = localitat,
                    provincia = provincia,
                    cp = cp,
                    tlf = tlf,
                    email = email
                )

                // Enviar petició al servidor
                val response = api.createUser(request)

                // Refrescar la llista d'usuaris si estem a la vista d'admin
                loadAllUsers()

                onResult(true, "Usuari '$nick' creat correctament")

            } catch (e: Exception) {
                // Gestió d'errors amb missatges específics en català
                val errorMessage = when {
                    e.message?.contains("409") == true -> {
                        when {
                            e.message?.contains("nick") == true -> "El nick '$nick' ja existeix"
                            e.message?.contains("nif") == true -> "El NIF ja està registrat"
                            e.message?.contains("email") == true -> "L'email ja està registrat"
                            else -> "Dades duplicades. Verifica nick, NIF i email"
                        }
                    }

                    e.message?.contains("400") == true -> "Dades invàlides. Verifica tots els camps"
                    e.message?.contains("403") == true -> "No tens permisos per crear usuaris"
                    else -> "Error al crear usuari: ${e.message}"
                }
                onResult(false, errorMessage)
            }
        }
    }
    /**
     * Actualització completa amb TOTS els camps (per EditProfileScreen actualitzat)
     */
    fun updateCompleteProfile(
        userId: Long,
        nick: String,
        nom: String,
        cognom1: String,
        cognom2: String?,
        nif: String,
        email: String,
        tlf: String,
        carrer: String,
        localitat: String,
        cp: String,
        provincia: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // Obtenir l'usuari actual per mantenir el rol i password
                val currentUser = api.getUserById(userId)

                val updateRequest = UpdateUserRequest(
                    id = userId,
                    nick = nick,
                    nom = nom,
                    cognom1 = cognom1,
                    cognom2 = when {
                        cognom2.isNullOrBlank() -> " "  // Espai per camps buits
                        else -> cognom2
                    },
                    rol = currentUser.rol,  // Mantenir el rol actual
                    nif = nif,
                    carrer = carrer,
                    localitat = localitat,
                    provincia = provincia,
                    cp = cp,
                    tlf = tlf,
                    email = email
                    // No enviem password en actualització normal
                )

                val response = api.updateUser(userId, updateRequest)
                _userProfileState.value = _userProfileState.value.copy(user = response)
                onResult(true, "Perfil actualitzat correctament")

            } catch (e: Exception) {
                val errorMessage = when {
                    e.message?.contains("409") == true -> "Dades duplicades (nick, NIF o email)"
                    e.message?.contains("400") == true -> "Dades invàlides"
                    else -> "Error: ${e.message}"
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

    /**
     * Canvia la contrasenya de l'usuari.
     *
     * IMPORTANT: Aquesta funció requereix verificar la contrasenya actual
     * abans d'actualitzar-la amb la nova.
     *
     * @param userId ID de l'usuari
     * @param currentPassword Contrasenya actual per verificació
     * @param newPassword Nova contrasenya
     * @param onResult Callback amb el resultat (èxit: Boolean, missatge: String)
     */
    fun changePassword(
        userId: Long,
        currentPassword: String,
        newPassword: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // Primer, verificar que la contrasenya actual és correcta
                // Això es pot fer intentant fer login amb les credencials actuals
                val user = _userProfileState.value.user
                if (user == null) {
                    onResult(false, "No s'ha pogut obtenir la informació de l'usuari")
                    return@launch
                }

                // Verificar contrasenya actual fent login
                try {
                    val authRequest = AuthenticationRequest(
                        nick = user.nick,
                        password = currentPassword
                    )
                    api.login(authRequest) // Si falla, la contrasenya actual és incorrecta
                } catch (e: Exception) {
                    onResult(false, "La contrasenya actual no és correcta")
                    return@launch
                }

                // Si arribem aquí, la contrasenya actual és correcta
                // Ara actualitzem amb la nova contrasenya
                val currentUserData = api.getUserById(userId)

                val updateRequest = UpdateUserRequest(
                    id = userId,
                    nick = currentUserData.nick,
                    nom = currentUserData.nom,
                    cognom1 = currentUserData.cognom1 ?: "",
                    cognom2 = currentUserData.cognom2,
                    rol = currentUserData.rol,
                    nif = currentUserData.nif ?: "",
                    carrer = currentUserData.carrer ?: "",
                    localitat = currentUserData.localitat ?: "",
                    provincia = currentUserData.provincia ?: "",
                    cp = currentUserData.cp ?: "",
                    tlf = currentUserData.tlf ?: "",
                    email = currentUserData.email ?: "",
                    password = newPassword  // IMPORTANT: Afegir la nova contrasenya
                )

                val response = api.updateUser(userId, updateRequest)

                // Actualitzar estat local si cal
                _userProfileState.value = _userProfileState.value.copy(user = response)

                onResult(true, "Contrasenya canviada correctament! Recorda usar la nova contrasenya en el proper login.")

            } catch (e: Exception) {
                val errorMessage = when {
                    e.message?.contains("401") == true -> "Contrasenya actual incorrecta"
                    e.message?.contains("403") == true -> "No tens permisos per canviar aquesta contrasenya"
                    e.message?.contains("400") == true -> "Format de contrasenya invàlid"
                    else -> "Error al canviar contrasenya: ${e.message}"
                }
                onResult(false, errorMessage)
            }
        }
    }

    /**
     * Canvia la contrasenya d'un altre usuari (només per administradors).
     * No requereix verificar la contrasenya actual.
     *
     * @param userId ID de l'usuari a modificar
     * @param newPassword Nova contrasenya
     * @param onResult Callback amb el resultat
     */
    fun resetUserPassword(
        userId: Long,
        newPassword: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // Obtenir dades actuals de l'usuari
                val currentUserData = api.getUserById(userId)

                val updateRequest = UpdateUserRequest(
                    id = userId,
                    nick = currentUserData.nick,
                    nom = currentUserData.nom,
                    cognom1 = currentUserData.cognom1 ?: "",
                    cognom2 = currentUserData.cognom2,
                    rol = currentUserData.rol,
                    nif = currentUserData.nif ?: "",
                    carrer = currentUserData.carrer ?: "",
                    localitat = currentUserData.localitat ?: "",
                    provincia = currentUserData.provincia ?: "",
                    cp = currentUserData.cp ?: "",
                    tlf = currentUserData.tlf ?: "",
                    email = currentUserData.email ?: "",
                    password = newPassword  // Nova contrasenya
                )

                api.updateUser(userId, updateRequest)

                onResult(true, "Contrasenya restablerta correctament per l'usuari ${currentUserData.nick}")

            } catch (e: Exception) {
                val errorMessage = when {
                    e.message?.contains("404") == true -> "Usuari no trobat"
                    e.message?.contains("403") == true -> "No tens permisos d'administrador"
                    else -> "Error al restablir contrasenya: ${e.message}"
                }
                onResult(false, errorMessage)
            }
        }
    }

    /**
     * Cerca un usuari pel seu ID.
     *
     * Procés:
     * 1. Valida que l'ID sigui vàlid (> 0)
     * 2. Fa la petició al servidor
     * 3. Actualitza userSearchState amb el resultat
     *
     * @param userId ID de l'usuari a cercar (com a String per facilitar input)
     *
     * @author Oscar
     * @since 1.0
     * @see AuthApiService.getUserById
     */
    fun searchUserById(userId: String) {
        viewModelScope.launch {
            // Validar que sigui un número vàlid
            val id = userId.toLongOrNull()
            if (id == null || id <= 0) {
                _userSearchState.value = UserSearchState(
                    isSearching = false,
                    error = "ID invàlid. Ha de ser un número positiu",
                    hasSearched = true
                )
                return@launch
            }

            _userSearchState.value = UserSearchState(isSearching = true)

            try {
                val user = api.getUserById(id)
                _userSearchState.value = UserSearchState(
                    isSearching = false,
                    searchResult = user,
                    hasSearched = true
                )
            } catch (e: Exception) {
                val errorMessage = when {
                    e.message?.contains("404") == true -> "No s'ha trobat cap usuari amb ID $id"
                    e.message?.contains("401") == true -> "Sessió expirada. Torna a iniciar sessió"
                    e.message?.contains("403") == true -> "No tens permisos per cercar usuaris"
                    else -> "Error de cerca: ${e.message}"
                }
                _userSearchState.value = UserSearchState(
                    isSearching = false,
                    error = errorMessage,
                    hasSearched = true
                )
            }
        }
    }

    /**
     * Cerca un usuari pel seu NIF/DNI.
     *
     * Procés:
     * 1. Valida que el NIF no estigui buit
     * 2. Fa la petició al servidor
     * 3. Actualitza userSearchState amb el resultat
     *
     * @param nif NIF/DNI de l'usuari a cercar
     *
     * @author Oscar
     * @since 1.0
     * @see AuthApiService.getUserByNif
     */
    fun searchUserByNif(nif: String) {
        viewModelScope.launch {
            if (nif.isBlank()) {
                _userSearchState.value = UserSearchState(
                    isSearching = false,
                    error = "El NIF no pot estar buit",
                    hasSearched = true
                )
                return@launch
            }

            _userSearchState.value = UserSearchState(isSearching = true)

            try {
                val response = api.getUserByNif(nif.trim())

                if (response.isSuccessful && response.body() != null) {
                    // Usuari trobat
                    _userSearchState.value = UserSearchState(
                        isSearching = false,
                        searchResult = response.body(),
                        hasSearched = true
                    )
                }else{
                    // Usuari no trobat (404 o resposta buida)
                    val errorMessage = when (response.code()) {
                        404 -> "No s'ha trobat cap usuari amb el NIF $nif"
                        401 -> "Sessió expirada. Torna a iniciar sessió"
                        403 -> "No tens permisos per cercar usuaris"
                        else -> "No s'ha trobat cap usuari amb el NIF $nif"
                    }
                    _userSearchState.value = UserSearchState(
                        isSearching = false,
                        error = errorMessage,
                        hasSearched = true
                    )
                }
            } catch (e: Exception) {
                val errorMessage = when {
                    e.message?.contains("401") == true -> "Sessió expirada. Torna a iniciar sessió"
                    e.message?.contains("403") == true -> "No tens permisos per cercar usuaris"
                    e.message?.contains("timeout") == true -> "Temps d'espera esgotat"
                    e.message?.contains("Unable to resolve host") == true -> "Sense connexió al servidor"
                    else -> "Error de connexió: ${e.message}"
                }
                _userSearchState.value = UserSearchState(
                    isSearching = false,
                    error = errorMessage,
                    hasSearched = true
                )
            }
        }
    }

    /**
     * Neteja l'estat de cerca, tornant-lo als valors inicials.
     *
     * @author Oscar
     * @since 1.0
     */
    fun clearSearch() {
        _userSearchState.value = UserSearchState()
    }

    /**
     * Neteja l'estat del perfil d'usuari (UserProfileState),
     * eliminant les dades de l'usuari actual o de l'usuari cercat.
     * * @author Oscar
     * @since 1.0
     */
    fun clearUserProfileState() {
        _userProfileState.value = UserProfileState(isLoading = true)
    }
}