package com.oscar.bibliosedaos.ui.viewmodels

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oscar.bibliosedaos.data.models.*
import com.oscar.bibliosedaos.data.network.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
class LoanViewModel : ViewModel() {

    val api = ApiClient.instance

    // ========== ESTATS DELS PRÉSTECS ACTIUS ==========

    /**
     * Estat de la llista de préstecs actius.
     */
    private val _activeLoansState = MutableStateFlow(LoansUiState())
    val activeLoansState: StateFlow<LoansUiState> = _activeLoansState.asStateFlow()

    // ========== ESTATS DE L'HISTORIAL ==========

    /**
     * Estat de l'historial complet de préstecs.
     */
    private val _loanHistoryState = MutableStateFlow(LoansUiState())
    val loanHistoryState: StateFlow<LoansUiState> = _loanHistoryState.asStateFlow()

    // ========== ESTAT DE CREACIÓ DE PRÉSTEC ==========

    /**
     * Estat del formulari de crear préstec.
     */
    val _createLoanState = MutableStateFlow(CreateLoanState())
    val createLoanState: StateFlow<CreateLoanState> = _createLoanState.asStateFlow()

    // ========== ESTAT DE DEVOLUCIÓ ==========

    /**
     * Estat de l'operació de devolució.
     */
    private val _returnLoanState = MutableStateFlow(ReturnLoanState())
    val returnLoanState: StateFlow<ReturnLoanState> = _returnLoanState.asStateFlow()

    // ========== OPERACIONS AMB PRÉSTECS ACTIUS ==========

    /**
     * Carrega la llista de préstecs actius d'un usuari.
     *
     * **Comportament:**
     * - Si usuariId és null, carrega tots els préstecs actius (només admin)
     * - Si usuariId és proporcionat, carrega només els préstecs d'aquell usuari
     *
     * @param usuariId ID de l'usuari (opcional)
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
     * Carrega l'historial complet de préstecs d'un usuari.
     *
     * **Comportament:**
     * - Inclou tant préstecs actius com retornats
     * - Si usuariId és null, carrega tots els préstecs (només admin)
     *
     * @param usuariId ID de l'usuari (opcional)
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
     * Crea un nou préstec de llibre.
     *
     * **Validacions:**
     * - L'exemplar ha d'estar disponible (estat "lliure")
     * - L'usuari ha d'existir
     * - Només administradors poden crear préstecs
     *
     * **Accions automàtiques del backend:**
     * - Marca l'exemplar com "prestat"
     * - Assigna dataPrestec a avui si no s'especifica
     *
     * @param usuariId ID de l'usuari que farà el préstec
     * @param exemplarId ID de l'exemplar a prestar
     * @param dataPrestec Data del préstec (opcional, per defecte avui)
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
     * Marca un préstec com a retornat.
     *
     * **Accions automàtiques del backend:**
     * - Actualitza dataDevolucio amb la data actual
     * - Marca l'exemplar com "lliure"
     *
     * **Permisos:**
     * - Administrador: pot retornar qualsevol préstec
     * - Usuari: només pot retornar els seus propis préstecs
     *
     * @param prestecId ID del préstec a retornar
     */
    fun returnLoan(prestecId: Long?) {
        viewModelScope.launch {
            _returnLoanState.value = _returnLoanState.value.copy(
                isReturning = prestecId
            )

            try {
                val message = api.retornarPrestec(prestecId)

                _returnLoanState.value = ReturnLoanState(
                    success = true,
                    successMessage = message,
                    returnedLoanId = prestecId
                )

            } catch (e: Exception) {
                val errorMessage = when {
                    e.message?.contains("404") == true -> "Préstec no trobat"
                    e.message?.contains("403") == true -> "No tens permisos per retornar aquest préstec"
                    else -> "Error retornant préstec: ${e.message}"
                }

                _returnLoanState.value = _returnLoanState.value.copy(
                    isReturning = null,
                    error = errorMessage
                )
            }
        }
    }

    // ========== UTILITATS ==========

    /**
     * Neteja els missatges d'error de tots els estats.
     */
    fun clearErrors() {
        _activeLoansState.value = _activeLoansState.value.copy(error = null)
        _loanHistoryState.value = _loanHistoryState.value.copy(error = null)
        _createLoanState.value = _createLoanState.value.copy(error = null)
        _returnLoanState.value = _returnLoanState.value.copy(error = null)
    }

    /**
     * Reinicia els estats de formularis.
     */
    fun resetForms() {
        _createLoanState.value = CreateLoanState()
        _returnLoanState.value = ReturnLoanState()
    }

    /**
     * Recarrega préstecs actius després d'una operació.
     *
     * @param usuariId ID de l'usuari
     */
    fun refreshActiveLoans(usuariId: Long? = null) {
        loadActiveLoans(usuariId)
    }

    /**
     * Recarrega historial després d'una operació.
     *
     * @param usuariId ID de l'usuari
     */
    fun refreshHistory(usuariId: Long? = null) {
        loadLoanHistory(usuariId)
    }
}

// ========== UI STATES ==========

/**
 * Estat de la UI per la llista de préstecs.
 *
 * @property loans Llista de préstecs
 * @property isLoading Si s'està carregant
 * @property error Missatge d'error (null si no n'hi ha)
 */
data class LoansUiState(
    val loans: List<Prestec> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * Estat del formulari de crear préstec.
 *
 * @property isSubmitting Si s'està enviant el formulari
 * @property success Si la creació ha estat exitosa
 * @property successMessage Missatge d'èxit
 * @property createdLoan Préstec creat
 * @property error Missatge d'error
 */
data class CreateLoanState(
    val isSubmitting: Boolean = false,
    val success: Boolean = false,
    val successMessage: String? = null,
    val createdLoan: Prestec? = null,
    val error: String? = null
)

/**
 * Estat de l'operació de devolució.
 *
 * @property isReturning ID del préstec que s'està retornant (null si cap)
 * @property success Si la devolució ha estat exitosa
 * @property successMessage Missatge d'èxit
 * @property returnedLoanId ID del préstec retornat
 * @property error Missatge d'error
 */
data class ReturnLoanState(
    val isReturning: Long? = null,
    val success: Boolean = false,
    val successMessage: String? = null,
    val returnedLoanId: Long? = null,
    val error: String? = null
)
@RequiresApi(Build.VERSION_CODES.O)
fun LoanViewModel.createLoanImproved(
    usuariId: Long,
    exemplarId: Long,
    dataPrestec: String? = null,
    onSuccess: () -> Unit = {},
    onError: (String) -> Unit = {}
) {
    viewModelScope.launch {
        _createLoanState.value = CreateLoanState(isSubmitting = true)

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

            // Callback d'èxit
            onSuccess()

        } catch (e: Exception) {
            val errorMessage = when {
                e.message?.contains("400") == true ->
                    "L'exemplar ja està prestat o no està disponible"
                e.message?.contains("404") == true ->
                    "Exemplar o usuari no trobat"
                e.message?.contains("403") == true ->
                    "No tens permisos per crear préstecs"
                e.message?.contains("409") == true ->
                    "L'exemplar ja té un préstec actiu"
                else ->
                    "Error creant préstec: ${e.message}"
            }

            _createLoanState.value = CreateLoanState(
                isSubmitting = false,
                error = errorMessage
            )

            // Callback d'error
            onError(errorMessage)
        }
    }
}

/**
 * Mètode per obtenir préstecs actius d'un exemplar específic.
 */
fun LoanViewModel.getActiveLoanForExemplar(exemplarId: Long): Prestec? {
    return activeLoansState.value.loans.find {
        it.exemplar.id == exemplarId && it.isActive
    }
}

/**
 * Mètode per comprovar si un usuari té préstecs actius.
 */
fun LoanViewModel.userHasActiveLoans(userId: Long): Boolean {
    return activeLoansState.value.loans.any {
        it.usuari?.id == userId && it.isActive
    }
}

/**
 * Mètode per obtenir el nombre de préstecs actius d'un usuari.
 */
fun LoanViewModel.getActiveLoansCountForUser(userId: Long): Int {
    return activeLoansState.value.loans.count {
        it.usuari?.id == userId && it.isActive
    }
}
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
