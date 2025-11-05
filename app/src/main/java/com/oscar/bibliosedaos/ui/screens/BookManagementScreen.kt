package com.oscar.bibliosedaos.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.oscar.bibliosedaos.data.models.*
import com.oscar.bibliosedaos.data.network.User
import com.oscar.bibliosedaos.navigation.AppScreens
import com.oscar.bibliosedaos.ui.viewmodels.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Pantalla principal de gestió del catàleg de llibres amb gestió de préstecs integrada.
 *
 * @param navController Controlador de navegació
 * @param bookViewModel ViewModel per gestió de llibres
 * @param authViewModel ViewModel per gestió d'usuaris
 * @param loanViewModel ViewModel per gestió de préstecs
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookManagementScreen(
    navController: NavController,
    bookViewModel: BookViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel(),
    loanViewModel: LoanViewModel = viewModel()
) {
    // ========== ESTATS ==========

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Llibres", "Autors", "Exemplars")
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Estats observables dels ViewModels
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

    // ========== UI PRINCIPAL ==========

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Gestió del Catàleg",
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Tornar")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            // Recarregar dades
                            bookViewModel.loadLlibres()
                            bookViewModel.loadAutors()
                            bookViewModel.loadExemplars()
                        }
                    ) {
                        Icon(Icons.Default.Refresh, "Actualitzar")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    when (selectedTab) {
                        0 -> navController.navigate(AppScreens.AddBookScreen.route)
                        1 -> { /* Diàleg per afegir autor */ }
                        2 -> navController.navigate(AppScreens.AddExemplarScreen.route)
                    }
                }
            ) {
                Icon(Icons.Default.Add, "Afegir")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
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

                // NOVA PESTANYA D'EXEMPLARS AMB GESTIÓ DE PRÉSTECS
                2 -> ExemplarsTabWithLoans(
                    bookViewModel = bookViewModel,
                    authViewModel = authViewModel,
                    loanViewModel = loanViewModel,
                    onAddClick = {
                        navController.navigate(AppScreens.AddExemplarScreen.route)
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
                        llibreToDelete?.let { bookViewModel.deleteLlibre(it) }
                        showDeleteDialog = false
                        llibreToDelete = null
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

// ========== PESTANYA DE LLIBRES (sense canvis) ==========

@Composable
private fun LlibresTab(
    llibresState: LlibresUiState,
    onEditLlibre: (Llibre) -> Unit,
    onDeleteLlibre: (Long) -> Unit
) {
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
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
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

// ========== PESTANYA D'AUTORS (sense canvis) ==========

@Composable
private fun AutorsTab(
    autorsState: AutorsUiState,
    onDeleteAutor: (Long) -> Unit
) {
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
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
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

// ========== NOVA PESTANYA D'EXEMPLARS AMB GESTIÓ DE PRÉSTECS ==========

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExemplarsTabWithLoans(
    bookViewModel: BookViewModel,
    authViewModel: AuthViewModel,
    loanViewModel: LoanViewModel,
    onAddClick: () -> Unit
) {
    val exemplarsState by bookViewModel.exemplarsState.collectAsState()
    val activeLoansState by loanViewModel.activeLoansState.collectAsState()
    val usersState by authViewModel.allUsersState.collectAsState()

    var showUserSelectionDialog by remember { mutableStateOf(false) }
    var selectedExemplar by remember { mutableStateOf<Exemplar?>(null) }
    var targetStatus by remember { mutableStateOf<ExemplarStatus?>(null) }

    val coroutineScope = rememberCoroutineScope()

    // Carregar dades inicials
    LaunchedEffect(Unit) {
        bookViewModel.loadExemplars()
        loanViewModel.loadActiveLoans(null)
        authViewModel.loadAllUsers()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Títol
        Text(
            text = "Gestió d'Exemplars i Préstecs",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        // Llista d'exemplars
        when {
            exemplarsState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }

            exemplarsState.error != null -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = exemplarsState.error!!,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            exemplarsState.exemplars.isEmpty() -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text(
                        text = "No hi ha exemplars disponibles",
                        modifier = Modifier.padding(32.dp).fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }

            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(exemplarsState.exemplars) { exemplar ->
                        ExemplarItemWithLoan(
                            exemplar = exemplar,
                            activeLoan = activeLoansState.loans.find {
                                it.exemplar.id == exemplar.id && it.isActive
                            },
                            onStatusClick = { status ->
                                selectedExemplar = exemplar
                                targetStatus = status

                                val currentStatus = ExemplarStatus.fromString(exemplar.reservat)

                                when {
                                    // Retornar préstec
                                    currentStatus == ExemplarStatus.PRESTAT &&
                                            status == ExemplarStatus.LLIURE -> {
                                        val loan = activeLoansState.loans.find {
                                            it.exemplar.id == exemplar.id && it.isActive
                                        }
                                        if (loan != null) {
                                            coroutineScope.launch {
                                                loanViewModel.returnLoan(loan.id)
                                                bookViewModel.updateExemplar(
                                                    exemplar.id!!,
                                                    exemplar.copy(reservat = status.value)
                                                )
                                                bookViewModel.loadExemplars()
                                                loanViewModel.loadActiveLoans(null)
                                            }
                                        }
                                    }

                                    // Prestar o reservar
                                    currentStatus == ExemplarStatus.LLIURE &&
                                            status != ExemplarStatus.LLIURE -> {
                                        showUserSelectionDialog = true
                                    }

                                    // Altres transicions
                                    else -> {
                                        coroutineScope.launch {
                                            bookViewModel.updateExemplar(
                                                exemplar.id!!,
                                                exemplar.copy(reservat = status.value)
                                            )
                                            bookViewModel.loadExemplars()
                                        }
                                    }
                                }
                            },
                            onDelete = {
                                coroutineScope.launch {
                                    bookViewModel.deleteExemplar(exemplar.id!!)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Diàleg selecció d'usuari
    if (showUserSelectionDialog && selectedExemplar != null && targetStatus != null) {
        UserSelectionDialog(
            users = usersState.users,
            isLoading = usersState.isLoading,
            onUserSelected = { user ->
                coroutineScope.launch {
                    bookViewModel.updateExemplar(
                        selectedExemplar!!.id!!,
                        selectedExemplar!!.copy(reservat = targetStatus!!.value)
                    )

                    if (targetStatus == ExemplarStatus.PRESTAT) {
                        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                        loanViewModel.createLoan(
                            usuariId = user.id,
                            exemplarId = selectedExemplar!!.id!!,
                            dataPrestec = today
                        )
                    }

                    bookViewModel.loadExemplars()
                    loanViewModel.loadActiveLoans(null)

                    showUserSelectionDialog = false
                    selectedExemplar = null
                    targetStatus = null
                }
            },
            onDismiss = {
                showUserSelectionDialog = false
                selectedExemplar = null
                targetStatus = null
            }
        )
    }
}

// ========== COMPONENT: ITEM D'EXEMPLAR AMB PRÉSTEC ==========

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExemplarItemWithLoan(
    exemplar: Exemplar,
    activeLoan: Prestec? = null,
    onStatusClick: (ExemplarStatus) -> Unit,
    onDelete: () -> Unit
) {
    var showStatusMenu by remember { mutableStateOf(false) }
    val currentStatus = ExemplarStatus.fromString(exemplar.reservat)

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Informació del llibre
                    Text(
                        text = exemplar.llibre?.titol ?: "Llibre desconegut",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    exemplar.llibre?.autor?.let { autor ->
                        Text(
                            text = "Autor: ${autor.nom}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Ubicació
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Place,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = exemplar.lloc,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    // Estat
                    Spacer(modifier = Modifier.height(8.dp))

                    Box {
                        AssistChip(
                            onClick = { showStatusMenu = true },
                            label = {
                                Text(
                                    text = currentStatus.displayName,
                                    color = Color.White
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = when(currentStatus) {
                                        ExemplarStatus.LLIURE -> Icons.Default.CheckCircle
                                        ExemplarStatus.PRESTAT -> Icons.Default.Person
                                        ExemplarStatus.RESERVAT -> Icons.Default.Schedule
                                    },
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = currentStatus.color
                            )
                        )

                        // Menú desplegable
                        DropdownMenu(
                            expanded = showStatusMenu,
                            onDismissRequest = { showStatusMenu = false }
                        ) {
                            ExemplarStatus.values().forEach { status ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = when(status) {
                                                    ExemplarStatus.LLIURE -> Icons.Default.CheckCircle
                                                    ExemplarStatus.PRESTAT -> Icons.Default.Person
                                                    ExemplarStatus.RESERVAT -> Icons.Default.Schedule
                                                },
                                                contentDescription = null,
                                                tint = status.color,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(status.displayName)
                                        }
                                    },
                                    onClick = {
                                        showStatusMenu = false
                                        if (status != currentStatus) {
                                            onStatusClick(status)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Informació de préstec
                    if (currentStatus == ExemplarStatus.PRESTAT && activeLoan != null) {
                        Spacer(modifier = Modifier.height(8.dp))

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "📚 Prestat a: ${activeLoan.usuari?.nom ?: "Desconegut"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "📅 Des de: ${activeLoan.dataPrestec}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                // Botó eliminar
                IconButton(
                    onClick = onDelete,
                    enabled = currentStatus == ExemplarStatus.LLIURE
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = if (currentStatus == ExemplarStatus.LLIURE)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }
            }
        }
    }
}

// ========== DIÀLEG DE SELECCIÓ D'USUARI ==========

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSelectionDialog(
    users: List<User>,
    isLoading: Boolean,
    onUserSelected: (User) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Capçalera
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Seleccionar Usuari",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Tancar")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Camp de cerca
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Cercar usuari...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Llista d'usuaris
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }

                    users.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            Text(
                                text = "No hi ha usuaris disponibles",
                                modifier = Modifier.align(Alignment.Center),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    else -> {
                        val filteredUsers = users.filter { user ->
                            searchQuery.isEmpty() ||
                                    user.nom.contains(searchQuery, ignoreCase = true) ||
                                    user.nick.contains(searchQuery, ignoreCase = true) ||
                                    user.email?.contains(searchQuery, ignoreCase = true) == true
                        }

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredUsers) { user ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onUserSelected(user) },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Avatar
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(MaterialTheme.colorScheme.primary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = user.nom.first().uppercase(),
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        // Informació
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${user.nom} ${user.cognom1 ?: ""}",
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = "@${user.nick}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        // Badge admin
                                        if (user.isAdmin) {
                                            AssistChip(
                                                onClick = { },
                                                label = { Text("Admin") },
                                                enabled = false
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ========== ENUM ESTATS ==========

enum class ExemplarStatus(val value: String, val displayName: String, val color: Color) {
    LLIURE("lliure", "Lliure", Color(0xFF4CAF50)),
    PRESTAT("prestat", "Prestat", Color(0xFFF44336)),
    RESERVAT("reservat", "Reservat", Color(0xFFFF9800));

    companion object {
        fun fromString(value: String): ExemplarStatus {
            return values().find { it.value == value } ?: LLIURE
        }
    }
}

// ========== CARDS REUTILITZABLES ==========

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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = llibre.titol,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                llibre.autor?.let {
                    Text(
                        text = "Autor: ${it.nom}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Text(
                    text = "ISBN: ${llibre.isbn}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, "Editar")
                }
                if (isDeleting) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            "Eliminar",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

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
            Text(
                text = autor.nom,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )

            if (isDeleting) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        "Eliminar",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}