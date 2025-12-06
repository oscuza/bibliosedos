package com.oscar.bibliosedaos.ui.viewmodels

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oscar.bibliosedaos.data.models.*
import com.oscar.bibliosedaos.data.network.ApiClient
import com.oscar.bibliosedaos.data.network.AuthApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * ViewModel per gestionar préstecs de llibres.
 *
 * Aquest ViewModel gestiona totes les operacions relacionades amb els
 * préstecs de llibres, incloent:
 * - Visualització de préstecs actius
 * - Historial complet de préstecs
 * - Creació de nous préstecs (només admin)
 * - Devolució de llibres

 *
 * **Funcionalitats Principals:**
 * - Càrrega de préstecs actius i historial
 * - Gestió d'estats de càrrega i errors
 * - Actualització reactiva de la UI
 * - Validació abans de crear préstecs
 *
 * @property api Instància de l'API per comunicació amb el backend
 *
 * @author Oscar
 * @since 1.0
 * @see Prestec
 * @see CreatePrestecRequest
 */
class LoanViewModel(
    private val api: AuthApiService = ApiClient.instance
) : ViewModel() {

    // ========== ESTATS DELS PRÉSTECS ACTIUS ==========

    /**
     * Estat reactiu de la llista de préstecs actius.
     * 
     * Aquest StateFlow conté la informació sobre els préstecs que encara
     * no han estat retornats (dataDevolucio == null).
     * 
     * **Contingut:**
     * - Llista de préstecs actius
     * - Estat de càrrega
     * - Missatges d'error
     * 
     * @see LoansUiState
     * @see loadActiveLoans
     */
    private val _activeLoansState = MutableStateFlow(LoansUiState())
    val activeLoansState: StateFlow<LoansUiState> = _activeLoansState.asStateFlow()

    // ========== ESTATS DE L'HISTORIAL ==========

    /**
     * Estat reactiu de l'historial complet de préstecs.
     * 
     * Aquest StateFlow conté la informació sobre tots els préstecs,
     * tant actius com retornats (historial complet).
     * 
     * **Contingut:**
     * - Llista completa de préstecs
     * - Estat de càrrega
     * - Missatges d'error
     * 
     * @see LoansUiState
     * @see loadLoanHistory
     */
    private val _loanHistoryState = MutableStateFlow(LoansUiState())
    val loanHistoryState: StateFlow<LoansUiState> = _loanHistoryState.asStateFlow()

    // ========== ESTAT DE CREACIÓ DE PRÉSTEC ==========

    /**
     * Estat reactiu del formulari de creació de préstec.
     * 
     * Aquest StateFlow conté la informació sobre l'operació de crear
     * un nou préstec, incloent l'estat de submissió i els resultats.
     * 
     * **Contingut:**
     * - Estat de submissió (isSubmitting)
     * - Missatge d'èxit o error
     * - Préstec creat (si s'ha creat correctament)
     * 
     * @see CreateLoanState
     * @see createLoan
     */
    val _createLoanState = MutableStateFlow(CreateLoanState())
    val createLoanState: StateFlow<CreateLoanState> = _createLoanState.asStateFlow()

    // ========== ESTAT DE DEVOLUCIÓ ==========

    /**
     * Estat reactiu de l'operació de devolució de préstec.
     * 
     * Aquest StateFlow conté la informació sobre l'operació de retornar
     * un préstec, incloent l'estat de l'operació i els resultats.
     * 
     * **Contingut:**
     * - ID del préstec que s'està retornant (isReturning)
     * - Missatge d'èxit o error
     * - ID del préstec retornat (si s'ha retornat correctament)
     * 
     * @see ReturnLoanState
     * @see returnLoan
     */
    private val _returnLoanState = MutableStateFlow(ReturnLoanState())
    val returnLoanState: StateFlow<ReturnLoanState> = _returnLoanState.asStateFlow()

    // ========== OPERACIONS AMB PRÉSTECS ACTIUS ==========

    /**
     * Carrega la llista de préstecs actius des del backend.
     * 
     * Aquest mètode realitza una petició HTTP GET al servidor per obtenir
     * els préstecs que encara no han estat retornats (dataDevolucio == null).
     * 
     * **Comportament:**
     * - Si `usuariId` és `null`: Carrega tots els préstecs actius del sistema (només admin)
     * - Si `usuariId` és proporcionat: Carrega només els préstecs actius d'aquell usuari
     * 
     * **Estat de Càrrega:**
     * - Abans de la petició: `isLoading = true`
     * - Després de l'èxit: `isLoading = false`, `loans = [llista de préstecs actius]`
     * - Després de l'error: `isLoading = false`, `error = [missatge d'error]`
     * 
     * **Permisos:**
     * - Usuari normal: Pot carregar només els seus propis préstecs (ha de passar el seu ID)
     * - Administrador: Pot carregar tots els préstecs o els d'un usuari específic
     * 
     * **Errors Possibles:**
     * - Error 401/403: Token JWT invàlid o sense permisos
     * - Error 404: L'usuari no existeix (si s'especifica usuariId)
     * - Error de xarxa: Problemes de connexió amb el servidor
     * 
     * @param usuariId ID de l'usuari del qual carregar els préstecs (opcional, null per tots)
     * 
     * @author Oscar
     * @since 1.0
     * @see LoansUiState
     * @see AuthApiService.getPrestecsActius
     */
    fun loadActiveLoans(usuariId: Long? = null) {
        viewModelScope.launch {
            _activeLoansState.value = _activeLoansState.value.copy(isLoading = true)
            try {
                val loans = api.getPrestecsActius(usuariId)
                _activeLoansState.value = LoansUiState(
                    loans = loans,
                    isLoading = false
                )
            } catch (e: Exception) {
                _activeLoansState.value = LoansUiState(
                    error = "Error carregant préstecs actius: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    // ========== OPERACIONS AMB HISTORIAL ==========

    /**
     * Carrega l'historial complet de préstecs des del backend.
     * 
     * Aquest mètode realitza una petició HTTP GET al servidor per obtenir
     * tots els préstecs (tant actius com retornats) d'un usuari o del sistema.
     * 
     * **Comportament:**
     * - Inclou tant préstecs actius (dataDevolucio == null) com retornats (dataDevolucio != null)
     * - Si `usuariId` és `null`: Carrega tots els préstecs del sistema (només admin)
     * - Si `usuariId` és proporcionat: Carrega només l'historial d'aquell usuari
     * 
     * **Estat de Càrrega:**
     * - Abans de la petició: `isLoading = true`
     * - Després de l'èxit: `isLoading = false`, `loans = [llista completa de préstecs]`
     * - Després de l'error: `isLoading = false`, `error = [missatge d'error]`
     * 
     * **Permisos:**
     * - Usuari normal: Pot carregar només el seu propi historial (ha de passar el seu ID)
     * - Administrador: Pot carregar l'historial complet o el d'un usuari específic
     * 
     * **Ús:**
     * Utilitza aquest mètode per mostrar l'historial complet de préstecs,
     * incloent els que ja han estat retornats.
     * 
     * **Errors Possibles:**
     * - Error 401/403: Token JWT invàlid o sense permisos
     * - Error 404: L'usuari no existeix (si s'especifica usuariId)
     * - Error de xarxa: Problemes de connexió amb el servidor
     * 
     * @param usuariId ID de l'usuari del qual carregar l'historial (opcional, null per tots)
     * 
     * @author Oscar
     * @since 1.0
     * @see LoansUiState
     * @see loadActiveLoans
     * @see AuthApiService.getAllPrestecs
     */
    fun loadLoanHistory(usuariId: Long? = null) {
        viewModelScope.launch {
            _loanHistoryState.value = _loanHistoryState.value.copy(isLoading = true)
            try {
                val loans = api.getAllPrestecs(usuariId)
                _loanHistoryState.value = LoansUiState(
                    loans = loans,
                    isLoading = false
                )
            } catch (e: Exception) {
                _loanHistoryState.value = LoansUiState(
                    error = "Error carregant historial: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    // ========== OPERACIONS DE CREACIÓ ==========

    /**
     * Crea un nou préstec de llibre al sistema.
     * 
     * Aquest mètode realitza una petició HTTP POST al servidor per crear
     * un nou préstec d'un exemplar a un usuari.
     * 
     * **Validacions del Backend:**
     * - L'exemplar ha d'estar disponible (estat "lliure")
     * - L'usuari ha d'existir al sistema
     * - Només administradors poden crear préstecs
     * 
     * **Accions Automàtiques del Backend:**
     * - Marca l'exemplar com "prestat" (estat "prestat")
     * - Assigna `dataPrestec` a la data actual si no s'especifica
     * - Crea el registre del préstec a la base de dades
     * 
     * **Estat de Creació:**
     * - Abans de la petició: `isSubmitting = true`
     * - Després de l'èxit: `isSubmitting = false`, `success = true`, `createdLoan = [préstec creat]`
     * - Després de l'error: `isSubmitting = false`, `error = [missatge d'error]`
     * 
     * **Errors Possibles:**
     * - Error 400: L'exemplar ja està prestat o dades invàlides
     * - Error 404: L'exemplar o l'usuari no existeix
     * - Error 403: No tens permisos d'administrador
     * - Error de xarxa: Problemes de connexió amb el servidor
     * 
     * **Format de Data:**
     * La data ha d'estar en format ISO 8601: "yyyy-MM-dd" (ex: "2024-01-15").
     * Si no s'especifica, s'utilitza la data actual.
     * 
     * @param usuariId ID de l'usuari que farà el préstec
     * @param exemplarId ID de l'exemplar físic a prestar
     * @param dataPrestec Data del préstec en format "yyyy-MM-dd" (opcional, per defecte data actual)
     * 
     * @author Oscar
     * @since 1.0
     * @see CreateLoanState
     * @see CreatePrestecRequest
     * @see AuthApiService.createPrestec
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun createLoan(
        usuariId: Long,
        exemplarId: Long,
        dataPrestec: String? = null
    ) {
        viewModelScope.launch {
            _createLoanState.value = _createLoanState.value.copy(isSubmitting = true)

            try {
                // Preparar data del préstec (avui si no s'especifica)
                val dataActual = dataPrestec ?: LocalDate.now()
                    .format(DateTimeFormatter.ISO_LOCAL_DATE)

                // Crear request
                val request = CreatePrestecRequest(
                    dataPrestec = dataActual,
                    usuari = UserIdOnly(id = usuariId),
                    exemplar = ExemplarIdOnly(id = exemplarId)
                )

                // Enviar al backend
                val prestecCreat = api.createPrestec(request)

                _createLoanState.value = CreateLoanState(
                    success = true,
                    successMessage = "Préstec creat correctament",
                    createdLoan = prestecCreat
                )

            } catch (e: Exception) {
                val errorMessage = when {
                    e.message?.contains("400") == true -> "L'exemplar ja està prestat"
                    e.message?.contains("404") == true -> "Exemplar o usuari no trobat"
                    e.message?.contains("403") == true -> "No tens permisos per crear préstecs"
                    else -> "Error creant préstec: ${e.message}"
                }

                _createLoanState.value = _createLoanState.value.copy(
                    isSubmitting = false,
                    error = errorMessage
                )
            }
        }
    }

    // ========== OPERACIONS DE DEVOLUCIÓ ==========

    /**
     * Marca un préstec com a retornat al sistema.
     * 
     * Aquest mètode realitza una petició HTTP PUT al servidor per marcar
     * un préstec com retornat i alliberar l'exemplar associat.
     * 
     * **Accions Automàtiques del Backend:**
     * - Actualitza `dataDevolucio` amb la data actual
     * - Marca l'exemplar associat com "lliure" (disponible per préstec)
     * - Registra la devolució a la base de dades
     * 
     * **Permisos:**
     * - **Administrador**: Pot retornar qualsevol préstec del sistema
     * - **Usuari Normal**: Només pot retornar els seus propis préstecs actius
     * 
     * **Estat de Devolució:**
     * - Abans de la petició: `isReturning = prestecId`
     * - Després de l'èxit: `isReturning = null`, `success = true`, `returnedLoanId = prestecId`
     * - Després de l'error: `isReturning = null`, `error = [missatge d'error]`
     * 
     * **Errors Possibles:**
     * - Error 404: El préstec no existeix o ja està retornat
     * - Error 403: No tens permisos per retornar aquest préstec
     * - Error de xarxa: Problemes de connexió amb el servidor
     * 
     * **Nota Important:**
     * Després de retornar un préstec exitosament, és recomanable recarregar
     * la llista de préstecs per reflectir els canvis a la UI. Això no es fa
     * automàticament perquè la pantalla pot necessitar recarregar amb un
     * `usuariId` específic.
     * 
     * @param prestecId ID del préstec a retornar (no pot ser null)
     * 
     * @author Oscar
     * @since 1.0
     * @see ReturnLoanState
     * @see loadActiveLoans
     * @see loadLoanHistory
     * @see AuthApiService.retornarPrestec
     */
    fun returnLoan(prestecId: Long?) {
        viewModelScope.launch {
            _returnLoanState.value = _returnLoanState.value.copy(
                isReturning = prestecId
            )

            try {
                val response = api.retornarPrestec(prestecId)
                
                if (response.isSuccessful) {
                    // Llegir el cos de la resposta com a String
                    // Nota: response.body()?.string() només es pot cridar una vegada
                    val message = try {
                        response.body()?.use { it.string() }?.trim() ?: "Préstec retornat correctament"
                    } catch (e: IOException) {
                        // Si hi ha error llegint el cos, assumim que l'operació va bé
                        // (pot ser que el cos estigui buit o ja consumit)
                        "Préstec retornat correctament"
                    } catch (e: Exception) {
                        // Qualsevol altre error també assumim que va bé
                        "Préstec retornat correctament"
                    }
                    
                    _returnLoanState.value = ReturnLoanState(
                        success = true,
                        successMessage = message,
                        returnedLoanId = prestecId
                    )
                    
                    // NO recarregar automàticament els préstecs aquí
                    // La pantalla ho farà quan sigui necessari amb el usuariId correcte
                } else {
                    // Resposta no exitosa - llegir el missatge d'error si existeix
                    val errorMessage = try {
                        response.errorBody()?.use { it.string() }?.trim()
                    } catch (e: Exception) {
                        null
                    }
                    
                    val finalErrorMessage = errorMessage ?: when (response.code()) {
                        404 -> "Préstec no trobat"
                        403 -> "No tens permisos per retornar aquest préstec"
                        else -> "Error retornant préstec (codi ${response.code()})"
                    }
                    
                    _returnLoanState.value = _returnLoanState.value.copy(
                        isReturning = null,
                        error = finalErrorMessage
                    )
                }

            } catch (e: HttpException) {
                // HttpException s'hauria de capturar abans, però per seguretat
                val errorMessage = when (e.code()) {
                    404 -> "Préstec no trobat"
                    403 -> "No tens permisos per retornar aquest préstec"
                    else -> "Error retornant préstec (codi ${e.code()})"
                }
                
                _returnLoanState.value = _returnLoanState.value.copy(
                    isReturning = null,
                    error = errorMessage
                )
            } catch (e: IOException) {
                // Error de xarxa real
                _returnLoanState.value = _returnLoanState.value.copy(
                    isReturning = null,
                    error = "Error de connexió. Comprova la teva connexió a Internet"
                )
            } catch (e: Exception) {
                // Qualsevol altre error
                _returnLoanState.value = _returnLoanState.value.copy(
                    isReturning = null,
                    error = "Error retornant préstec: ${e.message ?: "Error desconegut"}"
                )
            }
        }
    }

    // ========== UTILITATS ==========

    /**
     * Neteja tots els missatges d'error de tots els estats del ViewModel.
     * 
     * Aquest mètode elimina els missatges d'error de:
     * - Préstecs actius ([activeLoansState])
     * - Historial de préstecs ([loanHistoryState])
     * - Creació de préstec ([createLoanState])
     * - Devolució de préstec ([returnLoanState])
     * 
     * **Ús:**
     * Utilitza aquest mètode quan l'usuari hagi vist l'error i vulguis
     * netejar-lo de la interfície, o després de realitzar una acció
     * exitosa que hagi resolt l'error anterior.
     * 
     * @author Oscar
     * @since 1.0
     * @see activeLoansState
     * @see loanHistoryState
     * @see createLoanState
     * @see returnLoanState
     */
    fun clearErrors() {
        _activeLoansState.value = _activeLoansState.value.copy(error = null)
        _loanHistoryState.value = _loanHistoryState.value.copy(error = null)
        _createLoanState.value = _createLoanState.value.copy(error = null)
        _returnLoanState.value = _returnLoanState.value.copy(error = null)
    }

    /**
     * Reinicia els estats de tots els formularis a valors inicials.
     * 
     * Aquest mètode restableix els estats de:
     * - Formulari de crear préstec ([createLoanState])
     * - Formulari de devolució ([returnLoanState])
     * 
     * **Ús:**
     * Utilitza aquest mètode després de completar una operació exitosa
     * o quan vulguis netejar els formularis per preparar-los per a una
     * nova entrada de l'usuari.
     * 
     * **Efecte:**
     * Tots els estats es restableixen a valors per defecte (buits, sense
     * errors, sense valors de submissió, etc.).
     * 
     * @author Oscar
     * @since 1.0
     * @see CreateLoanState
     * @see ReturnLoanState
     */
    fun resetForms() {
        _createLoanState.value = CreateLoanState()
        _returnLoanState.value = ReturnLoanState()
    }

    /**
     * Recarrega la llista de préstecs actius després d'una operació.
     * 
     * Aquest mètode és una conveniència que crida a [loadActiveLoans]
     * amb l'usuariId especificat. S'utilitza per refrescar la llista
     * després d'operacions que modifiquen els préstecs (crear, retornar).
     * 
     * **Ús:**
     * Utilitza aquest mètode després de crear o retornar un préstec per
     * assegurar que la llista mostrada a la UI estigui sincronitzada amb
     * l'estat real del servidor.
     * 
     * @param usuariId ID de l'usuari del qual recarregar els préstecs (opcional, null per tots)
     * 
     * @author Oscar
     * @since 1.0
     * @see loadActiveLoans
     */
    fun refreshActiveLoans(usuariId: Long? = null) {
        loadActiveLoans(usuariId)
    }

    /**
     * Recarrega l'historial de préstecs després d'una operació.
     * 
     * Aquest mètode és una conveniència que crida a [loadLoanHistory]
     * amb l'usuariId especificat. S'utilitza per refrescar l'historial
     * després d'operacions que modifiquen els préstecs (crear, retornar).
     * 
     * **Ús:**
     * Utilitza aquest mètode després de crear o retornar un préstec per
     * assegurar que l'historial mostrat a la UI estigui sincronitzat amb
     * l'estat real del servidor.
     * 
     * @param usuariId ID de l'usuari del qual recarregar l'historial (opcional, null per tots)
     * 
     * @author Oscar
     * @since 1.0
     * @see loadLoanHistory
     */
    fun refreshHistory(usuariId: Long? = null) {
        loadLoanHistory(usuariId)
    }
}

// ========== UI STATES ==========

/**
 * Estat de la UI per la llista de préstecs.
 * 
 * Aquesta classe de dades representa l'estat actual de la llista de préstecs
 * a la interfície d'usuari, incloent els préstecs, l'estat de càrrega i
 * possibles errors.
 * 
 * @property loans Llista de préstecs (actius o historial complet)
 * @property isLoading Indica si s'està carregant la llista des del servidor
 * @property error Missatge d'error si n'hi ha (null si no n'hi ha errors)
 * 
 * @author Oscar
 * @since 1.0
 * @see Prestec
 */
data class LoansUiState(
    val loans: List<Prestec> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * Estat del formulari de crear préstec.
 * 
 * Aquesta classe de dades representa l'estat actual del formulari de crear
 * un nou préstec, incloent l'estat de submissió, el resultat de l'operació
 * i possibles errors.
 * 
 * @property isSubmitting Indica si s'està enviant el formulari al servidor
 * @property success Indica si la creació del préstec ha estat exitosa
 * @property successMessage Missatge d'èxit després de crear el préstec
 * @property createdLoan Préstec creat (null si encara no s'ha creat)
 * @property error Missatge d'error si n'hi ha (null si no n'hi ha errors)
 * 
 * @author Oscar
 * @since 1.0
 * @see Prestec
 * @see LoanViewModel.createLoan
 */
data class CreateLoanState(
    val isSubmitting: Boolean = false,
    val success: Boolean = false,
    val successMessage: String? = null,
    val createdLoan: Prestec? = null,
    val error: String? = null
)

/**
 * Estat de l'operació de devolució de préstec.
 * 
 * Aquesta classe de dades representa l'estat actual de l'operació de retornar
 * un préstec, incloent l'ID del préstec que s'està retornant, el resultat
 * de l'operació i possibles errors.
 * 
 * @property isReturning ID del préstec que s'està retornant actualment (null si no n'hi ha cap)
 * @property success Indica si la devolució del préstec ha estat exitosa
 * @property successMessage Missatge d'èxit després de retornar el préstec
 * @property returnedLoanId ID del préstec retornat (null si encara no s'ha retornat)
 * @property error Missatge d'error si n'hi ha (null si no n'hi ha errors)
 * 
 * @author Oscar
 * @since 1.0
 * @see LoanViewModel.returnLoan
 */
data class ReturnLoanState(
    val isReturning: Long? = null,
    val success: Boolean = false,
    val successMessage: String? = null,
    val returnedLoanId: Long? = null,
    val error: String? = null
)
// ========== UTILITATS COMPARTIDES ==========

/**
 * Classe d'utilitats per gestionar estats i transicions.
 */
object LoanManagementUtils {

    /**
     * Valida si una transició d'estat és vàlida.
     */
    fun isValidStateTransition(from: String, to: String): Boolean {
        return when (from) {
            "lliure" -> to in listOf("prestat", "reservat")
            "prestat" -> to in listOf("lliure") // Només es pot retornar
            "reservat" -> to in listOf("lliure", "prestat") // Es pot alliberar o prestar
            else -> false
        }
    }

    /**
     * Determina si cal seleccionar usuari per una transició.
     */
    fun needsUserSelection(from: String, to: String): Boolean {
        return when {
            from == "lliure" && to in listOf("prestat", "reservat") -> true
            from == "reservat" && to == "prestat" -> true
            else -> false
        }
    }

    /**
     * Determina si cal retornar un préstec per una transició.
     */
    fun needsLoanReturn(from: String, to: String): Boolean {
        return from == "prestat" && to == "lliure"
    }

    /**
     * Formata una data per mostrar a la UI.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun formatDate(dateString: String): String {
        return try {
            val date = LocalDate.parse(dateString)
            date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        } catch (e: Exception) {
            dateString
        }
    }

    /**
     * Calcula els dies de préstec.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun calculateLoanDays(dataPrestec: String): Long {
        return try {
            val startDate = LocalDate.parse(dataPrestec)
            val today = LocalDate.now()
            java.time.temporal.ChronoUnit.DAYS.between(startDate, today)
        } catch (e: Exception) {
            0
        }
    }
}
