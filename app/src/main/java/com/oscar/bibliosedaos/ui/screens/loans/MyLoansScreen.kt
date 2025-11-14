package com.oscar.bibliosedaos.ui.screens.loans

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.AssignmentReturn
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.oscar.bibliosedaos.ui.viewmodels.AuthViewModel
import com.oscar.bibliosedaos.ui.viewmodels.LoanViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Tipus d'alerta per a préstecs.
 */
private enum class AlertType {
    WARNING,    // Alerta quan queden 2 dies o menys
    OVERDUE     // Alerta quan el préstec està en retard
}

/**
 * Pantalla de préstecs actius de l'usuari.
 *
 * **Descripció:**
 * Mostra els llibres prestats d'un usuari. Pot funcionar en dos modes:
 * - Sense userId: mostra els préstecs de l'usuari actual (autenticat)
 * - Amb userId: mostra els préstecs d'un usuari específic (per administradors)
 *
 * **Funcionalitats:**
 * - Llistat de préstecs actius amb informació detallada
 * - Informació del llibre, autor i data del préstec
 * - Informació dels dies restants per retornar el llibre (per a usuaris normals)
 * - Botó per retornar llibre (només administrador)
 * - Actualització automàtica després de retornar un llibre
 * - Indicador de càrrega durant les operacions
 * - Gestió d'errors amb missatges informatius
 *
 * **Permisos:**
 * - 👥 Usuari normal: veu només els seus préstecs amb informació dels dies restants. NO pot retornar llibres ell mateix (ha de portar-los a la biblioteca)
 * - 👨‍💼 Administrador: pot veure préstecs de qualsevol usuari I retornar-los manualment quan l'usuari porta el llibre físicament
 *
 * **Paràmetres:**
 * @param navController Controlador de navegació per gestionar la navegació entre pantalles
 * @param loanViewModel ViewModel que gestiona la lògica de negoci dels préstecs
 * @param authViewModel ViewModel que gestiona l'autenticació i informació de l'usuari
 * @param userId (Opcional) ID de l'usuari dels quals es volen veure els préstecs.
 *               Si és null, mostra els préstecs de l'usuari autenticat.
 *
 *
 * @author Oscar
 * @since 1.0
 * @see LoanViewModel
 * @see AuthViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyLoansScreen(
    navController: NavController,
    loanViewModel: LoanViewModel,
    authViewModel: AuthViewModel,
    userId: Long? = null  // null = usuari actual, valor = usuari específic
) {
    // ==================== ESTADOS ====================

    // Obtenir dades de l'usuari autenticat
    val loginState by authViewModel.loginUiState.collectAsState()
    val currentUserId = loginState.authResponse?.id
    val userRole = loginState.authResponse?.rol

    // Obtenir préstecs actius
    val activeLoansState by loanViewModel.activeLoansState.collectAsState()
    val returnLoanState by loanViewModel.returnLoanState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Determinar quin userId utilitzar
    val targetUserId = userId ?: currentUserId
    val isViewingOwnLoans = targetUserId == currentUserId
    val isAdmin = userRole == 2

    // Estats per a alertes de préstecs
    var showLoanAlert by remember { mutableStateOf(false) }
    var alertMessage by remember { mutableStateOf("") }
    var alertTitle by remember { mutableStateOf("") }
    var alertType by remember { mutableStateOf<AlertType?>(null) } // "warning" o "overdue"
    var hasShownAlertForCurrentLoans by remember { mutableStateOf(false) } // Per evitar mostrar l'alerta repetidament
    
    // ==================== DEBUG: Simulació d'alertes ====================
    // Per simular alertes, canvia aquest valor:
    // - null: utilitza el càlcul real
    // - -5: simula préstecs en retard (5 dies de retard)
    // - 1: simula préstecs urgents (1 dia restant)
    // - 2: simula préstecs urgents (2 dies restants)
    val DEBUG_SIMULATE_DAYS_REMAINING: Int? = null // Canvia aquest valor per simular

    // ==================== EFECTES ====================

    // Carregar préstecs quan canvia l'usuari
    LaunchedEffect(targetUserId) {
        // Resetear el flag d'alerta quan canvia l'usuari
        hasShownAlertForCurrentLoans = false
        
        if (targetUserId != null && targetUserId > 0) {
            loanViewModel.loadActiveLoans(targetUserId)
        }
    }

    // Mostrar errors amb snackbar
    LaunchedEffect(activeLoansState.error) {
        activeLoansState.error?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            loanViewModel.clearErrors()
        }
    }

    // Mostrar missatges d'èxit de devolució
    LaunchedEffect(returnLoanState.success) {
        if (returnLoanState.success) {
            returnLoanState.successMessage?.let { message ->
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Short
                )
            }
            // Recarregar la llista després de retornar amb el usuariId correcte
            if (targetUserId != null && targetUserId > 0) {
                loanViewModel.loadActiveLoans(targetUserId)
            }
            // Netejar estat de devolució
            loanViewModel.resetForms()
        }
    }

    // Mostrar errors de devolució
    LaunchedEffect(returnLoanState.error) {
        returnLoanState.error?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            loanViewModel.clearErrors()
        }
    }

    // Comprovar alertes de préstecs (només per a usuaris normals veient els seus propis préstecs)
    LaunchedEffect(activeLoansState.loans, isViewingOwnLoans, isAdmin, activeLoansState.isLoading, DEBUG_SIMULATE_DAYS_REMAINING) {
        // Resetear el flag quan canvia el mode debug per permetre tornar a mostrar l'alerta
        if (DEBUG_SIMULATE_DAYS_REMAINING != null) {
            hasShownAlertForCurrentLoans = false
        }
        
        if (isViewingOwnLoans && !isAdmin && activeLoansState.loans.isNotEmpty() && !activeLoansState.isLoading) {
            // Resetear el flag quan canvien els préstecs (per exemple, després de retornar un llibre)
            if (hasShownAlertForCurrentLoans && DEBUG_SIMULATE_DAYS_REMAINING == null) {
                // Si ja s'ha mostrat l'alerta per aquests préstecs, no tornar-la a mostrar
                // Només tornar-la a mostrar si els préstecs han canviat (per exemple, després de retornar un llibre)
                // O si estem en mode debug
                return@LaunchedEffect
            }
            
            // Buscar préstecs en retard (dies negatius)
            val overdueLoans = activeLoansState.loans.filter { loan ->
                val daysRemaining = DEBUG_SIMULATE_DAYS_REMAINING ?: calculateDaysRemaining(loan.dataPrestec)
                daysRemaining < 0
            }
            
            // Buscar préstecs amb 2 dies o menys (però no en retard)
            val urgentLoans = activeLoansState.loans.filter { loan ->
                val daysRemaining = DEBUG_SIMULATE_DAYS_REMAINING ?: calculateDaysRemaining(loan.dataPrestec)
                daysRemaining >= 0 && daysRemaining <= 2
            }
            
            // Prioritzar alerta de préstecs en retard
            if (overdueLoans.isNotEmpty()) {
                val bookTitles = overdueLoans.joinToString(", ") { it.exemplar.llibre?.titol ?: "Llibre desconegut" }
                val maxDaysOverdue = overdueLoans.maxOfOrNull { 
                    val daysRemaining = DEBUG_SIMULATE_DAYS_REMAINING ?: calculateDaysRemaining(it.dataPrestec)
                    -daysRemaining
                } ?: 0
                
                alertTitle = "⚠️ Préstecs en retard"
                alertMessage = if (overdueLoans.size == 1) {
                    "El llibre \"$bookTitles\" està en retard.\n\n" +
                    "Porta el llibre a la biblioteca el més aviat possible.\n\n" +
                    "Dies de retard: $maxDaysOverdue"
                } else {
                    "Tens ${overdueLoans.size} llibres en retard:\n$bookTitles\n\n" +
                    "Porta els llibres a la biblioteca el més aviat possible.\n\n" +
                    "Dies màxims de retard: $maxDaysOverdue"
                }
                alertType = AlertType.OVERDUE
                showLoanAlert = true
                hasShownAlertForCurrentLoans = true
            } else if (urgentLoans.isNotEmpty()) {
                val bookTitles = urgentLoans.joinToString(", ") { it.exemplar.llibre?.titol ?: "Llibre desconegut" }
                val minDaysRemaining = urgentLoans.minOfOrNull { 
                    DEBUG_SIMULATE_DAYS_REMAINING ?: calculateDaysRemaining(it.dataPrestec)
                } ?: 0
                
                alertTitle = "⏰ Recordatori de retorn"
                alertMessage = if (urgentLoans.size == 1) {
                    "El llibre \"$bookTitles\" s'ha de retornar aviat.\n\n" +
                    "Queden $minDaysRemaining ${if (minDaysRemaining == 1) "dia" else "dies"} per retornar-lo.\n\n" +
                    "Recorda portar-lo a la biblioteca abans que expiri el termini."
                } else {
                    "Tens ${urgentLoans.size} llibres que s'han de retornar aviat:\n$bookTitles\n\n" +
                    "Queden $minDaysRemaining ${if (minDaysRemaining == 1) "dia" else "dies"} per retornar-los.\n\n" +
                    "Recorda portar-los a la biblioteca abans que expiri el termini."
                }
                alertType = AlertType.WARNING
                showLoanAlert = true
                hasShownAlertForCurrentLoans = true
            }
        } else {
            // Resetear el flag quan no es compleixen les condicions (per exemple, quan es canvia d'usuari)
            hasShownAlertForCurrentLoans = false
        }
    }

    // ==================== UI ====================

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isViewingOwnLoans) {
                                "Els meus préstecs"
                            } else {
                                "Préstecs de l'usuari"
                            },
                            style = MaterialTheme.typography.headlineSmall
                        )
                        // Mostrar badge d'admin si està veient préstecs d'altri
                        if (!isViewingOwnLoans && isAdmin) {
                            Text(
                                text = "Mode administrador",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Tornar"
                        )
                    }
                },
                actions = {
                    // Botó de refrescar
                    IconButton(
                        onClick = {
                            loanViewModel.refreshActiveLoans(targetUserId)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Actualitzar"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                // Estado de carga
                activeLoansState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                // No hay préstamos
                activeLoansState.loans.isEmpty() -> {
                    EmptyLoansMessage(
                        isViewingOwnLoans = isViewingOwnLoans,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                // Mostrar lista de préstamos
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Header informatiu si és admin veient préstecs d'altri
                        if (!isViewingOwnLoans && isAdmin) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.AdminPanelSettings,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Mode Administrador",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer
                                            )
                                            Text(
                                                text = "Pots retornar manualment els llibres d'aquest usuari",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Llista de préstecs
                        items(
                            items = activeLoansState.loans,
                            key = { it.id!! }
                        ) { loan ->
                            LoanCard(
                                loan = loan,
                                canReturn = isAdmin && !isViewingOwnLoans, // Només admin pot retornar (no el propi usuari)
                                isReturning = returnLoanState.isReturning == loan.id,
                                isAdminReturning = !isViewingOwnLoans && isAdmin,
                                showDaysRemaining = true, // Mostrar dies restants sempre
                                isViewingOwnLoans = isViewingOwnLoans, // Per diferenciar l'estil
                                onReturnClick = {
                                    coroutineScope.launch {
                                        loanViewModel.returnLoan(loan.id)
                                    }
                                }
                            )
                        }
                    }
                }
            }
            
            // AlertDialog per a alertes de préstecs
            if (showLoanAlert) {
                AlertDialog(
                    onDismissRequest = { showLoanAlert = false },
                    title = {
                        Text(
                            text = alertTitle,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Text(
                            text = alertMessage,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = { showLoanAlert = false },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = when (alertType) {
                                    AlertType.OVERDUE -> MaterialTheme.colorScheme.error
                                    AlertType.WARNING -> MaterialTheme.colorScheme.primary
                                    null -> MaterialTheme.colorScheme.primary
                                }
                            )
                        ) {
                            Text("Entesos")
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface,
                    icon = {
                        Icon(
                            imageVector = when (alertType) {
                                AlertType.OVERDUE -> Icons.Default.Warning
                                AlertType.WARNING -> Icons.Default.Schedule
                                null -> Icons.Default.Info
                            },
                            contentDescription = null,
                            tint = when (alertType) {
                                AlertType.OVERDUE -> MaterialTheme.colorScheme.error
                                AlertType.WARNING -> MaterialTheme.colorScheme.primary
                                null -> MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier.size(48.dp)
                        )
                    }
                )
            }
        }
    }
}

/**
 * Calcula els dies restants per retornar un llibre.
 * 
 * **Lògica:**
 * - Període de préstec: 30 dies
 * - Calcula la diferència entre la data actual i la data límit (data de préstec + 30 dies)
 * 
 * @param dataPrestec Data del préstec en format "yyyy-MM-dd"
 * @return Nombre de dies restants (negatiu si està en retard)
 */
private fun calculateDaysRemaining(dataPrestec: String?): Int {
    if (dataPrestec == null) return 0
    
    return try {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val prestecDate = dateFormat.parse(dataPrestec) ?: return 0
        
        // Calcular la data límit (30 dies després del préstec)
        val calendar = Calendar.getInstance()
        calendar.time = prestecDate
        calendar.add(Calendar.DAY_OF_YEAR, 30) // Període de préstec: 30 dies
        val limitDate = calendar.time
        
        // Data actual
        val today = Date()
        
        // Calcular diferència en dies
        val diffInMillis = limitDate.time - today.time
        val diffInDays = (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
        
        diffInDays
    } catch (e: Exception) {
        0
    }
}

/**
 * Missatge quan no hi ha préstecs actius.
 */
@Composable
private fun EmptyLoansMessage(
    isViewingOwnLoans: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.BookmarkBorder,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isViewingOwnLoans) {
                "No tens cap llibre prestat"
            } else {
                "Aquest usuari no té préstecs actius"
            },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isViewingOwnLoans) {
                "Explora el catàleg i demana un llibre!"
            } else {
                ""
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

/**
 * Card que mostra informació d'un préstec individual.
 *
 * **Modificació:** 
 * - Afegit paràmetre isAdminReturning per mostrar text diferent quan l'admin retorna un llibre d'un altre usuari.
 * - Afegit paràmetre showDaysRemaining per mostrar els dies restants sempre.
 * - Afegit paràmetre isViewingOwnLoans per diferenciar l'estil entre usuari normal i admin.
 */
@Composable
private fun LoanCard(
    loan: com.oscar.bibliosedaos.data.models.Prestec,
    canReturn: Boolean,
    isReturning: Boolean,
    isAdminReturning: Boolean = false,
    showDaysRemaining: Boolean = false,
    isViewingOwnLoans: Boolean = false,
    onReturnClick: () -> Unit
) {
    var showReturnDialog by remember { mutableStateOf(false) }

    // Obtenir informació del llibre des de l'exemplar
    val bookTitle = loan.exemplar?.llibre?.titol ?: "Títol desconegut"
    val authorName = loan.exemplar?.llibre?.autor?.nom ?: "Autor desconegut"
    val isbn = loan.exemplar?.llibre?.isbn ?: "ISBN no disponible"
    val exemplarLocation = loan.exemplar?.lloc ?: "Ubicació desconeguda"

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Título del libro
            Text(
                text = bookTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Autor
            Text(
                text = "Autor: $authorName",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ISBN
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "ISBN: $isbn",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Ubicació de l'exemplar
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Ubicació: $exemplarLocation",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Fecha de préstamo
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Prestat el: ${loan.dataPrestec}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Mostrar dies restants sempre
            if (showDaysRemaining) {
                Spacer(modifier = Modifier.height(12.dp))
                
                // Dies restants
                val daysRemaining = calculateDaysRemaining(loan.dataPrestec)
                val (message, color) = when {
                    daysRemaining < 0 -> "Retard: ${-daysRemaining} dies" to MaterialTheme.colorScheme.error
                    daysRemaining == 0 -> "Últim dia per retornar" to MaterialTheme.colorScheme.error
                    daysRemaining <= 3 -> "Queden $daysRemaining dies" to MaterialTheme.colorScheme.error
                    daysRemaining <= 7 -> "Queden $daysRemaining dies" to MaterialTheme.colorScheme.tertiary
                    else -> "Queden $daysRemaining dies" to MaterialTheme.colorScheme.primary
                }
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = color.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = color
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = color
                        )
                    }
                }
                
                // Missatge per a usuaris normals
                if (isViewingOwnLoans) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Porta el llibre a la biblioteca per retornar-lo",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }

            // Botón de retorno (solo si puede retornar - només admin)
            if (canReturn) {
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { showReturnDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isReturning,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAdminReturning) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                ) {
                    if (isReturning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Retornant...")
                    } else {
                        Icon(
                            imageVector = if (isAdminReturning) {
                                Icons.Default.AdminPanelSettings
                            } else {
                                Icons.AutoMirrored.Filled.AssignmentReturn
                            },
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (isAdminReturning) {
                                "Retornar llibre (Admin)"
                            } else {
                                "Retornar llibre"
                            }
                        )
                    }
                }
            }
        }
    }

    // Diálogo de confirmación
    if (showReturnDialog) {
        AlertDialog(
            onDismissRequest = { showReturnDialog = false },
            icon = {
                Icon(
                    imageVector = if (isAdminReturning) {
                        Icons.Default.AdminPanelSettings
                    } else {
                        Icons.AutoMirrored.Filled.AssignmentReturn
                    },
                    contentDescription = null,
                    tint = if (isAdminReturning) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            },
            title = {
                Text(
                    if (isAdminReturning) {
                        "Retornar llibre (Administrador)"
                    } else {
                        "Retornar llibre"
                    }
                )
            },
            text = {
                Column {
                    Text(
                        "Estàs segur que vols retornar el llibre '$bookTitle'?"
                    )
                    if (isAdminReturning) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Com a administrador, estàs retornant aquest llibre manualment.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showReturnDialog = false
                        onReturnClick()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAdminReturning) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                ) {
                    Text("Retornar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReturnDialog = false }) {
                    Text("Cancel·lar")
                }
            }
        )
    }
}