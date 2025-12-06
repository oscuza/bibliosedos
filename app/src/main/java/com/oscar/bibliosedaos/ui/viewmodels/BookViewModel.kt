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
class BookViewModel(
    private val api: AuthApiService = ApiClient.instance
) : ViewModel() {

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
     * Carrega tots els llibres del sistema des del backend.
     * 
     * Aquest mètode realitza una petició HTTP GET al servidor per obtenir
     * la llista completa de llibres registrats a la biblioteca.
     * 
     * **Estat de Càrrega:**
     * - Abans de la petició: `isLoading = true`
     * - Després de l'èxit: `isLoading = false`, `llibres = [llista de llibres]`
     * - Després de l'error: `isLoading = false`, `error = [missatge d'error]`
     * 
     * **Errors Possibles:**
     * - Error de xarxa: Problemes de connexió amb el servidor
     * - Error 401/403: Token JWT invàlid o expirat
     * - Error 500: Error intern del servidor
     * 
     * @author Oscar
     * @since 1.0
     * @see LlibresUiState
     * @see AuthApiService.getAllLlibres
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
     * Crea un nou llibre al sistema.
     * 
     * Aquest mètode realitza una petició HTTP PUT al servidor per afegir
     * un nou llibre a la base de dades de la biblioteca.
     * 
     * **Validacions del Backend:**
     * - ISBN ha de ser únic
     * - Tots els camps obligatoris han d'estar omplerts
     * - Només administradors poden crear llibres
     * 
     * **Estat de Creació:**
     * - Abans de la petició: `isCreating = true`
     * - Després de l'èxit: `isCreating = false`, llibre afegit a la llista
     * - Després de l'error: `isCreating = false`, `error = [missatge d'error]`
     * 
     * **Actualització de l'Estat:**
     * El nou llibre s'afegeix automàticament a la llista local després
     * de la creació exitosa per mantenir la sincronització amb el servidor.
     * 
     * @param llibre Objecte [Llibre] amb les dades del nou llibre a crear
     * 
     * @author Oscar
     * @since 1.0
     * @see Llibre
     * @see AuthApiService.addLlibre
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
     * Actualitza les dades d'un llibre existent al sistema.
     * 
     * Aquest mètode realitza una petició HTTP PUT al servidor per modificar
     * les dades d'un llibre que ja existeix a la base de dades.
     * 
     * **Validacions:**
     * - El llibre amb l'ID proporcionat ha d'existir
     * - Només administradors poden actualitzar llibres
     * - L'ISBN pot ser modificat sempre que sigui únic
     * 
     * **Estat d'Actualització:**
     * - Abans de la petició: `isUpdating = id`
     * - Després de l'èxit: `isUpdating = null`, llibre actualitzat a la llista
     * - Després de l'error: `isUpdating = null`, `error = [missatge d'error]`
     * 
     * **Actualització de l'Estat:**
     * El llibre a la llista local s'actualitza automàticament amb les
     * dades retornades pel servidor després de l'actualització exitosa.
     * 
     * @param id Identificador únic del llibre a actualitzar
     * @param llibre Objecte [Llibre] amb les dades actualitzades
     * 
     * @author Oscar
     * @since 1.0
     * @see Llibre
     * @see AuthApiService.updateLlibre
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
     * Elimina un llibre del sistema de forma permanent.
     * 
     * Aquest mètode realitza una petició HTTP DELETE al servidor per
     * eliminar un llibre de la base de dades de la biblioteca.
     * 
     * **Advertències:**
     * - L'operació és irreversible
     * - Només administradors poden eliminar llibres
     * - El llibre s'elimina de la llista local després de l'èxit
     * 
     * **Estat d'Eliminació:**
     * - Abans de la petició: `isDeleting = id`
     * - Després de l'èxit: `isDeleting = null`, llibre eliminat de la llista
     * - Després de l'error: `isDeleting = null`, `error = [missatge d'error]`
     * 
     * **Errors Possibles:**
     * - Error 404: El llibre no existeix
     * - Error 403: No tens permisos d'administrador
     * - Error 409: El llibre té exemplars associats que impedeixen l'eliminació
     * 
     * @param id Identificador únic del llibre a eliminar
     * 
     * @author Oscar
     * @since 1.0
     * @see AuthApiService.deleteLlibre
     */
    fun deleteLlibre(id: Long) {
        viewModelScope.launch {
            _llibresState.value = _llibresState.value.copy(isDeleting = id)
            try {
                val response = api.deleteLlibre(id)
                if (response.isSuccessful) {
                    val updatedList = _llibresState.value.llibres.filter { it.id != id }
                    _llibresState.value = _llibresState.value.copy(
                        llibres = updatedList,
                        isDeleting = null
                    )
                } else {
                    // Si la resposta no és exitosa, llançar HttpException per gestionar l'error
                    throw retrofit2.HttpException(response)
                }
            } catch (e: Exception) {
                val errorMessage = if (e is retrofit2.HttpException) {
                    try {
                        val errorBody = e.response()?.errorBody()?.string()
                        errorBody ?: "Error eliminant llibre: ${e.message}"
                    } catch (ex: Exception) {
                        "Error eliminant llibre: ${e.message}"
                    }
                } else {
                    "Error eliminant llibre: ${e.message}"
                }
                _llibresState.value = _llibresState.value.copy(
                    error = errorMessage,
                    isDeleting = null
                )
            }
        }
    }

    // ========== OPERACIONS AMB AUTORS ==========

    /**
     * Carrega tots els autors del sistema des del backend.
     * 
     * Aquest mètode realitza una petició HTTP GET al servidor per obtenir
     * la llista completa d'autors registrats a la biblioteca, ordenats
     * alfabèticament per nom.
     * 
     * **Estat de Càrrega:**
     * - Abans de la petició: `isLoading = true`
     * - Després de l'èxit: `isLoading = false`, `autors = [llista d'autors]`
     * - Després de l'error: `isLoading = false`, `error = [missatge d'error]`
     * 
     * **Ordre de Resultats:**
     * Els autors es retornen ordenats alfabèticament pel seu nom de forma
     * ascendent (A-Z).
     * 
     * @author Oscar
     * @since 1.0
     * @see AutorsUiState
     * @see AuthApiService.getAllAutors
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
     * Crea un nou autor al sistema.
     * 
     * Aquest mètode realitza una petició HTTP PUT al servidor per afegir
     * un nou autor a la base de dades de la biblioteca.
     * 
     * **Validacions del Backend:**
     * - El nom de l'autor ha de ser únic
     * - El nom no pot estar buit
     * - Només administradors poden crear autors
     * 
     * **Estat de Creació:**
     * - Abans de la petició: `isCreating = true`
     * - Després de l'èxit: `isCreating = false`, autor afegit a la llista
     * - Després de l'error: `isCreating = false`, `error = [missatge d'error]`
     * 
     * **Actualització de l'Estat:**
     * El nou autor s'afegeix automàticament a la llista local després
     * de la creació exitosa.
     * 
     * @param nom Nom complet de l'autor a crear
     * 
     * @author Oscar
     * @since 1.0
     * @see Autor
     * @see AuthApiService.addAutor
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
     * Elimina un autor del sistema de forma permanent.
     * 
     * Aquest mètode realitza una petició HTTP DELETE al servidor per
     * eliminar un autor de la base de dades de la biblioteca.
     * 
     * **Advertències:**
     * - L'operació és irreversible
     * - Només administradors poden eliminar autors
     * - Si l'autor té llibres associats, pot produir un error o eliminar
     *   també els llibres (depèn de la configuració del backend)
     * 
     * **Estat d'Eliminació:**
     * - Abans de la petició: `isDeleting = id`
     * - Després de l'èxit: `isDeleting = null`, autor eliminat de la llista
     * - Després de l'error: `isDeleting = null`, `error = [missatge d'error]`
     * 
     * **Errors Possibles:**
     * - Error 404: L'autor no existeix
     * - Error 403: No tens permisos d'administrador
     * - Error 409: L'autor té llibres associats
     * 
     * @param id Identificador únic de l'autor a eliminar
     * 
     * @author Oscar
     * @since 1.0
     * @see AuthApiService.deleteAutor
     */
    fun deleteAutor(id: Long) {
        viewModelScope.launch {
            _autorsState.value = _autorsState.value.copy(isDeleting = id)
            try {
                val response = api.deleteAutor(id)
                if (response.isSuccessful) {
                    val updatedList = _autorsState.value.autors.filter { it.id != id }
                    _autorsState.value = _autorsState.value.copy(
                        autors = updatedList,
                        isDeleting = null
                    )
                } else {
                    // Si la resposta no és exitosa, llançar HttpException per gestionar l'error
                    throw retrofit2.HttpException(response)
                }
            } catch (e: Exception) {
                val errorMessage = if (e is retrofit2.HttpException) {
                    try {
                        val errorBody = e.response()?.errorBody()?.string()
                        errorBody ?: "Error eliminant autor: ${e.message}"
                    } catch (ex: Exception) {
                        "Error eliminant autor: ${e.message}"
                    }
                } else {
                    "Error eliminant autor: ${e.message}"
                }
                _autorsState.value = _autorsState.value.copy(
                    error = errorMessage,
                    isDeleting = null
                )
            }
        }
    }

    // ========== OPERACIONS AMB EXEMPLARS ==========

    /**
     * Carrega tots els exemplars del sistema des del backend.
     * 
     * Aquest mètode realitza una petició HTTP GET al servidor per obtenir
     * la llista completa d'exemplars físics registrats a la biblioteca.
     * 
     * **Estat de Càrrega:**
     * - Abans de la petició: `isLoading = true`
     * - Després de l'èxit: `isLoading = false`, `exemplars = [llista d'exemplars]`
     * - Després de l'error: `isLoading = false`, `error = [missatge d'error]`
     * 
     * **Informació Inclosa:**
     * Cada exemplar inclou la seva ubicació física, estat de disponibilitat
     * i la informació completa del llibre associat.
     * 
     * @author Oscar
     * @since 1.0
     * @see ExemplarsUiState
     * @see Exemplar
     * @see AuthApiService.getAllExemplars
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
     * Cerca exemplars disponibles (lliures) filtrant per títol del llibre o nom de l'autor.
     * 
     * Aquest mètode realitza una petició HTTP GET al servidor per obtenir
     * exemplars que estan disponibles per préstec ("reservat" == "lliure")
     * i que coincideixen amb els criteris de cerca proporcionats.
     * 
     * **Criteris de Cerca:**
     * - Si s'especifica `titol`: Busca exemplars de llibres que continguin el títol
     * - Si s'especifica `autor`: Busca exemplars de llibres escrits per l'autor
     * - Si ambdós són null: Retorna tots els exemplars disponibles
     * - La cerca és case-insensitive (no distingeix majúscules/minúscules)
     * - La cerca és parcial (coincidències parcials)
     * 
     * **Estat de Cerca:**
     * - Abans de la petició: `isSearching = true`
     * - Després de l'èxit: `isSearching = false`, `searchResults = [resultats]`
     * - Després de l'error: `isSearching = false`, `error = [missatge d'error]`
     * 
     * **Resultats:**
     * Els resultats s'emmagatzemen a `searchResults` i no modifiquen la
     * llista principal d'exemplars. Utilitza [clearSearchResults] per netejar
     * els resultats de cerca.
     * 
     * @param titol Títol del llibre a cercar (opcional, pot ser null)
     * @param autor Nom de l'autor a cercar (opcional, pot ser null)
     * 
     * @author Oscar
     * @since 1.0
     * @see Exemplar
     * @see ExemplarsUiState.searchResults
     * @see clearSearchResults
     * @see AuthApiService.getExemplarsLliures
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
     * Crea un nou exemplar físic d'un llibre al sistema.
     * 
     * Aquest mètode realitza una petició HTTP PUT al servidor per afegir
     * un nou exemplar físic a la biblioteca.
     * 
     * **Validacions:**
     * - L'exemplar ha d'estar associat a un llibre existent
     * - La ubicació (lloc) ha d'estar especificada
     * - Només administradors poden crear exemplars
     * - Per defecte, l'exemplar es crea amb estat "lliure"
     * 
     * **Estat de Creació:**
     * - Abans de la petició: `isCreating = true`
     * - Després de l'èxit: `isCreating = false`, exemplar afegit a la llista
     * - Després de l'error: `isCreating = false`, `error = [missatge d'error]`
     * 
     * **Ús Comú:**
     * S'utilitza quan s'afegeixen nous exemplars físics d'un llibre ja
     * existent a la biblioteca (ex: compra de més còpies).
     * 
     * @param exemplar Objecte [Exemplar] amb les dades del nou exemplar a crear
     * 
     * @author Oscar
     * @since 1.0
     * @see Exemplar
     * @see AuthApiService.addExemplar
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
     * Actualitza les dades d'un exemplar existent al sistema.
     * 
     * Aquest mètode és clau per la gestió de préstecs, ja que permet
     * modificar l'estat de disponibilitat d'un exemplar (ex: de "lliure"
     * a "prestat" quan es presta un llibre).
     * 
     * **Ús Principal:**
     * - Canviar l'estat d'un exemplar (lliure → prestat → lliure)
     * - Actualitzar la ubicació física de l'exemplar
     * - Gestionar reserves d'exemplars
     * 
     * **Estat d'Actualització:**
     * - Abans de la petició: `isUpdating = id`
     * - Després de l'èxit: `isUpdating = null`, exemplar actualitzat a la llista
     * - Després de l'error: `isUpdating = null`, `error = [missatge d'error]`
     * 
     * **Validacions:**
     * - L'exemplar amb l'ID proporcionat ha d'existir
     * - L'estat ha de ser vàlid: "lliure", "prestat", o "reservat"
     * 
     * **Errors Possibles:**
     * - Error 404: L'exemplar no existeix
     * - Error 400: Dades invàlides
     * - Error 403: No tens permisos
     * 
     * @param id Identificador únic de l'exemplar a actualitzar
     * @param exemplar Objecte [Exemplar] amb les dades actualitzades
     * 
     * @author Oscar
     * @since 2.0
     * @see Exemplar
     * @see ExemplarsUiState
     * @see AuthApiService.updateExemplar
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
     * Actualitza només l'estat d'un exemplar (mètode de conveniència).
     * 
     * Aquest mètode és una abstracció sobre [updateExemplar] que permet
     * canviar només l'estat de disponibilitat d'un exemplar sense necessitat
     * de passar tot l'objecte Exemplar.
     * 
     * **Ús Recomanat:**
     * Utilitza aquest mètode quan només necessites canviar l'estat de
     * l'exemplar (ex: marcar com "prestat" o "lliure"). Per a canvis més
     * complexos, utilitza [updateExemplar] directament.
     * 
     * **Estats Vàlids:**
     * - "lliure": Exemplar disponible per préstec
     * - "prestat": Exemplar actualment prestat a un usuari
     * - "reservat": Exemplar reservat per un usuari
     * 
     * **Processos:**
     * 1. Busca l'exemplar a la llista local per ID
     * 2. Crea una còpia amb l'estat actualitzat
     * 3. Crida a [updateExemplar] amb l'exemplar actualitzat
     * 
     * @param id Identificador únic de l'exemplar a actualitzar
     * @param newStatus Nou estat de disponibilitat ("lliure", "prestat", "reservat")
     * 
     * @author Oscar
     * @since 1.0
     * @see updateExemplar
     * @see Exemplar.reservat
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
     * Elimina un exemplar del sistema de forma permanent.
     * 
     * Aquest mètode realitza una petició HTTP DELETE al servidor per
     * eliminar un exemplar físic de la base de dades de la biblioteca.
     * 
     * **Advertències:**
     * - L'operació és irreversible
     * - Només administradors poden eliminar exemplars
     * - No es pot eliminar un exemplar que està actualment prestat
     * 
     * **Estat d'Eliminació:**
     * - Abans de la petició: `isDeleting = id`
     * - Després de l'èxit: `isDeleting = null`, exemplar eliminat de la llista
     * - Després de l'error: `isDeleting = null`, `error = [missatge d'error]`
     * 
     * **Errors Possibles:**
     * - Error 404: L'exemplar no existeix
     * - Error 403: No tens permisos d'administrador
     * - Error 400: L'exemplar està prestat i no es pot eliminar
     * 
     * @param id Identificador únic de l'exemplar a eliminar
     * 
     * @author Oscar
     * @since 1.0
     * @see AuthApiService.deleteExemplar
     */
    fun deleteExemplar(id: Long) {
        viewModelScope.launch {
            _exemplarsState.value = _exemplarsState.value.copy(isDeleting = id)
            try {
                val response = api.deleteExemplar(id)
                if (response.isSuccessful) {
                    val updatedList = _exemplarsState.value.exemplars.filter { it.id != id }
                    _exemplarsState.value = _exemplarsState.value.copy(
                        exemplars = updatedList,
                        isDeleting = null
                    )
                } else {
                    // Si la resposta no és exitosa, llançar HttpException per gestionar l'error
                    throw retrofit2.HttpException(response)
                }
            } catch (e: Exception) {
                val errorMessage = when {
                    e is retrofit2.HttpException && e.code() == 400 ->
                        "No es pot eliminar un exemplar prestat"

                    e is retrofit2.HttpException && e.code() == 404 ->
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
     * 
     * Aquest mètode elimina els resultats de cerca emmagatzemats a
     * [ExemplarsUiState.searchResults], deixant la propietat a `null`.
     * 
     * **Ús:**
     * Utilitza aquest mètode després de mostrar els resultats de cerca
     * o quan vols netejar la pantalla de cerca per iniciar una nova cerca.
     * 
     * **Efecte:**
     * - `searchResults = null` després de la crida
     * - No afecta la llista principal d'exemplars
     * 
     * @author Oscar
     * @since 1.0
     * @see ExemplarsUiState.searchResults
     * @see searchExemplarsLliures
     */
    fun clearSearchResults() {
        _exemplarsState.value = _exemplarsState.value.copy(searchResults = null)
    }

    // ========== FUNCIONS D'UTILITAT ==========

    /**
     * Neteja tots els missatges d'error de tots els estats.
     * 
     * Aquest mètode elimina els missatges d'error de les tres categories:
     * - Errors de llibres ([LlibresUiState.error])
     * - Errors d'autors ([AutorsUiState.error])
     * - Errors d'exemplars ([ExemplarsUiState.error])
     * 
     * **Ús:**
     * Utilitza aquest mètode quan l'usuari hagi vist l'error i vulguis
     * netejar-lo de la interfície, o després de realitzar una acció
     * exitosa que hagi resolt l'error anterior.
     * 
     * @author Oscar
     * @since 1.0
     * @see LlibresUiState.error
     * @see AutorsUiState.error
     * @see ExemplarsUiState.error
     */
    fun clearErrors() {
        _llibresState.value = _llibresState.value.copy(error = null)
        _autorsState.value = _autorsState.value.copy(error = null)
        _exemplarsState.value = _exemplarsState.value.copy(error = null)
    }

    /**
     * Recarrega totes les dades del sistema.
     * 
     * Aquest mètode realitza una recàrrega completa de totes les dades
     * del sistema: llibres, autors i exemplars. Utilitza les dades
     * més actualitzades del servidor.
     * 
     * **Processos:**
     * 1. Carrega tots els llibres ([loadLlibres])
     * 2. Carrega tots els autors ([loadAutors])
     * 3. Carrega tots els exemplars ([loadExemplars])
     * 
     * **Ús:**
     * Utilitza aquest mètode després de realitzar operacions que modifiquin
     * les dades del servidor per assegurar que la UI estigui sincronitzada
     * amb l'estat real del backend.
     * 
     * **Nota:**
     * Aquestes crides es realitzen en paral·lel, però cada una gestiona
     * el seu propi estat de càrrega i errors de forma independent.
     * 
     * @author Oscar
     * @since 1.0
     * @see loadLlibres
     * @see loadAutors
     * @see loadExemplars
     */
    fun refreshAll() {
        loadLlibres()
        loadAutors()
        loadExemplars()
    }

    /**
     * Busca un llibre a la llista local per el seu identificador.
     * 
     * Aquest mètode busca el llibre a la llista emmagatzemada localment
     * sense fer una petició al servidor. Si el llibre no està a la llista
     * local, retorna `null`.
     * 
     * **Ús:**
     * Utilitza aquest mètode quan necessitis accedir a les dades d'un
     * llibre que ja ha estat carregat prèviament.
     * 
     * **Nota:**
     * Si el llibre no està a la llista local, pot ser que no hagi estat
     * carregat encara. En aquest cas, considera cridar [loadLlibres] abans.
     * 
     * @param id Identificador únic del llibre a buscar
     * @return L'objecte [Llibre] si es troba, `null` si no existeix a la llista local
     * 
     * @author Oscar
     * @since 1.0
     * @see Llibre
     * @see loadLlibres
     */
    fun getLlibreById(id: Long): Llibre? {
        return _llibresState.value.llibres.find { it.id == id }
    }

    /**
     * Busca un autor a la llista local per el seu identificador.
     * 
     * Aquest mètode busca l'autor a la llista emmagatzemada localment
     * sense fer una petició al servidor. Si l'autor no està a la llista
     * local, retorna `null`.
     * 
     * **Ús:**
     * Utilitza aquest mètode quan necessitis accedir a les dades d'un
     * autor que ja ha estat carregat prèviament.
     * 
     * **Nota:**
     * Si l'autor no està a la llista local, pot ser que no hagi estat
     * carregat encara. En aquest cas, considera cridar [loadAutors] abans.
     * 
     * @param id Identificador únic de l'autor a buscar
     * @return L'objecte [Autor] si es troba, `null` si no existeix a la llista local
     * 
     * @author Oscar
     * @since 1.0
     * @see Autor
     * @see loadAutors
     */
    fun getAutorById(id: Long): Autor? {
        return _autorsState.value.autors.find { it.id == id }
    }

    /**
     * Busca un exemplar a la llista local per el seu identificador.
     * 
     * Aquest mètode busca l'exemplar a la llista emmagatzemada localment
     * sense fer una petició al servidor. Si l'exemplar no està a la llista
     * local, retorna `null`.
     * 
     * **Ús:**
     * Utilitza aquest mètode quan necessitis accedir a les dades d'un
     * exemplar que ja ha estat carregat prèviament.
     * 
     * **Nota:**
     * Si l'exemplar no està a la llista local, pot ser que no hagi estat
     * carregat encara. En aquest cas, considera cridar [loadExemplars] abans.
     * 
     * @param id Identificador únic de l'exemplar a buscar
     * @return L'objecte [Exemplar] si es troba, `null` si no existeix a la llista local
     * 
     * @author Oscar
     * @since 1.0
     * @see Exemplar
     * @see loadExemplars
     */
    fun getExemplarById(id: Long): Exemplar? {
        return _exemplarsState.value.exemplars.find { it.id == id }
    }

    /**
     * Comprova si un llibre té exemplars físics associats.
     * 
     * Aquest mètode verifica si existeix almenys un exemplar físic
     * associat al llibre especificat a la llista local d'exemplars.
     * 
     * **Ús:**
     * Utilitza aquest mètode per determinar si un llibre té còpies
     * físiques disponibles a la biblioteca abans de permetre certes
     * operacions (ex: eliminar el llibre).
     * 
     * **Nota:**
     * Aquesta comprovació es basa en les dades locals. Per assegurar
     * que les dades estan actualitzades, crida [loadExemplars] abans.
     * 
     * @param llibreId Identificador únic del llibre a comprovar
     * @return `true` si el llibre té almenys un exemplar, `false` altrament
     * 
     * @author Oscar
     * @since 1.0
     * @see Exemplar
     * @see getExemplarsByLlibre
     */
    fun llibreHasExemplars(llibreId: Long): Boolean {
        return _exemplarsState.value.exemplars.any { it.llibre?.id == llibreId }
    }

    /**
     * Obté el nombre d'exemplars disponibles (lliures) d'un llibre.
     * 
     * Aquest mètode compta quants exemplars físics d'un llibre estan
     * actualment disponibles per préstec (estat "lliure").
     * 
     * **Ús:**
     * Utilitza aquest mètode per mostrar a la UI quantes còpies d'un
     * llibre estan disponibles per préstec.
     * 
     * **Exemple:**
     * ```kotlin
     * val disponibles = viewModel.getExemplarsLliuresCount(llibreId)
     * Text("$disponibles exemplars disponibles")
     * ```
     * 
     * **Nota:**
     * Aquest recompte es basa en les dades locals. Per assegurar precisió,
     * crida [loadExemplars] abans d'utilitzar aquest mètode.
     * 
     * @param llibreId Identificador únic del llibre
     * @return Nombre d'exemplars disponibles (estat "lliure")
     * 
     * @author Oscar
     * @since 1.0
     * @see Exemplar.reservat
     * @see getExemplarsByLlibre
     */
    fun getExemplarsLliuresCount(llibreId: Long): Int {
        return _exemplarsState.value.exemplars.count {
            it.llibre?.id == llibreId && it.reservat == "lliure"
        }
    }

    /**
     * Obté tots els exemplars físics associats a un llibre.
     * 
     * Aquest mètode retorna una llista amb tots els exemplars físics
     * que estan associats al llibre especificat, independentment del
     * seu estat de disponibilitat.
     * 
     * **Ús:**
     * Utilitza aquest mètode quan necessitis mostrar o processar tots
     * els exemplars d'un llibre (disponibles, prestats, reservats).
     * 
     * **Informació Inclosa:**
     * Cada exemplar inclou:
     * - Identificador únic
     * - Ubicació física a la biblioteca
     * - Estat de disponibilitat
     * - Informació completa del llibre associat
     * 
     * @param llibreId Identificador únic del llibre
     * @return Llista d'[Exemplar] associats al llibre (pot estar buida)
     * 
     * @author Oscar
     * @since 1.0
     * @see Exemplar
     * @see getExemplarsLliuresCount
     * @see llibreHasExemplars
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