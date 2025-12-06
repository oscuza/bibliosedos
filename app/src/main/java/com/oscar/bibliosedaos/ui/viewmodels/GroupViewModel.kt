package com.oscar.bibliosedaos.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oscar.bibliosedaos.data.models.*
import com.oscar.bibliosedaos.data.network.ApiClient
import com.oscar.bibliosedaos.data.network.AuthApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Response

/**
 * ViewModel per gestionar grups de lectura i horaris.
 *
 * Gestiona:
 * - Llistat i creació de grups de lectura
 * - Eliminació de grups
 * - Gestió de membres de grups (afegir/eliminar)
 * - Llistat d'horaris disponibles
 * - Filtrat de grups per usuari (client-side)
 *
 * **Nota:** Alguns endpoints no estan disponibles al backend:
 * - No hi ha endpoint per obtenir un grup per ID (es busca en la llista)
 * - No hi ha endpoint per actualitzar grups
 * - No hi ha endpoint per obtenir grups per usuari (es filtra client-side)
 *
 * @author Oscar
 * @since 1.0
 */
class GroupViewModel(
    private val api: AuthApiService = ApiClient.instance
) : ViewModel() {

    // ========== ESTATS DE GRUPS ==========

    private val _grupsState = MutableStateFlow(GrupsUiState())
    val grupsState: StateFlow<GrupsUiState> = _grupsState.asStateFlow()

    // ========== ESTATS D'HORARIS ==========

    private val _horarisState = MutableStateFlow(HorarisUiState())
    val horarisState: StateFlow<HorarisUiState> = _horarisState.asStateFlow()

    // ========== ESTAT DE GRUP SELECCIONAT ==========

    private val _selectedGrupState = MutableStateFlow<Grup?>(null)
    val selectedGrupState: StateFlow<Grup?> = _selectedGrupState.asStateFlow()

    // ========== OPERACIONS AMB GRUPS ==========

    /**
     * Carrega tots els grups de lectura del sistema.
     *
     * @see GrupsUiState
     * @see AuthApiService.getAllGrups
     */
    fun loadGrups() {
        viewModelScope.launch {
            _grupsState.value = _grupsState.value.copy(isLoading = true, error = null)
            try {
                val grups = api.getAllGrups()
                _grupsState.value = GrupsUiState(
                    grups = grups,
                    isLoading = false
                )
            } catch (e: Exception) {
                _grupsState.value = _grupsState.value.copy(
                    error = "Error carregant grups: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    /**
     * Carrega un grup específic per ID.
     * 
     * Nota: El backend no té un endpoint específic per obtenir un grup per ID.
     * Aquesta funció busca el grup en la llista carregada.
     *
     * @param grupId Identificador del grup
     */
    fun loadGrupById(grupId: Long) {
        viewModelScope.launch {
            _grupsState.value = _grupsState.value.copy(isLoading = true, error = null)
            try {
                // Com el backend no té endpoint per obtenir un grup per ID,
                // carreguem tots els grups i busquem el desitjat
                val grups = api.getAllGrups()
                val grup = grups.find { it.id == grupId }
                if (grup != null) {
                    _selectedGrupState.value = grup
                } else {
                    _grupsState.value = _grupsState.value.copy(
                        error = "Grup amb ID $grupId no trobat",
                        isLoading = false
                    )
                    return@launch
                }
                _grupsState.value = _grupsState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _grupsState.value = _grupsState.value.copy(
                    error = "Error carregant grup: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    /**
     * Carrega els grups on un usuari participa.
     * 
     * Nota: El backend no té un endpoint específic per obtenir grups per usuari.
     * Aquesta funció filtra els grups carregats on l'usuari és membre o administrador.
     *
     * @param usuariId Identificador de l'usuari
     */
    fun loadGrupsByUsuari(usuariId: Long) {
        viewModelScope.launch {
            _grupsState.value = _grupsState.value.copy(isLoading = true, error = null)
            try {
                // Com el backend no té endpoint per obtenir grups per usuari,
                // carreguem tots els grups i filtrem
                val allGrups = api.getAllGrups()
                val grups = allGrups.filter { grup ->
                    grup.administrador?.id == usuariId || 
                    grup.membres?.any { it.id == usuariId } == true
                }
                _grupsState.value = GrupsUiState(
                    grups = grups,
                    isLoading = false
                )
            } catch (e: Exception) {
                _grupsState.value = _grupsState.value.copy(
                    error = "Error carregant grups de l'usuari: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    /**
     * Crea un nou grup de lectura.
     * 
     * Nota: El backend requereix un objecte Grup complet amb administrador i horari com a objectes,
     * no només IDs. Aquesta funció necessita els objectes User i Horari complets.
     *
     * @param nom Nom del grup
     * @param tematica Temàtica del grup
     * @param administrador Objecte User complet de l'administrador
     * @param horari Objecte Horari complet assignat
     * @param membres Llista d'objectes User que són membres (opcional, pot ser buida)
     */
    fun createGrup(
        nom: String,
        tematica: String,
        administrador: com.oscar.bibliosedaos.data.network.User,
        horari: Horari,
        membres: List<com.oscar.bibliosedaos.data.network.User> = emptyList()
    ) {
        viewModelScope.launch {
            _grupsState.value = _grupsState.value.copy(isCreating = true, error = null)
            try {
                // El backend requereix un objecte Grup complet
                val newGrup = Grup(
                    id = null, // El backend assignarà l'ID
                    nom = nom,
                    tematica = tematica,
                    administrador = administrador,
                    horari = horari,
                    membres = if (membres.isNotEmpty()) membres else null
                )
                val createdGrup = api.createGrup(newGrup)
                // Evitar duplicats: només afegir si no existeix ja
                val existingIds = _grupsState.value.grups.mapNotNull { it.id }.toSet()
                val updatedGrups = if (createdGrup.id != null && createdGrup.id in existingIds) {
                    // Si ja existeix, actualitzar la llista
                    _grupsState.value.grups.map { if (it.id == createdGrup.id) createdGrup else it }
                } else {
                    // Si no existeix, afegir-lo
                    _grupsState.value.grups + createdGrup
                }
                _grupsState.value = GrupsUiState(
                    grups = updatedGrups,
                    isCreating = false
                )
            } catch (e: Exception) {
                _grupsState.value = _grupsState.value.copy(
                    error = "Error creant grup: ${e.message}",
                    isCreating = false
                )
            }
        }
    }

    /**
     * Actualitza un grup existent.
     * 
     * Nota: El backend no té un endpoint per actualitzar grups.
     * Aquesta funcionalitat no està disponible actualment.
     *
     * @param grupId ID del grup a actualitzar
     * @param nom Nou nom del grup
     * @param tematica Nova temàtica
     * @param horariId Nou horari (opcional)
     */
    fun updateGrup(
        grupId: Long,
        nom: String,
        tematica: String,
        horariId: Long? = null
    ) {
        viewModelScope.launch {
            _grupsState.value = _grupsState.value.copy(
                isUpdating = grupId,
                error = null
            )
            try {
                // El backend no té endpoint per actualitzar grups
                _grupsState.value = _grupsState.value.copy(
                    error = "L'actualització de grups no està disponible al backend",
                    isUpdating = null
                )
            } catch (e: Exception) {
                _grupsState.value = _grupsState.value.copy(
                    error = "Error actualitzant grup: ${e.message}",
                    isUpdating = null
                )
            }
        }
    }

    /**
     * Elimina un grup del sistema.
     *
     * @param grupId ID del grup a eliminar
     */
    fun deleteGrup(grupId: Long) {
        viewModelScope.launch {
            _grupsState.value = _grupsState.value.copy(
                isDeleting = grupId,
                error = null
            )
            try {
                val response = api.deleteGrup(grupId)
                if (response.isSuccessful) {
                    // El backend retorna text/plain, no cal parsejar JSON
                    val updatedGrups = _grupsState.value.grups.filter { it.id != grupId }
                    _grupsState.value = GrupsUiState(
                        grups = updatedGrups,
                        isDeleting = null
                    )
                    if (_selectedGrupState.value?.id == grupId) {
                        _selectedGrupState.value = null
                    }
                } else {
                    _grupsState.value = _grupsState.value.copy(
                        error = "Error eliminant grup: ${response.code()} ${response.message()}",
                        isDeleting = null
                    )
                }
            } catch (e: Exception) {
                _grupsState.value = _grupsState.value.copy(
                    error = "Error eliminant grup: ${e.message}",
                    isDeleting = null
                )
            }
        }
    }

    /**
     * Afegeix un membre a un grup.
     * 
     * Nota: El backend requereix que el membreId coincideixi amb l'usuari autenticat.
     * L'endpoint utilitza path params, no body.
     *
     * @param grupId ID del grup
     * @param membreId ID de l'usuari a afegir (ha de ser l'usuari autenticat)
     */
    fun addMemberToGrup(grupId: Long, membreId: Long) {
        viewModelScope.launch {
            _grupsState.value = _grupsState.value.copy(
                isUpdating = grupId,
                error = null
            )
            try {
                // El backend utilitza PUT amb path params
                val updatedGrup = api.addMemberToGrup(grupId, membreId)
                val updatedGrups = _grupsState.value.grups.map {
                    if (it.id == grupId) updatedGrup else it
                }
                _grupsState.value = GrupsUiState(
                    grups = updatedGrups,
                    isUpdating = null
                )
                if (_selectedGrupState.value?.id == grupId) {
                    _selectedGrupState.value = updatedGrup
                }
            } catch (e: Exception) {
                _grupsState.value = _grupsState.value.copy(
                    error = "Error afegint membre: ${e.message}",
                    isUpdating = null
                )
            }
        }
    }

    /**
     * Carrega la llista de membres d'un grup.
     *
     * @param grupId ID del grup
     * @return Llista d'objectes User que són membres del grup
     */
    suspend fun loadMembresGrup(grupId: Long): List<com.oscar.bibliosedaos.data.network.User> {
        return try {
            api.getMembresGrup(grupId)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Elimina un membre d'un grup.
     * 
     * Nota: El backend utilitza la ruta /sortirUsuari, no /eliminarMembre.
     *
     * @param grupId ID del grup
     * @param membreId ID de l'usuari a eliminar
     */
    fun removeMemberFromGrup(grupId: Long, membreId: Long) {
        viewModelScope.launch {
            _grupsState.value = _grupsState.value.copy(
                isUpdating = grupId,
                error = null
            )
            try {
                val response = api.removeMemberFromGrup(grupId, membreId)
                if (response.isSuccessful) {
                    // El backend retorna text/plain, no cal parsejar JSON
                    // Recarregar el grup per obtenir l'estat actualitzat
                    loadGrupById(grupId)
                    loadGrups() // Recarregar la llista completa
                } else {
                    _grupsState.value = _grupsState.value.copy(
                        error = "Error eliminant membre: ${response.code()} ${response.message()}",
                        isUpdating = null
                    )
                }
            } catch (e: Exception) {
                _grupsState.value = _grupsState.value.copy(
                    error = "Error eliminant membre: ${e.message}",
                    isUpdating = null
                )
            }
        }
    }

    /**
     * Selecciona un grup per mostrar els seus detalls.
     *
     * @param grup Grup a seleccionar
     */
    fun selectGrup(grup: Grup?) {
        _selectedGrupState.value = grup
    }

    // ========== OPERACIONS AMB HORARIS ==========

    /**
     * Carrega tots els horaris disponibles.
     *
     * @see HorarisUiState
     * @see AuthApiService.getAllHoraris
     */
    fun loadHoraris() {
        viewModelScope.launch {
            _horarisState.value = _horarisState.value.copy(isLoading = true, error = null)
            try {
                val horaris = api.getAllHoraris()
                _horarisState.value = HorarisUiState(
                    horaris = horaris,
                    isLoading = false
                )
            } catch (e: Exception) {
                _horarisState.value = _horarisState.value.copy(
                    error = "Error carregant horaris: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    /**
     * Carrega tots els horaris i filtra els lliures en el client.
     * 
     * Nota: El backend només proporciona l'endpoint per obtenir tots els horaris.
     * El filtrat es fa en el client.
     *
     * @see HorarisUiState
     * @see AuthApiService.getAllHoraris
     */
    fun loadHorarisLliures() {
        viewModelScope.launch {
            _horarisState.value = _horarisState.value.copy(isLoading = true, error = null)
            try {
                val allHoraris = api.getAllHoraris()
                val horarisLliures = allHoraris.filter { it.isLliure }
                _horarisState.value = HorarisUiState(
                    horaris = horarisLliures,
                    isLoading = false
                )
            } catch (e: Exception) {
                _horarisState.value = _horarisState.value.copy(
                    error = "Error carregant horaris lliures: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    /**
     * Obté els horaris lliures (disponibles) del estat actual.
     *
     * @return Llista d'horaris amb estat "lliure" o null
     */
    fun getHorarisLliures(): List<Horari> {
        return _horarisState.value.horaris.filter { it.isLliure }
    }

    /**
     * Crea un nou horari de sala.
     * 
     * **Permisos:** Només ADMIN
     * 
     * **Nota:** El backend valida que la combinació sala-dia-hora sigui única.
     * Si ja existeix un horari amb la mateixa combinació, retornarà error.
     *
     * @param sala Nom de la sala
     * @param dia Dia de la setmana (ex: "Dilluns", "Dimarts")
     * @param hora Hora de l'activitat (ex: "10:00", "18:30")
     * @param estat Estat inicial (opcional, per defecte "lliure")
     */
    fun createHorari(
        sala: String,
        dia: String,
        hora: String,
        estat: String = "lliure"
    ) {
        viewModelScope.launch {
            _horarisState.value = _horarisState.value.copy(
                isCreating = true,
                error = null
            )
            try {
                val newHorari = Horari(
                    sala = sala.trim(),
                    dia = dia.trim(),
                    hora = hora.trim(),
                    estat = estat
                )
                val createdHorari = api.createHorari(newHorari)
                // Evitar duplicats: només afegir si no existeix ja
                val existingIds = _horarisState.value.horaris.mapNotNull { it.id }.toSet()
                val updatedHoraris = if (createdHorari.id != null && createdHorari.id in existingIds) {
                    // Si ja existeix, actualitzar la llista
                    _horarisState.value.horaris.map { if (it.id == createdHorari.id) createdHorari else it }
                } else {
                    // Si no existeix, afegir-lo
                    _horarisState.value.horaris + createdHorari
                }
                _horarisState.value = HorarisUiState(
                    horaris = updatedHoraris,
                    isCreating = false
                )
            } catch (e: Exception) {
                _horarisState.value = _horarisState.value.copy(
                    error = "Error creant horari: ${e.message}",
                    isCreating = false
                )
            }
        }
    }

    /**
     * Elimina un horari del sistema.
     * 
     * **Permisos:** Només ADMIN
     * 
     * **NOTA:** Aquest endpoint NO existeix al backend actualment.
     * Quan s'afegeixi al backend, descomentar la crida a api.deleteHorari().
     *
     * @param horariId ID de l'horari a eliminar
     */
    fun deleteHorari(horariId: Long) {
        viewModelScope.launch {
            _horarisState.value = _horarisState.value.copy(
                isDeleting = horariId,
                error = null
            )
            try {
                // TODO: Descomentar quan el backend afegeixi l'endpoint DELETE
                // api.deleteHorari(horariId)
                // val updatedHoraris = _horarisState.value.horaris.filter { it.id != horariId }
                // _horarisState.value = HorarisUiState(
                //     horaris = updatedHoraris,
                //     isDeleting = null
                // )
                
                // Per ara, simulem l'eliminació localment (només per testing)
                // Quan el backend estigui llest, eliminar aquesta línia i descomentar les de dalt
                val updatedHoraris = _horarisState.value.horaris.filter { it.id != horariId }
                _horarisState.value = HorarisUiState(
                    horaris = updatedHoraris,
                    isDeleting = null
                )
            } catch (e: Exception) {
                _horarisState.value = _horarisState.value.copy(
                    error = "Error eliminant horari: ${e.message}",
                    isDeleting = null
                )
            }
        }
    }

    /**
     * Neteja l'error actual.
     */
    fun clearError() {
        _grupsState.value = _grupsState.value.copy(error = null)
        _horarisState.value = _horarisState.value.copy(error = null)
    }
}

// ========== UI STATES ==========

/**
 * Estat de la UI per grups de lectura.
 */
data class GrupsUiState(
    val grups: List<Grup> = emptyList(),
    val isLoading: Boolean = false,
    val isCreating: Boolean = false,
    val isUpdating: Long? = null,
    val isDeleting: Long? = null,
    val error: String? = null,
)

/**
 * Estat de la UI per horaris.
 */
data class HorarisUiState(
    val horaris: List<Horari> = emptyList(),
    val isLoading: Boolean = false,
    val isCreating: Boolean = false,
    val isDeleting: Long? = null,
    val error: String? = null,
)

