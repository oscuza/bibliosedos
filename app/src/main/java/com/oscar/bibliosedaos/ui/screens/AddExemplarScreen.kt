package com.oscar.bibliosedaos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.oscar.bibliosedaos.ui.viewmodels.BookViewModel
import kotlinx.coroutines.launch

/**
 * Pantalla per afegir un nou exemplar d'un llibre.
 *
 * **Descripció:**
 * Formulari per crear nous exemplars físics de llibres existents.
 * Cada exemplar representa una còpia física d'un llibre amb la seva
 * pròpia ubicació a la biblioteca.
 *
 * **Camps del Formulari:**
 * - Llibre: Selecció del llibre al qual pertany l'exemplar (obligatori)
 * - Ubicació: Lloc físic on es troba l'exemplar (obligatori)
 *
 * **Funcionalitats:**
 * - Llista desplegable amb tots els llibres disponibles
 * - Mostra informació detallada del llibre seleccionat
 * - Validació en temps real dels camps
 * - Creació automàtica amb estat "lliure"
 *
 * **Permisos:**
 * - ⚠️ Només accessible per administradors (rol=2)
 * - 🔑 Requereix token JWT vàlid
 *
 * @param navController Controlador de navegació
 * @param bookViewModel ViewModel per gestió de llibres
 *
 * @author Oscar
 * @since 1.0
 * @see BookViewModel.addExemplar
 * @see BookManagementScreen
 * @see Exemplar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExemplarScreen(
    navController: NavController,
    bookViewModel: BookViewModel = viewModel()
) {
    // ========== ESTATS DEL FORMULARI ==========

    var selectedLlibreId by remember { mutableStateOf<Long?>(null) }
    var lloc by remember { mutableStateOf("") }

    // Estats de validació
    var llibreError by remember { mutableStateOf<String?>(null) }
    var llocError by remember { mutableStateOf<String?>(null) }

    // Estats observables
    val llibresState by bookViewModel.llibresState.collectAsState()
    val formState by bookViewModel.exemplarFormState.collectAsState()

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Llibre seleccionat
    val selectedLlibre = llibresState.llibres.find { it.id == selectedLlibreId }

    // ========== CÀRREGA DE LLIBRES ==========

    LaunchedEffect(Unit) {
        bookViewModel.loadLlibres()
    }

    // ========== GESTIÓ DE RESPOSTES ==========

    LaunchedEffect(formState.success) {
        if (formState.success) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = formState.successMessage ?: "Exemplar creat correctament",
                    duration = SnackbarDuration.Short
                )
            }
            bookViewModel.resetForms()
            navController.navigateUp()
        }
    }

    LaunchedEffect(formState.error) {
        formState.error?.let { error ->
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = error,
                    duration = SnackbarDuration.Long,
                    actionLabel = "Tancar"
                )
            }
        }
    }

    // ========== FUNCIONS DE VALIDACIÓ ==========

    fun validateLlibre(): Boolean {
        llibreError = if (selectedLlibreId == null) {
            "Has de seleccionar un llibre"
        } else {
            null
        }
        return llibreError == null
    }

    fun validateLloc(): Boolean {
        llocError = when {
            lloc.isBlank() -> "La ubicació és obligatòria"
            lloc.length < 2 -> "La ubicació ha de tenir almenys 2 caràcters"
            lloc.length > 50 -> "La ubicació no pot superar 50 caràcters"
            else -> null
        }
        return llocError == null
    }

    fun validateAndSubmit() {
        val isLlibreValid = validateLlibre()
        val isLlocValid = validateLloc()

        if (isLlibreValid && isLlocValid && selectedLlibreId != null) {
            bookViewModel.addExemplar(
                lloc = lloc.trim(),
                llibreId = selectedLlibreId!!
            )
        }
    }

    // ========== UI ==========

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "📖 Afegir Nou Exemplar",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Tornar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ========== SELECCIÓ DE LLIBRE ==========

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Selecciona el Llibre",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Dropdown de llibres
                    var expanded by remember { mutableStateOf(false) }

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = selectedLlibre?.titol ?: "",
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Llibre *") },
                            placeholder = { Text("Selecciona un llibre") },
                            leadingIcon = {
                                Icon(Icons.Default.Book, contentDescription = null)
                            },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            },
                            isError = llibreError != null,
                            supportingText = {
                                llibreError?.let { Text(it) }
                            },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            if (llibresState.isLoading) {
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text("Carregant llibres...")
                                        }
                                    },
                                    onClick = { }
                                )
                            } else if (llibresState.llibres.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("No hi ha llibres disponibles") },
                                    onClick = { }
                                )
                            } else {
                                llibresState.llibres.forEach { llibre ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    llibre.titol,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                llibre.autor?.let { autor ->
                                                    Text(
                                                        autor.nom,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            selectedLlibreId = llibre.id
                                            llibreError = null
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Mostra informació del llibre seleccionat
                    selectedLlibre?.let { llibre ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        "Informació del Llibre",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }

                                HorizontalDivider(
                                    Modifier, DividerDefaults.Thickness, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f)
                                )

                                Column(
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    InfoRow("ISBN", llibre.isbn)
                                    InfoRow("Títol", llibre.titol)
                                    llibre.autor?.let {
                                        InfoRow("Autor", it.nom)
                                    }
                                    InfoRow("Editorial", llibre.editorial)
                                    InfoRow("Pàgines", llibre.pagines.toString())
                                }
                            }
                        }
                    }
                }
            }

            // ========== UBICACIÓ DE L'EXEMPLAR ==========

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Ubicació de l'Exemplar",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        "Indica on estarà situat aquest exemplar a la biblioteca",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = lloc,
                        onValueChange = {
                            lloc = it
                            if (llocError != null) validateLloc()
                        },
                        label = { Text("Ubicació *") },
                        placeholder = { Text("Ex: Prestatgeria A3, Secció 2") },
                        leadingIcon = {
                            Icon(Icons.Default.LocationOn, contentDescription = null)
                        },
                        isError = llocError != null,
                        supportingText = {
                            llocError?.let {
                                Text(it)
                            } ?: Text("${lloc.length}/50 caràcters")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Exemples d'ubicacions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(
                            onClick = { lloc = "Prestatgeria A1" },
                            label = { Text("Prestatgeria A1") }
                        )
                        AssistChip(
                            onClick = { lloc = "Secció Infantil" },
                            label = { Text("Secció Infantil") }
                        )
                        AssistChip(
                            onClick = { lloc = "Magatzem" },
                            label = { Text("Magatzem") }
                        )
                    }
                }
            }

            // ========== INFORMACIÓ ADDICIONAL ==========

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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Column {
                        Text(
                            "Estat Inicial",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            "L'exemplar es crearà amb estat 'lliure' i estarà disponible immediatament per préstec",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            // ========== INFO OBLIGATORIETAT ==========

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        "Els camps marcats amb * són obligatoris",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ========== BOTONS D'ACCIÓ ==========

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { navController.navigateUp() },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Cancel·lar")
                }

                Button(
                    onClick = { validateAndSubmit() },
                    modifier = Modifier.weight(1f),
                    enabled = !formState.isSubmitting
                ) {
                    if (formState.isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Crear Exemplar")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Component per mostrar una fila d'informació.
 *
 * @param label Etiqueta del camp
 * @param value Valor del camp
 */
@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

