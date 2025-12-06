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
import com.oscar.bibliosedaos.data.models.Horari
import com.oscar.bibliosedaos.ui.viewmodels.GroupViewModel

/**
 * Pantalla per veure els horaris disponibles per a grups de lectura.
 *
 * **Descripció:**
 * Mostra tots els horaris de les sales de la biblioteca per als grups de lectura,
 * diferenciant visualment els horaris disponibles dels ocupats.
 *
 * **Funcionalitats:**
 * - Llistat de tots els horaris
 * - Separació visual entre horaris disponibles i ocupats
 * - Informació: sala, dia, hora, estat
 *
 * **Permisos:**
 * - 👥 Accessible per tots els usuaris autenticats
 * -  Requereix token JWT vàlid
 *
 * @param navController Controlador de navegació
 * @param groupViewModel ViewModel per gestionar horaris
 *
 * @author Oscar
 * @since 1.0
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HorarisScreen(
    navController: NavController,
    groupViewModel: GroupViewModel
) {
    val horarisState by groupViewModel.horarisState.collectAsState()

    // Carregar horaris al iniciar
    LaunchedEffect(Unit) {
        groupViewModel.loadHoraris()
    }

    // Separar horaris en disponibles i ocupats
    val horarisLliures = horarisState.horaris.filter { it.isLliure }
    val horarisOcupats = horarisState.horaris.filter { !it.isLliure }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Grups de Lectura") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Enrere")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val errorMessage = horarisState.error
            when {
                horarisState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                errorMessage != null -> {
                    ErrorMessageHoraris(
                        message = errorMessage,
                        onRetry = { groupViewModel.loadHoraris() },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                horarisState.horaris.isEmpty() -> {
                    EmptyStateHoraris(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Horaris disponibles
                        if (horarisLliures.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Horaris Disponibles",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            items(horarisLliures) { horari ->
                                HorariCard(
                                    horari = horari,
                                    isAvailable = true
                                )
                            }
                        }

                        // Horaris ocupats
                        if (horarisOcupats.isNotEmpty()) {
                            item {
                                if (horarisLliures.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                                Text(
                                    text = "Horaris Ocupats",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            items(horarisOcupats) { horari ->
                                HorariCard(
                                    horari = horari,
                                    isAvailable = false
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Card que mostra la informació d'un horari.
 */
@Composable
fun HorariCard(
    horari: Horari,
    isAvailable: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isAvailable) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Sala
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Room,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (isAvailable) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                    Text(
                        text = horari.sala,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isAvailable) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        }
                    )
                }

                // Dia i hora
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isAvailable) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                    Text(
                        text = "${horari.dia} - ${horari.hora}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isAvailable) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onErrorContainer
                        }
                    )
                }
            }

            // Icona d'estat
            Surface(
                shape = MaterialTheme.shapes.small,
                color = if (isAvailable) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                }
            ) {
                Icon(
                    imageVector = if (isAvailable) {
                        Icons.Default.CheckCircle
                    } else {
                        Icons.Default.Block
                    },
                    contentDescription = if (isAvailable) "Disponible" else "Ocupat",
                    modifier = Modifier
                        .padding(8.dp)
                        .size(24.dp),
                    tint = if (isAvailable) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onError
                    }
                )
            }
        }
    }
}

/**
 * Component que mostra un missatge d'error amb opció de reintentar.
 */
@Composable
fun ErrorMessageHoraris(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )
        Button(onClick = onRetry) {
            Text("Tornar a intentar")
        }
    }
}

/**
 * Component que mostra l'estat buit quan no hi ha horaris.
 */
@Composable
fun EmptyStateHoraris(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            Icons.Default.Schedule,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "No hi ha horaris disponibles",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Contacta amb l'administrador per afegir horaris",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

