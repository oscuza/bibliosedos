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
import com.oscar.bibliosedaos.data.models.Prestec
import com.oscar.bibliosedaos.ui.viewmodels.AuthViewModel
import com.oscar.bibliosedaos.ui.viewmodels.LoanViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Pantalla de historial complet de préstecs de l'usuari.
 *
 * **Descripció:**
 * Mostra tots els préstecs (actius i retornats) d'un usuari. Pot funcionar en dos modes:
 * - Sense userId: mostra l'historial de l'usuari actual (autenticat)
 * - Amb userId: mostra l'historial d'un usuari específic (per administradors)
 *
 * **Funcionalitats:**
 * - Llistat complet de préstecs (actius i retornats)
 * - Informació del llibre, autor i dates del préstec
 * - Indicador visual diferent per préstecs actius vs retornats
 * - Botó per retornar llibre (només per préstecs actius)
 * - Filtre per veure només actius o només retornats
 * - Actualització automàtica després de retornar un llibre
 * - Indicador de càrrega durant les operacions
 * - Gestió d'errors amb missatges informatius
 *
 * **Permisos:**
 * - 👥 Usuari normal: veu només el seu historial amb informació dels dies restants. NO pot retornar llibres ell mateix (ha de portar-los a la biblioteca)
 * - 👨‍💼 Administrador: pot veure historial de qualsevol usuari i retornar préstecs manualment
 *
 * **Paràmetres:**
 * @param navController Controlador de navegació per gestionar la navegació entre pantalles
 * @param loanViewModel ViewModel que gestiona la lògica de negoci dels préstecs
 * @param authViewModel ViewModel que gestiona l'autenticació i informació de l'usuari
 * @param userId (Opcional) ID de l'usuari del qual es vol veure l'historial.
 *               Si és null, mostra l'historial de l'usuari autenticat.
 *
 * @author Oscar
 * @since 1.0
 * @see LoanViewModel
 * @see AuthViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanHistoryScreen(
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

    // Obtenir historial de préstecs
    val loanHistoryState by loanViewModel.loanHistoryState.collectAsState()
    val returnLoanState by loanViewModel.returnLoanState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Filtre per mostrar actius o retornats
    var filterType by remember { mutableIntStateOf(0) } // 0 = Tots, 1 = Actius, 2 = Retornats

    // Determinar quin userId utilitzar
    val targetUserId = userId ?: currentUserId
    val isViewingOwnHistory = targetUserId == currentUserId
    val isAdmin = userRole == 2

    // Filtrar préstecs segons el tipus seleccionat
    val filteredLoans = when (filterType) {
        1 -> loanHistoryState.loans.filter { it.isActive }
        2 -> loanHistoryState.loans.filter { !it.isActive }
        else -> loanHistoryState.loans
    }

    // Ordenar per data de préstec (més recents primer)
    val sortedLoans = filteredLoans.sortedByDescending { 
        try {
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).parse(it.dataPrestec)
        } catch (e: Exception) {
            null
        }
    }

    // ==================== EFECTES ====================

    // Carregar historial quan canvia l'usuari
    LaunchedEffect(targetUserId) {
        if (targetUserId != null && targetUserId > 0) {
            loanViewModel.loadLoanHistory(targetUserId)
        }
    }

    // Mostrar errors amb snackbar
    LaunchedEffect(loanHistoryState.error) {
        loanHistoryState.error?.let {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = it,
                    duration = SnackbarDuration.Short
                )
            }
            loanViewModel.clearErrors()
        }
    }

    // Mostrar missatges d'èxit de devolució
    LaunchedEffect(returnLoanState.success) {
        if (returnLoanState.success) {
            returnLoanState.successMessage?.let { message ->
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        message = message,
                        duration = SnackbarDuration.Short
                    )
                }
            }
            // Recarregar l'historial després de retornar
            loanViewModel.refreshHistory(targetUserId)
            // Netejar estat de devolució
            loanViewModel.resetForms()
        }
    }

    // Mostrar errors de devolució
    LaunchedEffect(returnLoanState.error) {
        returnLoanState.error?.let {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(
                    message = it,
                    duration = SnackbarDuration.Short
                )
            }
            loanViewModel.clearErrors()
        }
    }

    // ==================== UI ====================

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isViewingOwnHistory) {
                                "Historial de Préstecs"
                            } else {
                                "Historial de Préstecs"
                            },
                            style = MaterialTheme.typography.headlineSmall
                        )
                        // Mostrar badge d'admin si està veient historial d'altri
                        if (!isViewingOwnHistory && isAdmin) {
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
                            loanViewModel.refreshHistory(targetUserId)
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
                loanHistoryState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                // No hay préstamos
                loanHistoryState.loans.isEmpty() -> {
                    EmptyHistoryMessage(
                        isViewingOwnHistory = isViewingOwnHistory,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                // Mostrar lista de préstamos
                else -> {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Filtres
                        FilterChips(
                            filterType = filterType,
                            onFilterChange = { filterType = it },
                            totalCount = loanHistoryState.loans.size,
                            activeCount = loanHistoryState.loans.count { it.isActive },
                            returnedCount = loanHistoryState.loans.count { !it.isActive },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        )

                        // Llista de préstecs
                        if (sortedLoans.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FilterList,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = when (filterType) {
                                            1 -> "No tens préstecs actius"
                                            2 -> "No tens préstecs retornats"
                                            else -> "No hi ha préstecs"
                                        },
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Header informatiu si és admin veient historial d'altri
                                if (!isViewingOwnHistory && isAdmin) {
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
                                    items = sortedLoans,
                                    key = { it.id!! }
                                ) { loan ->
                                    LoanHistoryCard(
                                        loan = loan,
                                        canReturn = loan.isActive && isAdmin && !isViewingOwnHistory, // Només admin pot retornar (no el propi usuari)
                                        isReturning = returnLoanState.isReturning == loan.id,
                                        isAdminReturning = !isViewingOwnHistory && isAdmin,
                                        isViewingOwnHistory = isViewingOwnHistory,
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
                }
            }
        }
    }
}

/**
 * Filtres per mostrar tots, actius o retornats.
 */
@Composable
private fun FilterChips(
    filterType: Int,
    onFilterChange: (Int) -> Unit,
    totalCount: Int,
    activeCount: Int,
    returnedCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = filterType == 0,
            onClick = { onFilterChange(0) },
            label = { Text("Tots ($totalCount)") },
            leadingIcon = {
                if (filterType == 0) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        )
        FilterChip(
            selected = filterType == 1,
            onClick = { onFilterChange(1) },
            label = { Text("Actius ($activeCount)") },
            leadingIcon = {
                if (filterType == 1) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        )
        FilterChip(
            selected = filterType == 2,
            onClick = { onFilterChange(2) },
            label = { Text("Retornats ($returnedCount)") },
            leadingIcon = {
                if (filterType == 2) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        )
    }
}

/**
 * Missatge quan no hi ha historial.
 */
@Composable
private fun EmptyHistoryMessage(
    isViewingOwnHistory: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.History,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isViewingOwnHistory) {
                "No tens cap préstec al teu historial"
            } else {
                "Aquest usuari no té préstecs al seu historial"
            },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isViewingOwnHistory) {
                "Tots els teus préstecs apareixeran aquí"
            } else {
                ""
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

/**
 * Card que mostra informació d'un préstec individual a l'historial.
 */
@Composable
private fun LoanHistoryCard(
    loan: Prestec,
    canReturn: Boolean,
    isReturning: Boolean,
    isAdminReturning: Boolean = false,
    isViewingOwnHistory: Boolean = false,
    onReturnClick: () -> Unit
) {
    var showReturnDialog by remember { mutableStateOf(false) }

    // Obtenir informació del llibre des de l'exemplar
    val bookTitle = loan.exemplar?.llibre?.titol ?: "Títol desconegut"
    val isActive = loan.isActive

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Títol del llibre
            Text(
                text = bookTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Dates
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
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
                        text = "Prestat: ${formatDate(loan.dataPrestec)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Mostrar dies restants per a préstecs actius
                if (isActive) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
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
                    if (isViewingOwnHistory) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Porta el llibre a la biblioteca per retornar-lo",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
                
                if (loan.dataDevolucio != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Retornat: ${formatDate(loan.dataDevolucio)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            // Botó de retorno (solo si puede retornar i està actiu)
            if (canReturn && isActive) {
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
    if (showReturnDialog && isActive) {
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

/**
 * Formateja una data de format "yyyy-MM-dd" a "dd/MM/yyyy".
 */
private fun formatDate(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val date = inputFormat.parse(dateString)
        if (date != null) {
            outputFormat.format(date)
        } else {
            dateString
        }
    } catch (e: Exception) {
        dateString
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

