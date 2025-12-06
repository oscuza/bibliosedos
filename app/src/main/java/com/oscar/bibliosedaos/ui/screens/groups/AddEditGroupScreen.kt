package com.oscar.bibliosedaos.ui.screens.groups

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.oscar.bibliosedaos.data.network.ApiClient
import com.oscar.bibliosedaos.ui.viewmodels.AuthViewModel
import com.oscar.bibliosedaos.ui.viewmodels.GroupViewModel
import kotlinx.coroutines.launch

/**
 * Pantalla per crear o editar un grup de lectura.
 *
 * **Descripció:**
 * Formulari per crear nous grups de lectura o editar grups existents.
 * Permet seleccionar un horari disponible i assignar un administrador.
 *
 * **Camps del Formulari:**
 * - Nom: Nom del grup (obligatori)
 * - Temàtica: Temàtica o gènere literari (obligatori)
 * - Horari: Selecció d'horari disponible (obligatori)
 *
 * **Permisos:**
 * - 👥 Qualsevol usuari autenticat pot crear un grup
 * -  Requereix token JWT vàlid
 * -  Només l'administrador del grup o un admin del sistema pot editar/eliminar
 *
 * @param navController Controlador de navegació
 * @param grupId ID del grup a editar (null si és crear nou)
 * @param groupViewModel ViewModel per gestió de grups
 *
 * @author Oscar
 * @since 1.0
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditGroupScreen(
    navController: NavController,
    grupId: Long?,
    groupViewModel: GroupViewModel,
    authViewModel: AuthViewModel
) {
    val isEditing = grupId != null
    val selectedGrup by groupViewModel.selectedGrupState.collectAsState()
    val grupsState by groupViewModel.grupsState.collectAsState()
    val horarisState by groupViewModel.horarisState.collectAsState()
    val loginState by authViewModel.loginUiState.collectAsState()
    val currentUserId = loginState.authResponse?.id

    // Estats del formulari
    var nom by remember { mutableStateOf("") }
    var tematica by remember { mutableStateOf("") }
    var selectedHorariId by remember { mutableStateOf<Long?>(null) }

    // Estats de validació
    var nomError by remember { mutableStateOf<String?>(null) }
    var tematicaError by remember { mutableStateOf<String?>(null) }
    var horariError by remember { mutableStateOf<String?>(null) }

    // Estat per al dropdown d'horaris
    var horariExpanded by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var hasSubmitted by remember { mutableStateOf(false) }

    // Carregar dades inicials
    LaunchedEffect(Unit) {
        // Carregar tots els horaris (lliures i ocupats) per mostrar-los tots
        groupViewModel.loadHoraris()
        // Nota: La edició de grups no està disponible al backend
        // Si s'intenta editar, es redirigeix a crear un grup nou
        if (isEditing && grupId != null) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = "L'edició de grups no està disponible. Pots crear un grup nou.",
                    duration = SnackbarDuration.Long
                )
                navController.popBackStack()
            }
        }
    }

    // Gestió de respostes
    LaunchedEffect(grupsState.isCreating, grupsState.error) {
        if (hasSubmitted && !isEditing) {
            val isProcessing = grupsState.isCreating

            if (!isProcessing) {
                if (grupsState.error == null) {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = "Grup creat correctament",
                            duration = SnackbarDuration.Short
                        )
                    }
                    navController.popBackStack()
                } else {
                    hasSubmitted = false
                }
            }
        }
    }

    LaunchedEffect(grupsState.error) {
        grupsState.error?.let { error ->
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = error,
                    duration = SnackbarDuration.Long,
                    actionLabel = "Tancar"
                )
            }
        }
    }

    // Funcions de validació
    fun validateNom(): Boolean {
        nomError = when {
            nom.isBlank() -> "El nom és obligatori"
            nom.length < 3 -> "El nom ha de tenir almenys 3 caràcters"
            else -> null
        }
        return nomError == null
    }

    fun validateTematica(): Boolean {
        tematicaError = when {
            tematica.isBlank() -> "La temàtica és obligatòria"
            tematica.length < 3 -> "La temàtica ha de tenir almenys 3 caràcters"
            else -> null
        }
        return tematicaError == null
    }

    fun validateHorari(): Boolean {
        horariError = when {
            selectedHorariId == null -> "Has de seleccionar un horari"
            !isEditing -> {
                // Al crear un grup nou, només es poden seleccionar horaris lliures
                val selectedHorari = horarisState.horaris.find { it.id == selectedHorariId }
                if (selectedHorari == null || !selectedHorari.isLliure) {
                    "Has de seleccionar un horari disponible (lliure)"
                } else {
                    null
                }
            }
            else -> null // En edició, es pot seleccionar qualsevol horari
        }
        return horariError == null
    }

    fun validateAndSubmit() {
        // La edició no està disponible al backend
        if (isEditing) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = "L'edició de grups no està disponible al backend",
                    duration = SnackbarDuration.Long
                )
            }
            return
        }

        val isNomValid = validateNom()
        val isTematicaValid = validateTematica()
        val isHorariValid = validateHorari()

        val horariId = selectedHorariId
        val userId = currentUserId
        
        if (isNomValid && isTematicaValid && isHorariValid && userId != null && horariId != null) {
            hasSubmitted = true
            // Per crear un grup, necessitem els objectes User i Horari complets
            scope.launch {
                try {
                    // Obtenir l'usuari complet de l'API
                    val administrador = ApiClient.instance.getUserById(userId)
                    
                    // Obtenir l'horari complet de la llista carregada
                    val horari = horarisState.horaris.find { it.id == horariId }
                    
                    if (horari != null && administrador != null) {
                        // Crear el grup amb els objectes complets
                        groupViewModel.createGrup(
                            nom = nom.trim(),
                            tematica = tematica.trim(),
                            administrador = administrador,
                            horari = horari
                        )
                    } else {
                        snackbarHostState.showSnackbar(
                            message = "Error: No s'han pogut obtenir les dades necessàries",
                            duration = SnackbarDuration.Short
                        )
                        hasSubmitted = false
                    }
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar(
                        message = "Error obtenint dades: ${e.message}",
                        duration = SnackbarDuration.Long
                    )
                    hasSubmitted = false
                }
            }
        }
    }

    // Separar horaris lliures i ocupats
    val horarisLliures = horarisState.horaris.filter { it.isLliure }
    val horarisOcupats = horarisState.horaris.filter { !it.isLliure }
    val selectedHorari = horarisState.horaris.find { it.id == selectedHorariId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nou Grup de Lectura") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Enrere")
                    }
                }
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
            // Nom del grup
            OutlinedTextField(
                value = nom,
                onValueChange = {
                    nom = it
                    if (nomError != null) validateNom()
                },
                label = { Text("Nom del grup *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = nomError != null,
                supportingText = nomError?.let { { Text(it) } }
            )

            // Temàtica
            OutlinedTextField(
                value = tematica,
                onValueChange = {
                    tematica = it
                    if (tematicaError != null) validateTematica()
                },
                label = { Text("Temàtica *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = tematicaError != null,
                supportingText = tematicaError?.let { { Text(it) } },
                placeholder = { Text("Ex: Ciència-ficció, Història, Poesia...") }
            )

            // Horari - Selector desplegable
            ExposedDropdownMenuBox(
                expanded = horariExpanded,
                onExpandedChange = { horariExpanded = !horariExpanded }
            ) {
                OutlinedTextField(
                    value = selectedHorari?.displayText ?: "",
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Horari *") },
                    placeholder = { Text("Selecciona un horari") },
                    leadingIcon = {
                        Icon(Icons.Default.Schedule, contentDescription = null)
                    },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = horariExpanded)
                    },
                    isError = horariError != null,
                    supportingText = horariError?.let { { Text(it) } },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = horariExpanded,
                    onDismissRequest = { horariExpanded = false },
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    // Horaris lliures
                    if (horarisLliures.isNotEmpty()) {
                        Text(
                            text = "Horaris Disponibles",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        horarisLliures.forEach { horari ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text = horari.displayText,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Text(
                                            text = "Disponible",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                },
                                onClick = {
                                    selectedHorariId = horari.id
                                    horariExpanded = false
                                    if (horariError != null) validateHorari()
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            )
                        }
                    }

                    // Horaris ocupats
                    if (horarisOcupats.isNotEmpty()) {
                        if (horarisLliures.isNotEmpty()) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        }
                        Text(
                            text = "Horaris Ocupats",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        horarisOcupats.forEach { horari ->
                            val isSelected = horari.id == selectedHorariId
                            val isEnabled = isEditing || isSelected

                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text = horari.displayText,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = if (isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                        Text(
                                            text = "Ocupat",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                },
                                onClick = {
                                    if (isEnabled) {
                                        selectedHorariId = horari.id
                                        horariExpanded = false
                                        if (horariError != null) validateHorari()
                                    }
                                },
                                enabled = isEnabled,
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Block,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            )
                        }
                    }

                    if (horarisLliures.isEmpty() && horarisOcupats.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No hi ha horaris disponibles") },
                            onClick = { horariExpanded = false },
                            enabled = false
                        )
                    }
                }
            }

            // Informació
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Informació",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Pots veure tots els horaris (disponibles i ocupats). " +
                            "Només pots seleccionar horaris disponibles per crear un grup nou.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Botó de guardar
            val isProcessing = grupsState.isCreating

            Button(
                onClick = { validateAndSubmit() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isProcessing && !isEditing
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text("Crear Grup")
            }
        }
    }
}

