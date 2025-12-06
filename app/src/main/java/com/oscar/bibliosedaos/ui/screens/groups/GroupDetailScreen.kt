package com.oscar.bibliosedaos.ui.screens.groups

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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.oscar.bibliosedaos.data.models.Grup
import com.oscar.bibliosedaos.data.network.User
import com.oscar.bibliosedaos.navigation.AppScreens
import com.oscar.bibliosedaos.ui.viewmodels.AuthViewModel
import com.oscar.bibliosedaos.ui.viewmodels.GroupViewModel

/**
 * Pantalla de detall d'un grup de lectura.
 *
 * **Descripció:**
 * Mostra la informació completa d'un grup de lectura, incloent
 * llista de membres, horari, administrador i permet gestionar membres.
 *
 * **Funcionalitats:**
 * - Informació completa del grup
 * - Llista de membres
 * - Informació de l'administrador
 * - Horari assignat
 * - Afegir/eliminar membres (segons permisos)
 * - Editar grup (només administrador del grup o admin del sistema)
 * - Eliminar grup (només administrador del grup o admin del sistema)
 *
 * @param navController Controlador de navegació
 * @param grupId ID del grup a mostrar
 * @param groupViewModel ViewModel per gestionar grups
 * @param authViewModel ViewModel per informació de l'usuari
 *
 * @author Oscar
 * @since 1.0
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    navController: NavController,
    grupId: Long,
    groupViewModel: GroupViewModel,
    authViewModel: AuthViewModel,
) {
    val selectedGrup by groupViewModel.selectedGrupState.collectAsState()
    val grupsState by groupViewModel.grupsState.collectAsState()
    val loginState by authViewModel.loginUiState.collectAsState()
    val currentUserId = loginState.authResponse?.id
    val isAdmin = authViewModel.isAdmin()

    // Carregar el grup si no està carregat
    LaunchedEffect(grupId) {
        if (selectedGrup?.id != grupId) {
            groupViewModel.loadGrupById(grupId)
        }
    }

    val grup = selectedGrup ?: grupsState.grups.find { it.id == grupId }
    val canEdit = grup != null && (isAdmin || grup.isAdmin(currentUserId ?: -1))
    val isMember = grup != null && grup.isMember(currentUserId ?: -1)

    // Estats per al diàleg de confirmació d'eliminació
    var showDeleteDialog by remember { mutableStateOf(false) }
    var grupToDelete by remember { mutableStateOf<Long?>(null) }

    // Tancar diàleg i navegar després d'eliminar amb èxit
    LaunchedEffect(grupsState.isDeleting, grupsState.error) {
        if (grupsState.isDeleting == null && grupToDelete != null && showDeleteDialog) {
            // L'eliminació ha acabat (amb èxit o error)
            showDeleteDialog = false
            val wasSuccessful = grupsState.error == null
            grupToDelete = null
            // Només navegar si l'eliminació va ser exitosa
            if (wasSuccessful) {
                navController.popBackStack()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(grup?.nom ?: "Grup de Lectura") }, navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Enrere")
                }
            }, actions = {
                if (canEdit) {
                    // Nota: L'edició de grups no està disponible al backend
                    // Només es permet eliminar grups
                    IconButton(
                        onClick = {
                            grupToDelete = grupId
                            showDeleteDialog = true
                        }) {
                        Icon(Icons.Default.Delete, "Eliminar")
                    }
                }
            })
        }) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val errorMessage = grupsState.error
            when {
                grupsState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                errorMessage != null -> {
                    ErrorMessage(
                        message = errorMessage,
                        onRetry = { groupViewModel.loadGrupById(grupId) },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                grup == null -> {
                    Text(
                        text = "Grup no trobat", modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Informació del grup
                        item {
                            GrupInfoSection(grup = grup)
                        }

                        // Horari
                        item {
                            if (grup.horari != null) {
                                HorariSection(horari = grup.horari)
                            }
                        }

                        // Administrador
                        item {
                            if (grup.administrador != null) {
                                AdminSection(admin = grup.administrador)
                            }
                        }

                        // Membres
                        item {
                            Text(
                                text = "Membres (${grup.totalMembres})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Llista de membres
                        if (grup.membres.isNullOrEmpty()) {
                            item {
                                Text(
                                    text = "Encara no hi ha membres en aquest grup",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            items(grup.membres) { membre ->
                                // El backend només permet eliminar membres si:
                                // 1. L'usuari és ADMIN del sistema, O
                                // 2. L'usuari és el propi membre eliminant-se a si mateix
                                val canRemoveMember = isAdmin || (membre.id == currentUserId)

                                MemberCard(
                                    user = membre, canRemove = canRemoveMember, onRemove = {
                                        groupViewModel.removeMemberFromGrup(
                                            grup.id!!, membre.id
                                        )
                                    })
                            }
                        }

                        // Botó per unir-se al grup
                        if (!isMember && currentUserId != null) {
                            item {
                                Button(
                                    onClick = {
                                        groupViewModel.addMemberToGrup(
                                            grup.id!!, currentUserId
                                        )
                                    }, modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.PersonAdd, "Unir-se")
                                    Spacer(Modifier.width(8.dp))
                                    Text("Unir-se al grup")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ========== DIÀLEG DE CONFIRMACIÓ D'ELIMINACIÓ ==========
    if (showDeleteDialog && grupToDelete != null) {
        val grupToDeleteObj = grupsState.grups.find { it.id == grupToDelete }
        AlertDialog(onDismissRequest = {
            if (grupsState.isDeleting == null) {
                showDeleteDialog = false
                grupToDelete = null
            }
        }, icon = {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
        }, title = {
            Text(
                " Confirmar Eliminació",
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
        }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Estàs segur que vols eliminar aquest grup de lectura?")
                grupToDeleteObj?.let {
                    Card(
                        modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Grup a eliminar:",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                text = it.nom,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Temàtica: ${it.tematica}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            it.horari?.let { horari ->
                                Text(
                                    text = "Horari: ${horari.displayText}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
                Text(
                    text = "Aquesta acció no es pot desfer. Tots els membres del grup perdran l'accés.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }, confirmButton = {
            Button(
                onClick = {
                    grupToDelete?.let { id ->
                        groupViewModel.deleteGrup(id)
                    }
                },
                enabled = grupsState.isDeleting == null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                if (grupsState.isDeleting == grupToDelete) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onError
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text("Eliminar")
            }
        }, dismissButton = {
            TextButton(
                onClick = {
                    if (grupsState.isDeleting == null) {
                        showDeleteDialog = false
                        grupToDelete = null
                    }
                }, enabled = grupsState.isDeleting == null
            ) {
                Text("Cancel·lar")
            }
        })
    }
}

/**
 * Secció d'informació del grup.
 */
@Composable
fun GrupInfoSection(grup: Grup) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = grup.nom,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Category,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = grup.tematica, style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

/**
 * Secció d'horari.
 */
@Composable
fun HorariSection(horari: com.oscar.bibliosedaos.data.models.Horari) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Horari",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = horari.displayText, style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

/**
 * Secció d'administrador.
 */
@Composable
fun AdminSection(admin: User) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Administrador",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = admin.nick ?: admin.nom, style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

/**
 * Card d'un membre del grup.
 */
@Composable
fun MemberCard(
    user: User,
    canRemove: Boolean,
    onRemove: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = user.nick ?: user.nom,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    if (user.nom != null && user.nick != user.nom) {
                        Text(
                            text = user.nom,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (canRemove) {
                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Default.RemoveCircle,
                        "Eliminar membre",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

