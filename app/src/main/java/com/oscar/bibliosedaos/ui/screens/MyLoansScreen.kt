package com.oscar.bibliosedaos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
 * - Botó per retornar llibre (usuari propietari O administrador)
 * - Actualització automàtica després de retornar un llibre
 * - Indicador de càrrega durant les operacions
 * - Gestió d'errors amb missatges informatius
 * - **NOU:** L'administrador pot retornar manualment els llibres de qualsevol usuari
 *
 * **Permisos:**
 * - 👥 Usuari normal: veu només els seus préstecs i pot retornar-los
 * - 👨‍💼 Administrador: pot veure préstecs de qualsevol usuari I retornar-los manualment
 *
 * **Paràmetres:**
 * @param navController Controlador de navegació per gestionar la navegació entre pantalles
 * @param loanViewModel ViewModel que gestiona la lògica de negoci dels préstecs
 * @param authViewModel ViewModel que gestiona l'autenticació i informació de l'usuari
 * @param userId (Opcional) ID de l'usuari dels quals es volen veure els préstecs.
 *               Si és null, mostra els préstecs de l'usuari autenticat.
 *
 * **Notes d'implementació:**
 * - Aquesta funció ha estat desenvolupada amb assistència d'IA (Claude - Anthropic)
 * - La implementació segueix les millors pràctiques de Jetpack Compose
 * - S'utilitza LaunchedEffect per carregar les dades quan canvia l'userId
 * - Els estats de càrrega i error es gestionen amb snackbar
 * - Modificació: Afegida funcionalitat per a que l'admin pugui retornar préstecs
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

    // ==================== EFECTES ====================

    // Carregar préstecs quan canvia l'usuari
    LaunchedEffect(targetUserId) {
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
            // Recarregar la llista després de retornar
            loanViewModel.refreshActiveLoans(targetUserId)
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
                                canReturn = isViewingOwnLoans || isAdmin, // MODIFICACIÓ: Admin també pot retornar
                                isReturning = returnLoanState.isReturning == loan.id,
                                isAdminReturning = !isViewingOwnLoans && isAdmin,
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
 * **Modificació:** Afegit paràmetre isAdminReturning per mostrar text diferent
 * quan l'admin retorna un llibre d'un altre usuari.
 */
@Composable
private fun LoanCard(
    loan: com.oscar.bibliosedaos.data.models.Prestec,
    canReturn: Boolean,
    isReturning: Boolean,
    isAdminReturning: Boolean = false,
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

            // Botón de retorno (solo si puede retornar)
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
                                Icons.Default.AssignmentReturn
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
                        Icons.Default.AssignmentReturn
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