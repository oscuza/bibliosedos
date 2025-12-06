package com.oscar.bibliosedaos.ui.screens.admin

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.oscar.bibliosedaos.data.local.Sanction
import com.oscar.bibliosedaos.data.local.SanctionManager
import com.oscar.bibliosedaos.data.models.Prestec
import com.oscar.bibliosedaos.data.network.User
import com.oscar.bibliosedaos.navigation.AppScreens
import com.oscar.bibliosedaos.ui.viewmodels.AuthViewModel
import com.oscar.bibliosedaos.ui.viewmodels.LoanViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Pantalla de gestió d'usuaris amb préstecs en retard o retornats tard.
 *
 * **Descripció:**
 * Pantalla exclusiva per a administradors que mostra un llistat de tots els usuaris
 * que tenen préstecs en retard (actius) o que han retornat llibres tard (històrics).
 *
 * **Funcionalitats:**
 * - Llistat d'usuaris amb préstecs en retard (actius)
 * - Llistat d'usuaris que han retornat llibres tard (històrics)
 * - Agrupació per usuari amb informació detallada
 * - Indicadors visuals de gravetat
 * - Navegació als préstecs de cada usuari
 * - Funcionalitat de sancions (pendent d'implementar)
 *
 * **Accés:**
 * Només accessible per usuaris amb rol d'administrador (rol=2).
 *
 * @param navController Controlador de navegació per transicions entre pantalles
 * @param loanViewModel ViewModel per gestionar operacions de préstecs
 * @param authViewModel ViewModel per gestionar informació d'usuaris
 *
 * @author Oscar
 * @since 1.0
 * @see MyLoansScreen
 * @see AdminHomeScreen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverdueLoansScreen(
    navController: NavController,
    loanViewModel: LoanViewModel,
    authViewModel: AuthViewModel
) {
    // ========== Estats Observables ==========

    /**
     * Estat dels préstecs actius (tots els del sistema).
     */
    val activeLoansState by loanViewModel.activeLoansState.collectAsState()

    /**
     * Estat de l'historial de préstecs (tots els del sistema).
     */
    val loanHistoryState by loanViewModel.loanHistoryState.collectAsState()

    /**
     * Estat del login per verificar que és administrador.
     */
    val loginState by authViewModel.loginUiState.collectAsState()

    // ========== Estats Locals ==========

    /**
     * Llista processada d'usuaris amb préstecs en retard o retornats tard.
     */
    var usersWithOverdueLoans by remember { mutableStateOf<List<UserWithOverdueInfo>>(emptyList()) }

    /**
     * Filtre per mostrar només actius, només històrics, o tots.
     */
    var filterType by remember { mutableStateOf<OverdueFilterType>(OverdueFilterType.ALL) }

    /**
     * Flag per prevenir doble clic al botó enrere.
     */
    var isNavigating by remember { mutableStateOf(false) }

    /**
     * Context per accedir a SharedPreferences.
     */
    val context = LocalContext.current

    /**
     * Usuari seleccionat per aplicar sanció.
     */
    var selectedUserForSanction by remember { mutableStateOf<UserWithOverdueInfo?>(null) }

    /**
     * Mostrar diàleg de sanció.
     */
    var showSanctionDialog by remember { mutableStateOf(false) }

    /**
     * Usuari seleccionat per eliminar sanció.
     */
    var selectedUserForRemoveSanction by remember { mutableStateOf<UserWithOverdueInfo?>(null) }

    /**
     * Mostrar diàleg de confirmació per eliminar sanció.
     */
    var showRemoveSanctionDialog by remember { mutableStateOf(false) }

    /**
     * Mostrar diàleg de confirmació per eliminar tot l'historial de sancions.
     */
    var showClearAllSanctionsDialog by remember { mutableStateOf(false) }

    /**
     * Sancions actives per usuari.
     */
    var userSanctions by remember { mutableStateOf<Map<Long, Sanction>>(emptyMap()) }

    /**
     * ID de l'admin actual.
     */
    val currentAdminId = loginState.authResponse?.id ?: 0L

    // ==================== DEBUG: Simulació de préstecs en retard ====================
    // Per simular préstecs en retard, canvia aquest valor:
    // - null: utilitza el càlcul real
    // - -5: simula préstecs actius en retard (5 dies de retard)
    // - 10: simula préstecs retornats tard (10 dies de retard quan es va retornar)
    val DEBUG_SIMULATE_DAYS_OVERDUE: Int? = -5 // Canvia aquest valor per simular
    val DEBUG_SIMULATE_DAYS_LATE_RETURN: Int? = 8 // Canvia aquest valor per simular retornats tard

    // ========== Efectes ==========

    // Funció per recarregar sancions
    val reloadSanctions: () -> Unit = {
        SanctionManager.cleanExpiredSanctions(context.applicationContext)
        val sanctions = SanctionManager.getAllActiveSanctions(context.applicationContext)
        userSanctions = sanctions.associateBy { it.userId }
    }

    // Carregar sancions actives
    LaunchedEffect(Unit) {
        reloadSanctions()
    }

    // Carregar préstecs quan es carrega la pantalla
    LaunchedEffect(Unit) {
        loanViewModel.loadActiveLoans(null) // Tots els préstecs actius
        loanViewModel.loadLoanHistory(null) // Tots els préstecs històrics
    }

    // Refrescar quan es torna a la pantalla
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                loanViewModel.loadActiveLoans(null)
                loanViewModel.loadLoanHistory(null)
                // Recarregar sancions quan es torna a la pantalla
                reloadSanctions()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Processar préstecs per detectar en retard o retornats tard
    LaunchedEffect(activeLoansState.loans, loanHistoryState.loans, filterType, DEBUG_SIMULATE_DAYS_OVERDUE, DEBUG_SIMULATE_DAYS_LATE_RETURN) {
        val overdueActiveLoans = activeLoansState.loans.filter { loan ->
            val daysRemaining = DEBUG_SIMULATE_DAYS_OVERDUE ?: calculateDaysRemaining(loan.dataPrestec)
            daysRemaining < 0
        }

        val overdueReturnedLoans = if (DEBUG_SIMULATE_DAYS_LATE_RETURN != null) {
            // Simular préstecs retornats tard
            loanHistoryState.loans.filter { loan ->
                loan.dataDevolucio != null && DEBUG_SIMULATE_DAYS_LATE_RETURN > 0
            }
        } else {
            loanHistoryState.loans.filter { loan ->
                loan.dataDevolucio != null && wasReturnedLate(loan.dataPrestec, loan.dataDevolucio)
            }
        }

        // Agrupar per usuari
        val userMap = mutableMapOf<Long, UserWithOverdueInfo>()

        // Processar préstecs actius en retard
        if (filterType == OverdueFilterType.ALL || filterType == OverdueFilterType.ACTIVE) {
            overdueActiveLoans.forEach { loan ->
                val userId = loan.usuari?.id ?: return@forEach
                val user = loan.usuari
                val daysRemaining = DEBUG_SIMULATE_DAYS_OVERDUE ?: calculateDaysRemaining(loan.dataPrestec)
                val daysOverdue = -daysRemaining

                if (userMap.containsKey(userId)) {
                    val existing = userMap[userId]!!
                    userMap[userId] = existing.copy(
                        activeOverdueLoans = existing.activeOverdueLoans + loan,
                        maxDaysOverdue = maxOf(existing.maxDaysOverdue, daysOverdue)
                    )
                } else {
                    userMap[userId] = UserWithOverdueInfo(
                        user = user,
                        activeOverdueLoans = listOf(loan),
                        returnedLateLoans = emptyList(),
                        maxDaysOverdue = daysOverdue,
                        maxDaysLateReturn = 0
                    )
                }
            }
        }

        // Processar préstecs retornats tard
        if (filterType == OverdueFilterType.ALL || filterType == OverdueFilterType.RETURNED) {
            overdueReturnedLoans.forEach { loan ->
                val userId = loan.usuari?.id ?: return@forEach
                val user = loan.usuari
                val daysLate = DEBUG_SIMULATE_DAYS_LATE_RETURN ?: calculateDaysLateReturn(loan.dataPrestec, loan.dataDevolucio!!)

                if (userMap.containsKey(userId)) {
                    val existing = userMap[userId]!!
                    userMap[userId] = existing.copy(
                        returnedLateLoans = existing.returnedLateLoans + loan,
                        maxDaysLateReturn = maxOf(existing.maxDaysLateReturn, daysLate)
                    )
                } else {
                    userMap[userId] = UserWithOverdueInfo(
                        user = user,
                        activeOverdueLoans = emptyList(),
                        returnedLateLoans = listOf(loan),
                        maxDaysOverdue = 0,
                        maxDaysLateReturn = daysLate
                    )
                }
            }
        }

        // Ordenar per gravetat (més dies de retard primer)
        usersWithOverdueLoans = userMap.values.sortedByDescending { 
            maxOf(it.maxDaysOverdue, it.maxDaysLateReturn)
        }
    }

    // ========== UI ==========

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Préstecs en retard",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = "Usuaris amb sancions pendents",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (!isNavigating) {
                                isNavigating = true
                                navController.popBackStack()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Tornar"
                        )
                    }
                },
                actions = {
                    // Botó per eliminar tot l'historial de sancions
                    IconButton(
                        onClick = { showClearAllSanctionsDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Eliminar historial de sancions",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    titleContentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                // Estat: Carregant
                activeLoansState.isLoading || loanHistoryState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                // Estat: Error
                activeLoansState.error != null || loanHistoryState.error != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = activeLoansState.error ?: loanHistoryState.error ?: "Error desconegut",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                // Estat: Llista buida
                usersWithOverdueLoans.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "No hi ha préstecs en retard",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Tots els préstecs estan al dia",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Estat: Llista amb usuaris
                else -> {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Filtres
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = filterType == OverdueFilterType.ALL,
                                onClick = { filterType = OverdueFilterType.ALL },
                                label = { Text("Tots") }
                            )
                            FilterChip(
                                selected = filterType == OverdueFilterType.ACTIVE,
                                onClick = { filterType = OverdueFilterType.ACTIVE },
                                label = { Text("Sancions Actives") }
                            )
                            FilterChip(
                                selected = filterType == OverdueFilterType.RETURNED,
                                onClick = { filterType = OverdueFilterType.RETURNED },
                                label = { Text("Retornats tard") }
                            )
                        }

                        // Llista d'usuaris
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = usersWithOverdueLoans,
                                key = { it.user.id ?: 0L }
                            ) { userInfo ->
                                // Buscar sanció per ID d'usuari
                                val userId = userInfo.user.id ?: 0L
                                val userSanction = userSanctions[userId]
                                UserOverdueCard(
                                    userInfo = userInfo,
                                    filterType = filterType,
                                    sanction = userSanction,
                                    onUserClick = {
                                        navController.navigate(
                                            AppScreens.MyLoansScreen.createRoute(userId)
                                        )
                                    },
                                    onApplySanctionClick = {
                                        selectedUserForSanction = userInfo
                                        showSanctionDialog = true
                                    },
                                    onRemoveSanctionClick = {
                                        selectedUserForRemoveSanction = userInfo
                                        showRemoveSanctionDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Diàleg per aplicar sancions
            if (showSanctionDialog && selectedUserForSanction != null) {
                SanctionDialog(
                    userInfo = selectedUserForSanction!!,
                    onDismiss = {
                        showSanctionDialog = false
                        selectedUserForSanction = null
                    },
                    onApply = { reason, durationDays ->
                        val userId = selectedUserForSanction!!.user.id ?: 0L
                        SanctionManager.applySanction(
                            context = context.applicationContext,
                            userId = userId,
                            reason = reason,
                            durationDays = durationDays,
                            adminId = currentAdminId
                        )
                        // Recarregar sancions immediatament després d'aplicar
                        reloadSanctions()
                        // Tancar diàleg
                        selectedUserForSanction = null
                        showSanctionDialog = false
                    }
                )
            }

            // Diàleg de confirmació per eliminar sanció
            if (showRemoveSanctionDialog && selectedUserForRemoveSanction != null) {
                val userInfo = selectedUserForRemoveSanction!!
                val sanction = userSanctions[userInfo.user.id]
                
                AlertDialog(
                    onDismissRequest = {
                        showRemoveSanctionDialog = false
                        selectedUserForRemoveSanction = null
                    },
                    icon = {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                    },
                    title = {
                        Text(
                            text = "Eliminar sanció",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Vols eliminar la sanció de l'usuari següent?",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(Modifier.height(4.dp))
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "${userInfo.user.nom} ${userInfo.user.cognom1}${if (userInfo.user.cognom2 != null) " ${userInfo.user.cognom2}" else ""}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (sanction != null) {
                                        Text(
                                            text = "Motiu: ${sanction.reason}",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        if (sanction.expirationDate != null) {
                                            val daysRemaining = sanction.getDaysRemaining()
                                            if (daysRemaining != null) {
                                                Text(
                                                    text = "Expirava en: $daysRemaining dies",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            Text(
                                text = "Aquesta acció no es pot desfer.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val userId = userInfo.user.id ?: 0L
                                SanctionManager.removeSanction(context.applicationContext, userId)
                                // Recarregar sancions immediatament després d'eliminar
                                reloadSanctions()
                                // Tancar diàleg
                                selectedUserForRemoveSanction = null
                                showRemoveSanctionDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Eliminar")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showRemoveSanctionDialog = false
                            selectedUserForRemoveSanction = null
                        }) {
                            Text("Cancel·lar")
                        }
                    }
                )
            }

            // Diàleg de confirmació per eliminar tot l'historial de sancions
            if (showClearAllSanctionsDialog) {
                val allSanctions = SanctionManager.getAllSanctions(context.applicationContext)
                AlertDialog(
                    onDismissRequest = { showClearAllSanctionsDialog = false },
                    icon = {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                    },
                    title = {
                        Text(
                            text = "Eliminar Historial de Sancions",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Vols eliminar tot l'historial de sancions?",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            
                            if (allSanctions.isNotEmpty()) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "Sancions totals: ${allSanctions.size}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        val activeCount = allSanctions.count { it.isActive && !it.isExpired() }
                                        val expiredCount = allSanctions.size - activeCount
                                        Text(
                                            text = "Actives: $activeCount",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        Text(
                                            text = "Expirades: $expiredCount",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "No hi ha sancions emmagatzemades.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            Text(
                                text = " ATENCIÓ: Aquesta acció eliminarà TOTES les sancions (actives i expirades). Aquesta acció no es pot desfer.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                SanctionManager.clearAllSanctions(context.applicationContext)
                                // Recarregar sancions
                                reloadSanctions()
                                showClearAllSanctionsDialog = false
                            },
                            enabled = allSanctions.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                Icons.Default.DeleteSweep,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Eliminar Tot")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showClearAllSanctionsDialog = false }) {
                            Text("Cancel·lar")
                        }
                    }
                )
            }
        }
    }
}

/**
 * Tipus de filtre per als préstecs en retard.
 */
private enum class OverdueFilterType {
    ALL,        // Tots els préstecs en retard (actius i retornats tard)
    ACTIVE,     // Només préstecs actius en retard
    RETURNED    // Només préstecs retornats tard
}

/**
 * Informació d'un usuari amb préstecs en retard o retornats tard.
 */
private data class UserWithOverdueInfo(
    val user: User,
    val activeOverdueLoans: List<Prestec>,
    val returnedLateLoans: List<Prestec>,
    val maxDaysOverdue: Int,
    val maxDaysLateReturn: Int
)

/**
 * Card que mostra informació d'un usuari amb préstecs en retard.
 */
@Composable
private fun UserOverdueCard(
    userInfo: UserWithOverdueInfo,
    filterType: OverdueFilterType,
    sanction: Sanction?,
    onUserClick: () -> Unit,
    onApplySanctionClick: () -> Unit,
    onRemoveSanctionClick: () -> Unit
) {
    val totalOverdue = userInfo.activeOverdueLoans.size + userInfo.returnedLateLoans.size
    val maxDays = maxOf(userInfo.maxDaysOverdue, userInfo.maxDaysLateReturn)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onUserClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                maxDays > 30 -> MaterialTheme.colorScheme.errorContainer
                maxDays > 15 -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.secondaryContainer
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Nom de l'usuari
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${userInfo.user.nom} ${userInfo.user.cognom1}${if (userInfo.user.cognom2 != null) " ${userInfo.user.cognom2}" else ""}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = userInfo.user.email ?: "Sense email",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Badge(
                    containerColor = MaterialTheme.colorScheme.error
                ) {
                    Text(
                        text = "$totalOverdue",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

            // Informació de préstecs actius en retard
            if (userInfo.activeOverdueLoans.isNotEmpty() && 
                (filterType == OverdueFilterType.ALL || filterType == OverdueFilterType.ACTIVE)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "${userInfo.activeOverdueLoans.size} préstec(s) actiu(s) en retard",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = "Màxim: ${userInfo.maxDaysOverdue} dies de retard",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // Informació de préstecs retornats tard
            if (userInfo.returnedLateLoans.isNotEmpty() && 
                (filterType == OverdueFilterType.ALL || filterType == OverdueFilterType.RETURNED)) {
                if (userInfo.activeOverdueLoans.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "${userInfo.returnedLateLoans.size} préstec(s) retornat(s) tard",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = "Màxim: ${userInfo.maxDaysLateReturn} dies de retard",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            // Informació de sanció activa
            if (sanction != null) {
                Spacer(Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Usuari sancionat",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Text(
                            text = "Motiu: ${sanction.reason}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (sanction.expirationDate != null) {
                            val daysRemaining = sanction.getDaysRemaining()
                            if (daysRemaining != null) {
                                Text(
                                    text = "Expira en: $daysRemaining dies",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            } else {
                                Text(
                                    text = "Expira: ${sanction.expirationDate}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        } else {
                            Text(
                                text = "Fins que retorni tots els llibres",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onRemoveSanctionClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Eliminar sanció")
                }
            } else {
                // Botó per aplicar sanció
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onApplySanctionClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Gavel,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Aplicar sanció")
                }
            }
        }
    }
}

/**
 * Calcula els dies restants per retornar un llibre.
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
        calendar.add(Calendar.DAY_OF_YEAR, 30)
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
 * Comprova si un préstec va ser retornat tard.
 * 
 * @param dataPrestec Data del préstec en format "yyyy-MM-dd"
 * @param dataDevolucio Data de devolució en format "yyyy-MM-dd"
 * @return true si el préstec va ser retornat tard
 */
private fun wasReturnedLate(dataPrestec: String?, dataDevolucio: String?): Boolean {
    if (dataPrestec == null || dataDevolucio == null) return false
    
    return try {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val prestecDate = dateFormat.parse(dataPrestec) ?: return false
        val devolucioDate = dateFormat.parse(dataDevolucio) ?: return false
        
        // Calcular la data límit (30 dies després del préstec)
        val calendar = Calendar.getInstance()
        calendar.time = prestecDate
        calendar.add(Calendar.DAY_OF_YEAR, 30)
        val limitDate = calendar.time
        
        // Si la data de devolució és posterior a la data límit, va ser retornat tard
        devolucioDate.after(limitDate)
    } catch (e: Exception) {
        false
    }
}

/**
 * Calcula quants dies de retard va tenir un préstec quan es va retornar.
 * 
 * @param dataPrestec Data del préstec en format "yyyy-MM-dd"
 * @param dataDevolucio Data de devolució en format "yyyy-MM-dd"
 * @return Nombre de dies de retard (0 si no va ser tard)
 */
private fun calculateDaysLateReturn(dataPrestec: String?, dataDevolucio: String?): Int {
    if (dataPrestec == null || dataDevolucio == null) return 0
    
    return try {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val prestecDate = dateFormat.parse(dataPrestec) ?: return 0
        val devolucioDate = dateFormat.parse(dataDevolucio) ?: return 0
        
        // Calcular la data límit (30 dies després del préstec)
        val calendar = Calendar.getInstance()
        calendar.time = prestecDate
        calendar.add(Calendar.DAY_OF_YEAR, 30)
        val limitDate = calendar.time
        
        // Si la data de devolució és anterior o igual a la data límit, no va ser tard
        if (!devolucioDate.after(limitDate)) return 0
        
        // Calcular diferència en dies
        val diffInMillis = devolucioDate.time - limitDate.time
        val diffInDays = (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
        
        diffInDays
    } catch (e: Exception) {
        0
    }
}

/**
 * Diàleg per aplicar una sanció a un usuari.
 */
@Composable
private fun SanctionDialog(
    userInfo: UserWithOverdueInfo,
    onDismiss: () -> Unit,
    onApply: (reason: String, durationDays: Int?) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    var durationType by remember { mutableStateOf<DurationType>(DurationType.DAYS) }
    var durationValue by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Aplicar sanció",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Usuari: ${userInfo.user.nom} ${userInfo.user.cognom1}${if (userInfo.user.cognom2 != null) " ${userInfo.user.cognom2}" else ""}",
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Motiu de la sanció") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 3,
                    isError = showError && reason.isBlank()
                )

                // Tipus de durada
                Text(
                    text = "Durada de la sanció:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = durationType == DurationType.DAYS,
                        onClick = { durationType = DurationType.DAYS },
                        label = { Text("Dies") }
                    )
                    FilterChip(
                        selected = durationType == DurationType.UNTIL_RETURN,
                        onClick = { durationType = DurationType.UNTIL_RETURN },
                        label = { Text("Fins retorn") }
                    )
                }

                if (durationType == DurationType.DAYS) {
                    OutlinedTextField(
                        value = durationValue,
                        onValueChange = { 
                            if (it.all { char -> char.isDigit() }) {
                                durationValue = it
                            }
                        },
                        label = { Text("Nombre de dies") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = showError && durationValue.isBlank(),
                        supportingText = if (showError && durationValue.isBlank()) {
                            { Text("Introdueix un nombre de dies") }
                        } else null
                    )
                } else {
                    Text(
                        text = "La sanció durarà fins que l'usuari retorni tots els llibres en retard.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (showError) {
                    Text(
                        text = "Completa tots els camps",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (reason.isBlank() || (durationType == DurationType.DAYS && durationValue.isBlank())) {
                        showError = true
                    } else {
                        val durationDays = if (durationType == DurationType.DAYS) {
                            durationValue.toIntOrNull()
                        } else {
                            null
                        }
                        onApply(reason, durationDays)
                    }
                }
            ) {
                Text("Aplicar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel·lar")
            }
        }
    )
}

/**
 * Tipus de durada de la sanció.
 */
private enum class DurationType {
    DAYS,           // Durada en dies
    UNTIL_RETURN    // Fins que retorni tots els llibres
}
