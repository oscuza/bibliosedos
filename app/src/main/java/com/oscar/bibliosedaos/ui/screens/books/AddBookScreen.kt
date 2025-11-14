package com.oscar.bibliosedaos.ui.screens.books

import androidx.compose.foundation.layout.*
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.oscar.bibliosedaos.data.models.Llibre
import com.oscar.bibliosedaos.ui.viewmodels.BookViewModel
import kotlinx.coroutines.launch

/**
 * Pantalla per afegir un nou llibre al catàleg.
 *
 * **Descripció:**
 * Formulari complet per crear nous llibres amb validació en temps real.
 * Permet seleccionar l'autor d'una llista o crear-ne un de nou.
 *
 * **Camps del Formulari:**
 * - ISBN: Codi únic del llibre (obligatori)
 * - Títol: Nom del llibre (obligatori)
 * - Autor: Selecció d'autor existent (opcional)
 * - Pàgines: Nombre de pàgines (mínim 1)
 * - Editorial: Nom de l'editorial (obligatori)
 *
 * **Validacions:**
 * - ISBN: No pot estar buit i ha de ser únic
 * - Títol: Mínim 2 caràcters
 * - Pàgines: Nombre positiu major que 0
 * - Editorial: No pot estar buida
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
 * @see BookViewModel.addLlibre
 * @see BookManagementScreen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBookScreen(
    navController: NavController,
    bookViewModel: BookViewModel = viewModel()
) {
    // ========== ESTATS DEL FORMULARI ==========

    var isbn by remember { mutableStateOf("") }
    var titol by remember { mutableStateOf("") }
    var pagines by remember { mutableStateOf("") }
    var editorial by remember { mutableStateOf("") }
    var selectedAutorId by remember { mutableStateOf<Long?>(null) }

    // Estats de validació
    var isbnError by remember { mutableStateOf<String?>(null) }
    var titolError by remember { mutableStateOf<String?>(null) }
    var paginesError by remember { mutableStateOf<String?>(null) }
    var editorialError by remember { mutableStateOf<String?>(null) }

    // Diàleg per afegir nou autor
    var showAddAutorDialog by remember { mutableStateOf(false) }

    // Estats observables
    val autorsState by bookViewModel.autorsState.collectAsState()
    val formState by bookViewModel.llibresState.collectAsState()

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Variable per rastrejar si s'ha enviat el formulari
    var hasSubmitted by remember { mutableStateOf(false) }

    // ========== CÀRREGA D'AUTORS ==========

    LaunchedEffect(Unit) {
        bookViewModel.loadAutors()
    }

    // ========== GESTIÓ DE RESPOSTES ==========

    LaunchedEffect(formState.isCreating, formState.error) {
        if (hasSubmitted && !formState.isCreating) {
            if (formState.error == null) {
                // Èxit: mostra missatge i navega enrere
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Llibre creat correctament",
                        duration = SnackbarDuration.Short
                    )
                }
                // Resetear el formulari
                isbn = ""
                titol = ""
                pagines = ""
                editorial = ""
                selectedAutorId = null
                hasSubmitted = false
                navController.navigateUp()
            } else {
                // Error: només mostra l'error (ja es gestiona en un altre LaunchedEffect)
                hasSubmitted = false
            }
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

    fun validateIsbn(): Boolean {
        isbnError = when {
            isbn.isBlank() -> "L'ISBN és obligatori"
            isbn.length < 10 -> "L'ISBN ha de tenir almenys 10 caràcters"
            !isbn.matches(Regex("^[0-9-]+$")) -> "L'ISBN només pot contenir números i guions"
            else -> null
        }
        return isbnError == null
    }

    fun validateTitol(): Boolean {
        titolError = when {
            titol.isBlank() -> "El títol és obligatori"
            titol.length < 2 -> "El títol ha de tenir almenys 2 caràcters"
            else -> null
        }
        return titolError == null
    }

    fun validatePagines(): Boolean {
        val paginesNum = pagines.toIntOrNull()
        paginesError = when {
            pagines.isBlank() -> "El nombre de pàgines és obligatori"
            paginesNum == null -> "Introdueix un nombre vàlid"
            paginesNum <= 0 -> "El nombre de pàgines ha de ser positiu"
            paginesNum > 10000 -> "Nombre de pàgines no vàlid"
            else -> null
        }
        return paginesError == null
    }

    fun validateEditorial(): Boolean {
        editorialError = when {
            editorial.isBlank() -> "L'editorial és obligatòria"
            editorial.length < 2 -> "L'editorial ha de tenir almenys 2 caràcters"
            else -> null
        }
        return editorialError == null
    }

    fun validateAndSubmit() {
        val isIsbnValid = validateIsbn()
        val isTitolValid = validateTitol()
        val isPaginesValid = validatePagines()
        val isEditorialValid = validateEditorial()

        if (isIsbnValid && isTitolValid && isPaginesValid && isEditorialValid) {
            hasSubmitted = true
            val nouLlibre = Llibre(
                isbn = isbn.trim(),
                titol = titol.trim(),
                pagines = pagines.toInt(),
                editorial = editorial.trim(),
                autor = selectedAutorId?.let { autorId ->
                    autorsState.autors.find { it.id == autorId }
                }
            )
            bookViewModel.createLlibre(nouLlibre)
        }
    }

    // ========== UI ==========

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "📖 Afegir Nou Llibre",
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
            // ========== INFORMACIÓ BÀSICA ==========

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
                        "Informació Bàsica",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Camp ISBN
                    OutlinedTextField(
                        value = isbn,
                        onValueChange = {
                            isbn = it
                            if (isbnError != null) validateIsbn()
                        },
                        label = { Text("ISBN *") },
                        placeholder = { Text("978-3-16-148410-0") },
                        leadingIcon = {
                            Icon(Icons.Default.Tag, contentDescription = null)
                        },
                        isError = isbnError != null,
                        supportingText = {
                            isbnError?.let { Text(it) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text
                        )
                    )

                    // Camp Títol
                    OutlinedTextField(
                        value = titol,
                        onValueChange = {
                            titol = it
                            if (titolError != null) validateTitol()
                        },
                        label = { Text("Títol *") },
                        placeholder = { Text("Nom del llibre") },
                        leadingIcon = {
                            Icon(Icons.Default.Book, contentDescription = null)
                        },
                        isError = titolError != null,
                        supportingText = {
                            titolError?.let { Text(it) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            // ========== DETALLS DEL LLIBRE ==========

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
                        "Detalls del Llibre",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Selecció d'Autor
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        var expanded by remember { mutableStateOf(false) }
                        val selectedAutor = autorsState.autors.find { it.id == selectedAutorId }

                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = selectedAutor?.nom ?: "",
                                onValueChange = { },
                                readOnly = true,
                                label = { Text("Autor") },
                                placeholder = { Text("Selecciona un autor") },
                                leadingIcon = {
                                    Icon(Icons.Default.Person, contentDescription = null)
                                },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                                },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )

                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                if (autorsState.autors.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("No hi ha autors disponibles") },
                                        onClick = { }
                                    )
                                } else {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                "Sense autor",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        },
                                        onClick = {
                                            selectedAutorId = null
                                            expanded = false
                                        }
                                    )

                                    autorsState.autors.forEach { autor ->
                                        DropdownMenuItem(
                                            text = { Text(autor.nom) },
                                            onClick = {
                                                selectedAutorId = autor.id
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Botó per afegir nou autor
                        IconButton(
                            onClick = { showAddAutorDialog = true }
                        ) {
                            Icon(
                                Icons.Default.PersonAdd,
                                contentDescription = "Afegir nou autor",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Camp Nombre de Pàgines
                    OutlinedTextField(
                        value = pagines,
                        onValueChange = {
                            if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                                pagines = it
                                if (paginesError != null) validatePagines()
                            }
                        },
                        label = { Text("Nombre de pàgines *") },
                        placeholder = { Text("250") },
                        leadingIcon = {
                            Icon(Icons.Default.Pages, contentDescription = null)
                        },
                        isError = paginesError != null,
                        supportingText = {
                            paginesError?.let { Text(it) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        )
                    )

                    // Camp Editorial
                    OutlinedTextField(
                        value = editorial,
                        onValueChange = {
                            editorial = it
                            if (editorialError != null) validateEditorial()
                        },
                        label = { Text("Editorial *") },
                        placeholder = { Text("Nom de l'editorial") },
                        leadingIcon = {
                            Icon(Icons.Default.Business, contentDescription = null)
                        },
                        isError = editorialError != null,
                        supportingText = {
                            editorialError?.let { Text(it) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            // ========== INFO ==========

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
                    enabled = !formState.isCreating
                ) {
                    if (formState.isCreating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Guardar")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // ========== DIÀLEG AFEGIR AUTOR ==========

    if (showAddAutorDialog) {
        var nouAutorNom by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddAutorDialog = false },
            title = { Text("Afegir Nou Autor") },
            text = {
                Column {
                    Text("Introdueix el nom complet de l'autor:")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = nouAutorNom,
                        onValueChange = { nouAutorNom = it },
                        label = { Text("Nom de l'autor") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (nouAutorNom.isNotBlank()) {
                            bookViewModel.createAutor(nouAutorNom.trim())
                            showAddAutorDialog = false
                            nouAutorNom = ""
                        }
                    },
                    enabled = nouAutorNom.isNotBlank() && !autorsState.isCreating
                ) {
                    if (autorsState.isCreating) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    } else {
                        Text("Afegir")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddAutorDialog = false }) {
                    Text("Cancel·lar")
                }
            }
        )
    }
}

