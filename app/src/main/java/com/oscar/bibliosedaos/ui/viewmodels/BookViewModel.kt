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
 * ViewModel per gestionar el manteniment de llibres, autors i exemplars.
 *
 * Aquest ViewModel gestiona totes les operacions CRUD relacionades amb
 * el catàleg de llibres de la biblioteca, incloent:
 * - Gestió de llibres (afegir, editar, eliminar, llistar)
 * - Gestió d'autors (afegir, eliminar, llistar)
 * - Gestió d'exemplars (afegir, editar, eliminar, llistar)
 *
 * **Funcionalitats Principals:**
 * - Càrrega de dades des del backend
 * - Validació de formularis
 * - Gestió d'estats de càrrega i errors
 * - Actualització reactiva de la UI
 *
 * @property api Instància de l'API per comunicació amb el backend
 *
 * @author Oscar
 * @since 1.0
 * @see Llibre
 * @see Autor
 * @see Exemplar
 */
class BookViewModel : ViewModel() {

    private val api = ApiClient.instance

    // ========== ESTATS DELS LLIBRES ==========

    /**
     * Estat de la llista de llibres.
     */
    private val _llibresState = MutableStateFlow(LlibresUiState())
    val llibresState: StateFlow<LlibresUiState> = _llibresState.asStateFlow()

    /**
     * Estat del formulari d'afegir/editar llibre.
     */
    private val _llibreFormState = MutableStateFlow(LlibreFormState())
    val llibreFormState: StateFlow<LlibreFormState> = _llibreFormState.asStateFlow()

    // ========== ESTATS DELS AUTORS ==========

    /**
     * Estat de la llista d'autors.
     */
    private val _autorsState = MutableStateFlow(AutorsUiState())
    val autorsState: StateFlow<AutorsUiState> = _autorsState.asStateFlow()

    // ========== ESTATS DELS EXEMPLARS ==========

    /**
     * Estat de la llista d'exemplars.
     */
    private val _exemplarsState = MutableStateFlow(ExemplarsUiState())
    val exemplarsState: StateFlow<ExemplarsUiState> = _exemplarsState.asStateFlow()

    /**
     * Estat del formulari d'afegir/editar exemplar.
     */
    private val _exemplarFormState = MutableStateFlow(ExemplarFormState())
    val exemplarFormState: StateFlow<ExemplarFormState> = _exemplarFormState.asStateFlow()

    // ========== OPERACIONS AMB LLIBRES ==========

    /**
     * Carrega la llista de llibres des del backend.
     *
     * Actualitza [llibresState] amb els llibres obtinguts o
     * amb un missatge d'error si la petició falla.
     */
    fun loadLlibres() {
        viewModelScope.launch {
            _llibresState.value = _llibresState.value.copy(isLoading = true)
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
     * Afegeix un nou llibre al sistema.
     *
     * @param isbn ISBN únic del llibre
     * @param titol Títol del llibre
     * @param pagines Nombre de pàgines
     * @param editorial Editorial del llibre
     * @param autorId ID de l'autor (opcional)
     */
    fun addLlibre(
        isbn: String,
        titol: String,
        pagines: Int,
        editorial: String,
        autorId: Long?
    ) {
        viewModelScope.launch {
            _llibreFormState.value = _llibreFormState.value.copy(isSubmitting = true)

            try {
                val autor = autorId?.let {
                    _autorsState.value.autors.find { it.id == autorId }
                }

                val nouLlibre = Llibre(
                    isbn = isbn,
                    titol = titol,
                    pagines = pagines,
                    editorial = editorial,
                    autor = autor
                )

                val llibreCreat = api.addLlibre(nouLlibre)
                _llibreFormState.value = LlibreFormState(
                    success = true,
                    successMessage = "Llibre creat correctament"
                )

                // Recarregar la llista
                loadLlibres()

            } catch (e: Exception) {
                _llibreFormState.value = _llibreFormState.value.copy(
                    isSubmitting = false,
                    error = "Error creant llibre: ${e.message}"
                )
            }
        }
    }

    /**
     * Actualitza un llibre existent.
     *
     * @param id ID del llibre a actualitzar
     * @param llibre Objecte Llibre amb les noves dades
     */
    fun updateLlibre(id: Long, llibre: Llibre) {
        viewModelScope.launch {
            _llibreFormState.value = _llibreFormState.value.copy(isSubmitting = true)

            try {
                val llibreActualitzat = api.updateLlibre(id, llibre)
                _llibreFormState.value = LlibreFormState(
                    success = true,
                    successMessage = "Llibre actualitzat correctament"
                )

                // Recarregar la llista
                loadLlibres()

            } catch (e: Exception) {
                _llibreFormState.value = _llibreFormState.value.copy(
                    isSubmitting = false,
                    error = "Error actualitzant llibre: ${e.message}"
                )
            }
        }
    }

    /**
     * Elimina un llibre del sistema.
     *
     * @param id ID del llibre a eliminar
     */
    fun deleteLlibre(id: Long) {
        viewModelScope.launch {
            _llibresState.value = _llibresState.value.copy(isDeleting = id)

            try {
                api.deleteLlibre(id)
                // Recarregar la llista
                loadLlibres()

            } catch (e: Exception) {
                _llibresState.value = _llibresState.value.copy(
                    isDeleting = null,
                    error = "Error eliminant llibre: ${e.message}"
                )
            }
        }
    }

    // ========== OPERACIONS AMB AUTORS ==========

    /**
     * Carrega la llista d'autors des del backend.
     */
    fun loadAutors() {
        viewModelScope.launch {
            _autorsState.value = _autorsState.value.copy(isLoading = true)
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
     * Afegeix un nou autor al sistema.
     *
     * @param nom Nom complet de l'autor
     */
    fun addAutor(nom: String) {
        viewModelScope.launch {
            _autorsState.value = _autorsState.value.copy(isAdding = true)

            try {
                val nouAutor = Autor(nom = nom)
                val autorCreat = api.addAutor(nouAutor)

                // Actualitzar la llista localment
                val novaLlista = _autorsState.value.autors + autorCreat
                _autorsState.value = _autorsState.value.copy(
                    autors = novaLlista,
                    isAdding = false
                )

            } catch (e: Exception) {
                _autorsState.value = _autorsState.value.copy(
                    isAdding = false,
                    error = "Error creant autor: ${e.message}"
                )
            }
        }
    }

    /**
     * Elimina un autor del sistema.
     *
     * @param id ID de l'autor a eliminar
     */
    fun deleteAutor(id: Long) {
        viewModelScope.launch {
            _autorsState.value = _autorsState.value.copy(isDeleting = id)

            try {
                api.deleteAutor(id)
                // Actualitzar la llista localment
                val novaLlista = _autorsState.value.autors.filter { it.id != id }
                _autorsState.value = _autorsState.value.copy(
                    autors = novaLlista,
                    isDeleting = null
                )

            } catch (e: Exception) {
                _autorsState.value = _autorsState.value.copy(
                    isDeleting = null,
                    error = "Error eliminant autor: ${e.message}"
                )
            }
        }
    }

    // ========== OPERACIONS AMB EXEMPLARS ==========

    /**
     * Carrega la llista d'exemplars des del backend.
     */
    fun loadExemplars() {
        viewModelScope.launch {
            _exemplarsState.value = _exemplarsState.value.copy(isLoading = true)
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
     *
     * @param titol Títol del llibre (opcional)
     * @param autor Nom de l'autor (opcional)
     */
    fun searchExemplarsLliures(titol: String? = null, autor: String? = null) {
        viewModelScope.launch {
            _exemplarsState.value = _exemplarsState.value.copy(isSearching = true)
            try {
                val exemplars = api.getExemplarsLliures(titol, autor)
                _exemplarsState.value = _exemplarsState.value.copy(
                    searchResults = exemplars,
                    isSearching = false
                )
            } catch (e: Exception) {
                _exemplarsState.value = _exemplarsState.value.copy(
                    isSearching = false,
                    error = "Error cercant exemplars: ${e.message}"
                )
            }
        }
    }

    /**
     * Afegeix un nou exemplar d'un llibre.
     *
     * @param lloc Ubicació física de l'exemplar
     * @param llibreId ID del llibre associat
     */
    fun addExemplar(lloc: String, llibreId: Long) {
        viewModelScope.launch {
            _exemplarFormState.value = _exemplarFormState.value.copy(isSubmitting = true)

            try {
                val llibre = _llibresState.value.llibres.find { it.id == llibreId }

                if (llibre == null) {
                    _exemplarFormState.value = _exemplarFormState.value.copy(
                        isSubmitting = false,
                        error = "Llibre no trobat"
                    )
                    return@launch
                }

                val nouExemplar = Exemplar(
                    lloc = lloc,
                    llibre = llibre
                )

                val exemplarCreat = api.addExemplar(nouExemplar)
                _exemplarFormState.value = ExemplarFormState(
                    success = true,
                    successMessage = "Exemplar creat correctament"
                )

                // Recarregar la llista
                loadExemplars()

            } catch (e: Exception) {
                _exemplarFormState.value = _exemplarFormState.value.copy(
                    isSubmitting = false,
                    error = "Error creant exemplar: ${e.message}"
                )
            }
        }
    }

    /**
     * Elimina un exemplar del sistema.
     *
     * @param id ID de l'exemplar a eliminar
     */
    fun deleteExemplar(id: Long) {
        viewModelScope.launch {
            _exemplarsState.value = _exemplarsState.value.copy(isDeleting = id)

            try {
                api.deleteExemplar(id)
                // Recarregar la llista
                loadExemplars()

            } catch (e: Exception) {
                _exemplarsState.value = _exemplarsState.value.copy(
                    isDeleting = null,
                    error = "Error eliminant exemplar: ${e.message}"
                )
            }
        }
    }

    /**
     * Actualitza l'estat (disponibilitat) d'un exemplar.
     *
     * **Estats possibles:**
     * - "lliure": Disponible per prestar
     * - "prestat": Actualment en préstec
     * - "reservat": Reservat per un usuari
     *
     * **Ús:**
     * Permet a l'admin canviar manualment l'estat d'un exemplar
     * per corregir errors o gestionar situacions especials.
     *
     * @param exemplarId ID de l'exemplar a actualitzar
     * @param nouEstat Nou estat: "lliure", "prestat" o "reservat"
     */
    fun updateExemplarStatus(exemplarId: Long, nouEstat: String) {
        viewModelScope.launch {
            _exemplarsState.value = _exemplarsState.value.copy(isDeleting = exemplarId)

            try {
                // Obtenir l'exemplar actual
                val exemplar = _exemplarsState.value.exemplars.find { it.id == exemplarId }

                if (exemplar == null) {
                    _exemplarsState.value = _exemplarsState.value.copy(
                        isDeleting = null,
                        error = "Exemplar no trobat"
                    )
                    return@launch
                }

                // Crear exemplar actualitzat amb nou estat
                val exemplarActualitzat = exemplar.copy(reservat = nouEstat)

                // Enviar al backend
                api.updateExemplar(exemplarId, exemplarActualitzat)

                // Recarregar la llista
                loadExemplars()

            } catch (e: Exception) {
                _exemplarsState.value = _exemplarsState.value.copy(
                    isDeleting = null,
                    error = "Error actualitzant estat: ${e.message}"
                )
            }
        }
    }

    /**
     * Neteja els missatges d'error.
     */
    fun clearErrors() {
        _llibresState.value = _llibresState.value.copy(error = null)
        _autorsState.value = _autorsState.value.copy(error = null)
        _exemplarsState.value = _exemplarsState.value.copy(error = null)
        _llibreFormState.value = _llibreFormState.value.copy(error = null)
        _exemplarFormState.value = _exemplarFormState.value.copy(error = null)
    }

    /**
     * Reinicia els estats dels formularis.
     */
    fun resetForms() {
        _llibreFormState.value = LlibreFormState()
        _exemplarFormState.value = ExemplarFormState()
    }
}

// ========== UI STATES ==========

/**
 * Estat de la UI per la llista de llibres.
 */
data class LlibresUiState(
    val llibres: List<Llibre> = emptyList(),
    val isLoading: Boolean = false,
    val isDeleting: Long? = null,
    val error: String? = null
)

/**
 * Estat del formulari de llibre.
 */
data class LlibreFormState(
    val isSubmitting: Boolean = false,
    val success: Boolean = false,
    val successMessage: String? = null,
    val error: String? = null
)

/**
 * Estat de la UI per la llista d'autors.
 */
data class AutorsUiState(
    val autors: List<Autor> = emptyList(),
    val isLoading: Boolean = false,
    val isAdding: Boolean = false,
    val isDeleting: Long? = null,
    val error: String? = null
)

/**
 * Estat de la UI per la llista d'exemplars.
 */
data class ExemplarsUiState(
    val exemplars: List<Exemplar> = emptyList(),
    val searchResults: List<Exemplar>? = null,
    val isLoading: Boolean = false,
    val isSearching: Boolean = false,
    val isDeleting: Long? = null,
    val error: String? = null
)

/**
 * Estat del formulari d'exemplar.
 */
data class ExemplarFormState(
    val isSubmitting: Boolean = false,
    val success: Boolean = false,
    val successMessage: String? = null,
    val error: String? = null
)