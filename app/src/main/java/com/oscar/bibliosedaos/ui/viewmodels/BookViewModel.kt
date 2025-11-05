package com.oscar.bibliosedaos.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oscar.bibliosedaos.data.models.*
import com.oscar.bibliosedaos.data.network.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel per gestionar llibres, autors i exemplars.
 *
 * Gestiona:
 * - CRUD de llibres
 * - CRUD d'autors
 * - CRUD d'exemplars
 * - Actualització d'estats d'exemplars per préstecs
 *
 * @author Oscar
 * @version 2.0 - Afegit updateExemplar per gestió de préstecs
 */
class BookViewModel : ViewModel() {

    val api = ApiClient.instance

    // ========== ESTATS DE LLIBRES ==========

    private val _llibresState = MutableStateFlow(LlibresUiState())
    val llibresState: StateFlow<LlibresUiState> = _llibresState.asStateFlow()

    // ========== ESTATS D'AUTORS ==========

    private val _autorsState = MutableStateFlow(AutorsUiState())
    val autorsState: StateFlow<AutorsUiState> = _autorsState.asStateFlow()

    // ========== ESTATS D'EXEMPLARS ==========

    val _exemplarsState = MutableStateFlow(ExemplarsUiState())
    val exemplarsState: StateFlow<ExemplarsUiState> = _exemplarsState.asStateFlow()

    // ========== OPERACIONS AMB LLIBRES ==========

    /**
     * Carrega tots els llibres del sistema.
     */
    fun loadLlibres() {
        viewModelScope.launch {
            _llibresState.value = LlibresUiState(isLoading = true)
            try {
                val llibres = api.getAllLlibres()
                _llibresState.value = LlibresUiState(
                    llibres = llibres,
                    isLoading = false
                )
            } catch (e: Exception) {
                _llibresState.value = LlibresUiState(
                    error = "Error carregant llibres: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    /**
     * Crea un nou llibre.
     */
    fun createLlibre(llibre: Llibre) {
        viewModelScope.launch {
            _llibresState.value = _llibresState.value.copy(isCreating = true)
            try {
                val newLlibre = api.addLlibre(llibre)
                val updatedList = _llibresState.value.llibres + newLlibre
                _llibresState.value = _llibresState.value.copy(
                    llibres = updatedList,
                    isCreating = false
                )
            } catch (e: Exception) {
                _llibresState.value = _llibresState.value.copy(
                    error = "Error creant llibre: ${e.message}",
                    isCreating = false
                )
            }
        }
    }

    /**
     * Actualitza un llibre existent.
     */
    fun updateLlibre(id: Long, llibre: Llibre) {
        viewModelScope.launch {
            _llibresState.value = _llibresState.value.copy(isUpdating = id)
            try {
                val updatedLlibre = api.updateLlibre(id, llibre)
                val updatedList = _llibresState.value.llibres.map {
                    if (it.id == id) updatedLlibre else it
                }
                _llibresState.value = _llibresState.value.copy(
                    llibres = updatedList,
                    isUpdating = null
                )
            } catch (e: Exception) {
                _llibresState.value = _llibresState.value.copy(
                    error = "Error actualitzant llibre: ${e.message}",
                    isUpdating = null
                )
            }
        }
    }

    /**
     * Elimina un llibre.
     */
    fun deleteLlibre(id: Long) {
        viewModelScope.launch {
            _llibresState.value = _llibresState.value.copy(isDeleting = id)
            try {
                api.deleteLlibre(id)
                val updatedList = _llibresState.value.llibres.filter { it.id != id }
                _llibresState.value = _llibresState.value.copy(
                    llibres = updatedList,
                    isDeleting = null
                )
            } catch (e: Exception) {
                _llibresState.value = _llibresState.value.copy(
                    error = "Error eliminant llibre: ${e.message}",
                    isDeleting = null
                )
            }
        }
    }

    // ========== OPERACIONS AMB AUTORS ==========

    /**
     * Carrega tots els autors del sistema.
     */
    fun loadAutors() {
        viewModelScope.launch {
            _autorsState.value = AutorsUiState(isLoading = true)
            try {
                val autors = api.getAllAutors()
                _autorsState.value = AutorsUiState(
                    autors = autors,
                    isLoading = false
                )
            } catch (e: Exception) {
                _autorsState.value = AutorsUiState(
                    error = "Error carregant autors: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    /**
     * Crea un nou autor.
     */
    fun createAutor(nom: String) {
        viewModelScope.launch {
            _autorsState.value = _autorsState.value.copy(isCreating = true)
            try {
                val autor = Autor(nom = nom)
                val newAutor = api.addAutor(autor)
                val updatedList = _autorsState.value.autors + newAutor
                _autorsState.value = _autorsState.value.copy(
                    autors = updatedList,
                    isCreating = false
                )
            } catch (e: Exception) {
                _autorsState.value = _autorsState.value.copy(
                    error = "Error creant autor: ${e.message}",
                    isCreating = false
                )
            }
        }
    }

    /**
     * Elimina un autor.
     */
    fun deleteAutor(id: Long) {
        viewModelScope.launch {
            _autorsState.value = _autorsState.value.copy(isDeleting = id)
            try {
                api.deleteAutor(id)
                val updatedList = _autorsState.value.autors.filter { it.id != id }
                _autorsState.value = _autorsState.value.copy(
                    autors = updatedList,
                    isDeleting = null
                )
            } catch (e: Exception) {
                _autorsState.value = _autorsState.value.copy(
                    error = "Error eliminant autor: ${e.message}",
                    isDeleting = null
                )
            }
        }
    }

    // ========== OPERACIONS AMB EXEMPLARS ==========

    /**
     * Carrega tots els exemplars del sistema.
     */
    fun loadExemplars() {
        viewModelScope.launch {
            _exemplarsState.value = ExemplarsUiState(isLoading = true)
            try {
                val exemplars = api.getAllExemplars()
                _exemplarsState.value = ExemplarsUiState(
                    exemplars = exemplars,
                    isLoading = false
                )
            } catch (e: Exception) {
                _exemplarsState.value = ExemplarsUiState(
                    error = "Error carregant exemplars: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    /**
     * Cerca exemplars lliures per títol o autor.
     */
    fun searchExemplarsLliures(titol: String?, autor: String?) {
        viewModelScope.launch {
            _exemplarsState.value = _exemplarsState.value.copy(isSearching = true)
            try {
                val results = api.getExemplarsLliures(titol, autor)
                _exemplarsState.value = _exemplarsState.value.copy(
                    searchResults = results,
                    isSearching = false
                )
            } catch (e: Exception) {
                _exemplarsState.value = _exemplarsState.value.copy(
                    error = "Error cercant exemplars: ${e.message}",
                    isSearching = false
                )
            }
        }
    }

    /**
     * Crea un nou exemplar.
     */
    fun createExemplar(exemplar: Exemplar) {
        viewModelScope.launch {
            _exemplarsState.value = _exemplarsState.value.copy(isCreating = true)
            try {
                val newExemplar = api.addExemplar(exemplar)
                val updatedList = _exemplarsState.value.exemplars + newExemplar
                _exemplarsState.value = _exemplarsState.value.copy(
                    exemplars = updatedList,
                    isCreating = false
                )
            } catch (e: Exception) {
                _exemplarsState.value = _exemplarsState.value.copy(
                    error = "Error creant exemplar: ${e.message}",
                    isCreating = false
                )
            }
        }
    }

    /**
     * NOU: Actualitza un exemplar (principalment per canviar l'estat).
     * Aquest mètode és clau per la gestió de préstecs.
     *
     * @param id Identificador de l'exemplar
     * @param exemplar Exemplar amb les dades actualitzades
     */
    fun updateExemplar(id: Long, exemplar: Exemplar) {
        viewModelScope.launch {
            _exemplarsState.value = _exemplarsState.value.copy(isUpdating = id)
            try {
                // Cridar l'API per actualitzar l'exemplar
                val updatedExemplar = api.updateExemplar(id, exemplar)

                // Actualitzar la llista local d'exemplars
                val updatedList = _exemplarsState.value.exemplars.map {
                    if (it.id == id) updatedExemplar else it
                }

                _exemplarsState.value = ExemplarsUiState(
                    exemplars = updatedList,
                    isLoading = false,
                    isUpdating = null
                )

                // Opcionalment, recarregar tots els exemplars per assegurar sincronització
                // Si vols més seguretat, descomenta la següent línia:
                // loadExemplars()

            } catch (e: Exception) {
                val errorMessage = when {
                    e.message?.contains("400") == true ->
                        "Dades invàlides per actualitzar l'exemplar"

                    e.message?.contains("404") == true ->
                        "Exemplar no trobat"

                    e.message?.contains("403") == true ->
                        "No tens permisos per actualitzar exemplars"

                    else ->
                        "Error actualitzant exemplar: ${e.message}"
                }

                _exemplarsState.value = _exemplarsState.value.copy(
                    error = errorMessage,
                    isUpdating = null
                )
            }
        }
    }

    /**
     * LEGACY: Actualitza l'estat d'un exemplar (mètode antic, mantingut per compatibilitat).
     * Es recomana utilitzar updateExemplar() directament.
     */
    fun updateExemplarStatus(id: Long, newStatus: String) {
        viewModelScope.launch {
            // Trobar l'exemplar actual
            val currentExemplar = _exemplarsState.value.exemplars.find { it.id == id }
            if (currentExemplar != null) {
                // Actualitzar només l'estat
                val updatedExemplar = currentExemplar.copy(reservat = newStatus)
                updateExemplar(id, updatedExemplar)
            } else {
                _exemplarsState.value = _exemplarsState.value.copy(
                    error = "Exemplar no trobat localment"
                )
            }
        }
    }

    /**
     * Elimina un exemplar.
     */
    fun deleteExemplar(id: Long) {
        viewModelScope.launch {
            _exemplarsState.value = _exemplarsState.value.copy(isDeleting = id)
            try {
                api.deleteExemplar(id)
                val updatedList = _exemplarsState.value.exemplars.filter { it.id != id }
                _exemplarsState.value = _exemplarsState.value.copy(
                    exemplars = updatedList,
                    isDeleting = null
                )
            } catch (e: Exception) {
                val errorMessage = when {
                    e.message?.contains("400") == true ->
                        "No es pot eliminar un exemplar prestat"

                    e.message?.contains("404") == true ->
                        "Exemplar no trobat"

                    else ->
                        "Error eliminant exemplar: ${e.message}"
                }

                _exemplarsState.value = _exemplarsState.value.copy(
                    error = errorMessage,
                    isDeleting = null
                )
            }
        }
    }

    /**
     * Neteja els resultats de cerca d'exemplars.
     */
    fun clearSearchResults() {
        _exemplarsState.value = _exemplarsState.value.copy(searchResults = null)
    }

    // ========== FUNCIONS D'UTILITAT ==========

    /**
     * Neteja tots els errors.
     */
    fun clearErrors() {
        _llibresState.value = _llibresState.value.copy(error = null)
        _autorsState.value = _autorsState.value.copy(error = null)
        _exemplarsState.value = _exemplarsState.value.copy(error = null)
    }

    /**
     * Recarrega totes les dades.
     */
    fun refreshAll() {
        loadLlibres()
        loadAutors()
        loadExemplars()
    }

    /**
     * Obté un llibre per ID.
     */
    fun getLlibreById(id: Long): Llibre? {
        return _llibresState.value.llibres.find { it.id == id }
    }

    /**
     * Obté un autor per ID.
     */
    fun getAutorById(id: Long): Autor? {
        return _autorsState.value.autors.find { it.id == id }
    }

    /**
     * Obté un exemplar per ID.
     */
    fun getExemplarById(id: Long): Exemplar? {
        return _exemplarsState.value.exemplars.find { it.id == id }
    }

    /**
     * Comprova si un llibre té exemplars.
     */
    fun llibreHasExemplars(llibreId: Long): Boolean {
        return _exemplarsState.value.exemplars.any { it.llibre?.id == llibreId }
    }

    /**
     * Obté el nombre d'exemplars lliures d'un llibre.
     */
    fun getExemplarsLliuresCount(llibreId: Long): Int {
        return _exemplarsState.value.exemplars.count {
            it.llibre?.id == llibreId && it.reservat == "lliure"
        }
    }

    /**
     * Obté tots els exemplars d'un llibre.
     */
    fun getExemplarsByLlibre(llibreId: Long): List<Exemplar> {
        return _exemplarsState.value.exemplars.filter { it.llibre?.id == llibreId }
    }
}

// ========== UI STATES ==========

/**
 * Estat de la UI per llibres.
 */
data class LlibresUiState(
    val llibres: List<Llibre> = emptyList(),
    val isLoading: Boolean = false,
    val isCreating: Boolean = false,
    val isUpdating: Long? = null,
    val isDeleting: Long? = null,
    val error: String? = null,
)

/**
 * Estat de la UI per autors.
 */
data class AutorsUiState(
    val autors: List<Autor> = emptyList(),
    val isLoading: Boolean = false,
    val isCreating: Boolean = false,
    val isDeleting: Long? = null,
    val error: String? = null,
)

/**
 * Estat de la UI per exemplars.
 */
data class ExemplarsUiState(
    val exemplars: List<Exemplar> = emptyList(),
    val searchResults: List<Exemplar>? = null,
    val isLoading: Boolean = false,
    val isSearching: Boolean = false,
    val isCreating: Boolean = false,
    val isUpdating: Long? = null,
    val isDeleting: Long? = null,
    val error: String? = null,
)