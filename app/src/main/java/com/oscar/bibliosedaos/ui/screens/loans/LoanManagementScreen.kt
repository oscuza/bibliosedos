package com.oscar.bibliosedaos.ui.screens.loans

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.oscar.bibliosedaos.navigation.AppScreens
import com.oscar.bibliosedaos.ui.viewmodels.AuthViewModel
import com.oscar.bibliosedaos.ui.viewmodels.LoanViewModel
import kotlinx.coroutines.launch

/**
 * Pantalla principal de gestió de préstecs per administradors.
 *
 * **Descripció:**
 * Pantalla dedicada a la gestió centralitzada de préstecs que proporciona
 * accés ràpid a totes les funcionalitats relacionades amb préstecs.
 *
 * **Funcionalitats:**
 * - Veure usuaris amb préstecs actius
 * - Gestionar préstecs en retard
 * - Accés centralitzat a totes les operacions de préstecs
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
 * @see UsersWithLoansScreen
 * @see OverdueLoansScreen
 * @see BookManagementScreen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanManagementScreen(
    navController: NavController,
    loanViewModel: LoanViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // ========== UI PRINCIPAL ==========

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Gestió de Préstecs",
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Tornar")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ========== TÍTOL ==========
            item {
                Text(
                    text = "Opcions de Gestió",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // ========== CARD: USUARIS AMB PRÉSTECS ACTIUS ==========
            item {
                ManagementOptionCard(
                    title = "Usuaris amb Préstecs Actius",
                    subtitle = "Veure tots els usuaris amb llibres prestats",
                    icon = Icons.Default.People,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    onClick = {
                        navController.navigate(AppScreens.UsersWithLoansScreen.route)
                    }
                )
            }

            // ========== CARD: PRÉSTECS EN RETARD ==========
            item {
                ManagementOptionCard(
                    title = "Préstecs en Retard",
                    subtitle = "Gestionar préstecs en retard i sancions",
                    icon = Icons.Default.Warning,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    onClick = {
                        navController.navigate(AppScreens.OverdueLoansScreen.route)
                    }
                )
            }

            // ========== INFORMACIÓ ADICIONAL ==========
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Informació",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = "Des d'aquesta pantalla pots gestionar totes les operacions relacionades amb préstecs de llibres.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Card reutilitzable per mostrar una opció de gestió.
 *
 * @param title Títol de l'opció
 * @param subtitle Subtítol o descripció de l'opció
 * @param icon Icona a mostrar
 * @param containerColor Color del contenidor
 * @param contentColor Color del contingut
 * @param onClick Callback quan es fa clic a la card
 */
@Composable
private fun ManagementOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.2f),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(40.dp)
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor.copy(alpha = 0.8f)
                    )
                }
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Anar a $title",
                tint = contentColor,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}





