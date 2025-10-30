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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.oscar.bibliosedaos.data.models.*
import com.oscar.bibliosedaos.navigation.AppScreens
import com.oscar.bibliosedaos.ui.viewmodels.AutorsUiState
import com.oscar.bibliosedaos.ui.viewmodels.BookViewModel
import com.oscar.bibliosedaos.ui.viewmodels.ExemplarsUiState
import com.oscar.bibliosedaos.ui.viewmodels.LlibresUiState
import kotlinx.coroutines.launch

/**
 * Pantalla principal de gestió del catàleg de llibres.
 *
 * **Descripció:**
 * Interfície d'administrador per gestionar tot el catàleg de la biblioteca.
 * Organitzada en tres pestanyes principals:
 * - Llibres: CRUD complet de llibres
 * - Autors: Gestió d'autors
 * - Exemplars: Control d'inventari
 *
 * **Funcionalitats:**
 * - Visualització en llistes amb cards
 * - Botons FAB per afegir nous elements
 * - Opcions d'edició i eliminació
 * - Cerca d'exemplars lliures
 * - Gestió d'errors i estats de càrrega
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
 * @see BookViewModel
 * @see AddBookScreen
 * @see AddExemplarScreen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookManagementScreen(
    navController: NavController,
    bookViewModel: BookViewModel = viewModel()
) {
    // ========== ESTATS ==========

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Llibres", "Autors", "Exemplars")
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Estats observables del ViewModel
    val llibresState by bookViewModel.llibresState.collectAsState()
    val autorsState by bookViewModel.autorsState.collectAsState()
    val exemplarsState by bookViewModel.exemplarsState.collectAsState()

    // Variables per al diàleg de confirmació d'eliminació
    var showDeleteDialog by remember { mutableStateOf(false) }
    var llibreToDelete by remember { mutableStateOf<Long?>(null) }

    // ========== CÀRREGA INICIAL ==========

    LaunchedEffect(Unit) {
        bookViewModel.loadLlibres()
        bookViewModel.loadAutors()
        bookViewModel.loadExemplars()
    }

    // ========== GESTIÓ D'ERRORS ==========

    LaunchedEffect(llibresState.error, autorsState.error, exemplarsState.error) {
        val error = llibresState.error ?: autorsState.error ?: exemplarsState.error
        error?.let {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = it,
                    duration = SnackbarDuration.Long,
                    actionLabel = "Tancar"
                )
                bookViewModel.clearErrors()
            }
        }
    }

    // ========== UI ==========

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "📚 Gestió del Catàleg",
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            when (selectedTab) {
                0 -> { // Llibres
                    FloatingActionButton(
                        onClick = { navController.navigate(AppScreens.AddBookScreen.route) },
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Default.Add, "Afegir Llibre")
                    }
                }
                1 -> { // Autors
                    var showAutorDialog by remember { mutableStateOf(false) }

                    FloatingActionButton(
                        onClick = { showAutorDialog = true },
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Default.PersonAdd, "Afegir Autor")
                    }

                    // Diàleg per afegir autor
                    if (showAutorDialog) {
                        AddAutorDialog(
                            onDismiss = { showAutorDialog = false },
                            onConfirm = { nom ->
                                bookViewModel.addAutor(nom)
                                showAutorDialog = false
                            }
                        )
                    }
                }
                2 -> { // Exemplars
                    FloatingActionButton(
                        onClick = { navController.navigate(AppScreens.AddExemplarScreen.route) },
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Default.Add, "Afegir Exemplar")
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ========== TABS ==========

            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        text = {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = when (index) {
                                        0 -> Icons.Default.Book
                                        1 -> Icons.Default.Person
                                        else -> Icons.Default.Inventory
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(title)
                            }
                        },
                        selected = selectedTab == index,
                        onClick = { selectedTab = index }
                    )
                }
            }

            // ========== CONTINGUT SEGONS TAB ==========

            when (selectedTab) {
                0 -> LlibresTab(
                llibresState = llibresState,
                onEditLlibre = { llibre ->
                    navController.navigate(
                        AppScreens.EditBookScreen.createRoute(llibre.id ?: 0)
                    )
                },
                onDeleteLlibre = { id ->
                    // Comprovar si té exemplars
                    val exemplarsDelLlibre = exemplarsState.exemplars.filter {
                        it.llibre?.id == id
                    }

                    if (exemplarsDelLlibre.isNotEmpty()) {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "⚠️ Aquest llibre té ${exemplarsDelLlibre.size} exemplar(s). Elimina'ls primer!",
                                duration = SnackbarDuration.Long
                            )
                        }
                    } else {
                        llibreToDelete = id
                        showDeleteDialog = true
                    }
                }
            )

                1 -> AutorsTab(
                    autorsState = autorsState,
                    onDeleteAutor = { id ->
                        bookViewModel.deleteAutor(id)
                    }
                )

                2 -> ExemplarsTab(
                    exemplarsState = exemplarsState,
                    onSearchExemplars = { titol, autor ->
                        bookViewModel.searchExemplarsLliures(titol, autor)
                    },
                    onDeleteExemplar = { id ->
                        bookViewModel.deleteExemplar(id)
                    }
                )
            }
        }
    }
    // ========== DIÀLEG DE CONFIRMACIÓ D'ELIMINACIÓ ==========

    if (showDeleteDialog && llibreToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                llibreToDelete = null
            },
            title = {
                Text(
                    "⚠️ Confirmar eliminació",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Text("Estàs segur que vols eliminar aquest llibre? Aquesta acció no es pot desfer.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        bookViewModel.deleteLlibre(llibreToDelete!!)
                        showDeleteDialog = false
                        llibreToDelete = null

                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Llibre eliminat correctament",
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        llibreToDelete = null
                    }
                ) {
                    Text("Cancel·lar")
                }
            }
        )
    }
}

/**
 * Tab de Llibres: Mostra la llista de llibres amb opcions CRUD.
 */
@Composable
private fun LlibresTab(
    llibresState: LlibresUiState,
    onEditLlibre: (Llibre) -> Unit,
    onDeleteLlibre: (Long) -> Unit
) {
    if (!llibresState.isLoading && llibresState.llibres.isNotEmpty()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "📚 Total de llibres:",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "${llibresState.llibres.size}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
    when {
        llibresState.isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        llibresState.llibres.isEmpty() -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No hi ha llibres registrats",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(llibresState.llibres) { llibre ->
                    LlibreCard(
                        llibre = llibre,
                        isDeleting = llibresState.isDeleting == llibre.id,
                        onEdit = { onEditLlibre(llibre) },
                        onDelete = { llibre.id?.let { onDeleteLlibre(it) } }
                    )
                }
            }
        }
    }
}

/**
 * Card per mostrar la informació d'un llibre.
 */
@Composable
private fun LlibreCard(
    llibre: Llibre,
    isDeleting: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
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
                Text(
                    text = llibre.titol,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                llibre.autor?.let { autor ->
                    Text(
                        text = "Autor: ${autor.nom}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = { },
                        label = { Text("ISBN: ${llibre.isbn}") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Tag,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )

                    AssistChip(
                        onClick = { },
                        label = { Text("${llibre.pagines} pàg.") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Pages,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }

                Text(
                    text = "Editorial: ${llibre.editorial}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isDeleting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Editar",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tab d'Autors: Mostra la llista d'autors.
 */
@Composable
private fun AutorsTab(
    autorsState: AutorsUiState,
    onDeleteAutor: (Long) -> Unit
) {
    if (!autorsState.isLoading && autorsState.autors.isNotEmpty()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "✍️ Total d'autors:",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "${autorsState.autors.size}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
    when {
        autorsState.isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        autorsState.autors.isEmpty() -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No hi ha autors registrats",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(autorsState.autors) { autor ->
                    AutorCard(
                        autor = autor,
                        isDeleting = autorsState.isDeleting == autor.id,
                        onDelete = { autor.id?.let { onDeleteAutor(it) } }
                    )
                }
            }
        }
    }
}

/**
 * Card per mostrar la informació d'un autor.
 */
@Composable
private fun AutorCard(
    autor: Autor,
    isDeleting: Boolean,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Column {
                    Text(
                        text = autor.nom,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "ID: ${autor.id}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isDeleting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp)
                )
            } else {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

/**
 * Tab d'Exemplars: Mostra la llista d'exemplars amb cerca.
 */
@Composable
private fun ExemplarsTab(
    exemplarsState: ExemplarsUiState,
    onSearchExemplars: (String?, String?) -> Unit,
    onDeleteExemplar: (Long) -> Unit
) {
    var searchTitle by remember { mutableStateOf("") }
    var searchAuthor by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Barra de cerca
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Cercar Exemplars Lliures",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchTitle,
                        onValueChange = { searchTitle = it },
                        label = { Text("Títol") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = searchAuthor,
                        onValueChange = { searchAuthor = it },
                        label = { Text("Autor") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            onSearchExemplars(
                                searchTitle.ifBlank { null },
                                searchAuthor.ifBlank { null }
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Cercar")
                    }

                    OutlinedButton(
                        onClick = {
                            searchTitle = ""
                            searchAuthor = ""
                            onSearchExemplars(null, null)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Netejar")
                    }
                }
            }
        }
        if (!exemplarsState.isLoading && !exemplarsState.isSearching) {
            val exemplarsToShow = exemplarsState.searchResults ?: exemplarsState.exemplars
            if (exemplarsToShow.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Contador total
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "${exemplarsToShow.size}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                "Total",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        // Contador lliures
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "${exemplarsToShow.count { it.reservat == "lliure" }}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Lliures",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        // Contador prestats
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "${exemplarsToShow.count { it.reservat == "prestat" }}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                "Prestats",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }
        // Llista d'exemplars
        val exemplarsToShow = exemplarsState.searchResults ?: exemplarsState.exemplars

        when {
            exemplarsState.isLoading || exemplarsState.isSearching -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            exemplarsToShow.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (exemplarsState.searchResults != null)
                            "No s'han trobat exemplars amb aquests criteris"
                        else
                            "No hi ha exemplars registrats",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(exemplarsToShow) { exemplar ->
                        ExemplarCard(
                            exemplar = exemplar,
                            isDeleting = exemplarsState.isDeleting == exemplar.id,
                            onDelete = { exemplar.id?.let { onDeleteExemplar(it) } }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Card per mostrar la informació d'un exemplar.
 */
@Composable
private fun ExemplarCard(
    exemplar: Exemplar,
    isDeleting: Boolean,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                exemplar.llibre?.let { llibre ->
                    Text(
                        text = llibre.titol,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    llibre.autor?.let {
                        Text(
                            text = "Autor: ${it.nom}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = { },
                        label = { Text("📍 ${exemplar.lloc}") }
                    )

                    val (color, icon, text) = when (exemplar.reservat) {
                        "lliure" -> Triple(
                            MaterialTheme.colorScheme.primary,
                            Icons.Default.CheckCircle,
                            "Lliure"
                        )
                        "prestat" -> Triple(
                            MaterialTheme.colorScheme.error,
                            Icons.Default.Schedule,
                            "Prestat"
                        )
                        else -> Triple(
                            MaterialTheme.colorScheme.tertiary,
                            Icons.Default.BookmarkBorder,
                            "Reservat"
                        )
                    }

                    AssistChip(
                        onClick = { },
                        label = { Text(text) },
                        leadingIcon = {
                            Icon(
                                icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = color
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            leadingIconContentColor = color
                        )
                    )
                }

                Text(
                    text = "ID: ${exemplar.id}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isDeleting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp)
                )
            } else {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

/**
 * Diàleg per afegir un nou autor.
 */
@Composable
private fun AddAutorDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var nom by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Afegir Nou Autor") },
        text = {
            Column {
                Text("Introdueix el nom complet de l'autor:")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = nom,
                    onValueChange = { nom = it },
                    label = { Text("Nom de l'autor") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (nom.isNotBlank()) {
                        onConfirm(nom.trim())
                    }
                },
                enabled = nom.isNotBlank()
            ) {
                Text("Afegir")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel·lar")
            }
        }
    )
}

