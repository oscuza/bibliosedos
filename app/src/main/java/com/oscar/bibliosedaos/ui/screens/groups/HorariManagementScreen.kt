package com.oscar.bibliosedaos.ui.screens.groups

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.oscar.bibliosedaos.data.models.Horari
import com.oscar.bibliosedaos.ui.viewmodels.GroupViewModel
import kotlinx.coroutines.launch

/**
 * Pantalla de gestió d'horaris per administradors.
 *
 * **Descripció:**
 * Permet als administradors crear i visualitzar horaris de sales per als grups de lectura.
 * L'administrador és l'encarregat de crear els possibles horaris per les sales creades.
 *
 * **Funcionalitats:**
 * - Llistat de tots els horaris (disponibles i ocupats)
 * - Crear nous horaris amb validació
 * - Separació visual entre horaris disponibles i ocupats
 * - Informació: sala, dia, hora, estat
 *
 * **Permisos:**
 * -  Només accessible per administradors (rol=2)
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
fun HorariManagementScreen(
    navController: NavController,
    groupViewModel: GroupViewModel
) {
    val horarisState by groupViewModel.horarisState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Estats per al diàleg de crear horari
    var showAddHorariDialog by remember { mutableStateOf(false) }
    var sala by remember { mutableStateOf("") }
    var dia by remember { mutableStateOf("") }
    var hora by remember { mutableStateOf("") }

    // Validacions
    var salaError by remember { mutableStateOf<String?>(null) }
    var diaError by remember { mutableStateOf<String?>(null) }
    var horaError by remember { mutableStateOf<String?>(null) }

    // Estats per al diàleg d'eliminació
    var showDeleteDialog by remember { mutableStateOf(false) }
    var horariToDelete by remember { mutableStateOf<Long?>(null) }

    // Carregar horaris al iniciar
    LaunchedEffect(Unit) {
        groupViewModel.loadHoraris()
    }

    // Gestió d'errors
    LaunchedEffect(horarisState.error) {
        horarisState.error?.let { error ->
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = error,
                    duration = SnackbarDuration.Long
                )
                groupViewModel.clearError()
            }
        }
    }

    // Tancar diàleg després de crear horari
    LaunchedEffect(horarisState.isCreating) {
        if (!horarisState.isCreating && horarisState.error == null && showAddHorariDialog) {
            showAddHorariDialog = false
            sala = ""
            dia = ""
            hora = ""
            salaError = null
            diaError = null
            horaError = null
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = "Horari creat correctament",
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    // Separar horaris en disponibles i ocupats
    val horarisLliures = horarisState.horaris.filter { it.isLliure }
    val horarisOcupats = horarisState.horaris.filter { !it.isLliure }

    // Funcions de validació
    fun validateSala(): Boolean {
        return if (sala.trim().isEmpty()) {
            salaError = "La sala és obligatòria"
            false
        } else {
            salaError = null
            true
        }
    }

    fun validateDia(): Boolean {
        return if (dia.trim().isEmpty()) {
            diaError = "El dia és obligatori"
            false
        } else {
            diaError = null
            true
        }
    }

    fun validateHora(): Boolean {
        return if (hora.trim().isEmpty()) {
            horaError = "L'hora és obligatòria"
            false
        } else if (!hora.matches(Regex("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$"))) {
            horaError = "Format d'hora invàlid (utilitza HH:MM)"
            false
        } else {
            horaError = null
            true
        }
    }

    fun validateAndCreate() {
        val isSalaValid = validateSala()
        val isDiaValid = validateDia()
        val isHoraValid = validateHora()

        if (isSalaValid && isDiaValid && isHoraValid) {
            groupViewModel.createHorari(
                sala = sala.trim(),
                dia = dia.trim(),
                hora = hora.trim(),
                estat = "lliure"
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestió d'Horaris") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Enrere")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { groupViewModel.loadHoraris() }
                    ) {
                        Icon(Icons.Default.Refresh, "Actualitzar")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddHorariDialog = true }
            ) {
                Icon(Icons.Default.Add, "Afegir Horari")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                horarisState.isLoading -> {
                    CircularProgressIndicator(
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
                                HorariManagementCard(
                                    horari = horari,
                                    isAvailable = true,
                                    isDeleting = horarisState.isDeleting == horari.id,
                                    onDeleteClick = {
                                        horariToDelete = horari.id
                                        showDeleteDialog = true
                                    }
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
                                HorariManagementCard(
                                    horari = horari,
                                    isAvailable = false,
                                    isDeleting = horarisState.isDeleting == horari.id,
                                    onDeleteClick = {
                                        horariToDelete = horari.id
                                        showDeleteDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Diàleg per crear horari
        if (showAddHorariDialog) {
            AlertDialog(
                onDismissRequest = {
                    if (!horarisState.isCreating) {
                        showAddHorariDialog = false
                        sala = ""
                        dia = ""
                        hora = ""
                        salaError = null
                        diaError = null
                        horaError = null
                    }
                },
                title = { Text("Nou Horari") },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Camp Sala
                        OutlinedTextField(
                            value = sala,
                            onValueChange = {
                                sala = it
                                if (salaError != null) validateSala()
                            },
                            label = { Text("Sala *") },
                            placeholder = { Text("Ex: Sala A, Sala B") },
                            singleLine = true,
                            isError = salaError != null,
                            supportingText = salaError?.let { { Text(it) } },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Camp Dia
                        OutlinedTextField(
                            value = dia,
                            onValueChange = {
                                dia = it
                                if (diaError != null) validateDia()
                            },
                            label = { Text("Dia *") },
                            placeholder = { Text("Ex: Dilluns, Dimarts") },
                            singleLine = true,
                            isError = diaError != null,
                            supportingText = diaError?.let { { Text(it) } },
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Camp Hora
                        OutlinedTextField(
                            value = hora,
                            onValueChange = {
                                hora = it
                                if (horaError != null) validateHora()
                            },
                            label = { Text("Hora *") },
                            placeholder = { Text("Ex: 10:00, 18:30") },
                            singleLine = true,
                            isError = horaError != null,
                            supportingText = horaError?.let { { Text(it) } },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Informació
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Informació",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "La combinació sala-dia-hora ha de ser única. " +
                                            "Si ja existeix un horari amb la mateixa combinació, " +
                                            "es mostrarà un error.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { validateAndCreate() },
                        enabled = !horarisState.isCreating
                    ) {
                        if (horarisState.isCreating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text("Crear")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            if (!horarisState.isCreating) {
                                showAddHorariDialog = false
                                sala = ""
                                dia = ""
                                hora = ""
                                salaError = null
                                diaError = null
                                horaError = null
                            }
                        },
                        enabled = !horarisState.isCreating
                    ) {
                        Text("Cancel·lar")
                    }
                }
            )
        }

        // Diàleg de confirmació d'eliminació
        if (showDeleteDialog && horariToDelete != null) {
            val horari = horarisState.horaris.find { it.id == horariToDelete }
            AlertDialog(
                onDismissRequest = {
                    if (horarisState.isDeleting == null) {
                        showDeleteDialog = false
                        horariToDelete = null
                    }
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
                        " Confirmar Eliminació",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Estàs segur que vols eliminar aquest horari?")
                        horari?.let {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Horari a eliminar:",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    Text(
                                        text = "${it.sala} - ${it.dia} - ${it.hora}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
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
                            horariToDelete?.let { id ->
                                // Tancar el diàleg immediatament abans d'eliminar
                                showDeleteDialog = false
                                groupViewModel.deleteHorari(id)
                                // Netejar horariToDelete després d'un petit retard per evitar que es mostri de nou
                                // El LaunchedEffect també netejarà això quan s'acabi l'eliminació
                            }
                        },
                        enabled = horarisState.isDeleting == null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        if (horarisState.isDeleting == horariToDelete) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onError
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text("Eliminar")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            if (horarisState.isDeleting == null) {
                                showDeleteDialog = false
                                horariToDelete = null
                            }
                        },
                        enabled = horarisState.isDeleting == null
                    ) {
                        Text("Cancel·lar")
                    }
                }
            )
        }

        // Netejar estat i mostrar missatge després d'eliminar
        LaunchedEffect(horarisState.isDeleting) {
            if (horarisState.isDeleting == null && horariToDelete != null) {
                // Assegurar que el diàleg està tancat
                showDeleteDialog = false
                // Netejar l'ID de l'horari a eliminar
                horariToDelete = null
                // Mostrar missatge de confirmació
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Horari eliminat correctament",
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }
}

/**
 * Card que mostra la informació d'un horari amb opció d'eliminar (per administradors).
 */
@Composable
fun HorariManagementCard(
    horari: Horari,
    isAvailable: Boolean,
    isDeleting: Boolean,
    onDeleteClick: () -> Unit
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

            // Botons d'acció
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
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

                // Botó eliminar
                IconButton(
                    onClick = onDeleteClick,
                    enabled = !isDeleting
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Eliminar horari",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

