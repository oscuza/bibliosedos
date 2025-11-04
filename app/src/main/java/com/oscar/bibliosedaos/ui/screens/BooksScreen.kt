package com.oscar.bibliosedaos.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.oscar.bibliosedaos.data.models.Exemplar
import com.oscar.bibliosedaos.data.models.Llibre
import com.oscar.bibliosedaos.data.network.TokenManager
import com.oscar.bibliosedaos.navigation.AppScreens
import com.oscar.bibliosedaos.ui.viewmodels.AuthViewModel
import com.oscar.bibliosedaos.ui.viewmodels.BookViewModel
import com.oscar.bibliosedaos.ui.viewmodels.LoanViewModel
import kotlinx.coroutines.delay

/**
 * Pantalla del catàleg de llibres de la biblioteca.
 *
 * **Descripció:**
 * Pantalla accessible per a tots els usuaris autenticats que mostra el catàleg
 * complet de llibres disponibles a la biblioteca amb informació de disponibilitat
 * en temps real basada en l'estat dels exemplars físics.
 *
 * **Funcionalitats:**
 * - Llistat complet de tots els llibres
 * - Informació detallada: títol, autor, editorial, ISBN, pàgines
 * - Indicador visual de disponibilitat per cada llibre
 * - Comptador d'exemplars: disponibles vs totals
 * - Estats dels exemplars: Lliure, Prestat, Reservat
 * - Cerca per títol o autor
 * - **Préstec de llibres:** Botó per prestar exemplars lliures
 *
 * **Càlcul de Disponibilitat:**
 * - **Lliure:** exemplar.reservat == "lliure"
 * - **Prestat:** exemplar.reservat == "prestat"
 * - **Reservat:** exemplar.reservat == "reservat"
 *
 * **Permisos:**
 * - 👥 Accessible per usuaris normals i administradors
 * - 🔒 Requereix token JWT vàlid
 * - 📚 Préstec: Backend només permet a admins, però la UI permet a tots (validació backend)
 *
 * **Navegació:**
 * - **Entrada:** Des de ProfileScreen (qualsevol rol)
 * - **Sortida:** Botó enrere torna a ProfileScreen
 *
 * @param navController Controlador de navegació
 * @param bookViewModel ViewModel per gestionar llibres i exemplars
 * @param authViewModel ViewModel per informació de l'usuari
 * @param loanViewModel ViewModel per gestionar préstecs
 *
 * @author Oscar
 * @since 1.0
 * @see Llibre
 * @see Exemplar
 * @see BookViewModel
 * @see LoanViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BooksScreen(
    navController: NavController,
    bookViewModel: BookViewModel,
    authViewModel: AuthViewModel,
    loanViewModel: LoanViewModel
) {
    // ========== Estats Observables ==========

    /**
     * Estat de la llista de llibres.
     * Conté: llibres, isLoading, error
     */
    val llibresState by bookViewModel.llibresState.collectAsState()

    /**
     * Estat de la llista d'exemplars.
     * Necessari per calcular disponibilitat.
     */
    val exemplarsState by bookViewModel.exemplarsState.collectAsState()

    /**
     * Informació de l'usuari actual per saber l'ID.
     */
    val loginState by authViewModel.loginUiState.collectAsState()

    /**
     * Estat de creació de préstec.
     */
    val createLoanState by loanViewModel.createLoanState.collectAsState()

    val context = LocalContext.current

    // ========== Estats Locals ==========

    /**
     * Text de cerca introduït per l'usuari.
     */
    var searchText by remember { mutableStateOf("") }

    /**
     * Flag per prevenir doble clic al botó enrere.
     */
    var isNavigating by remember { mutableStateOf(false) }

    /**
     * Llibre seleccionat per veure detalls.
     */
    var selectedBook by remember { mutableStateOf<Llibre?>(null) }

    /**
     * Exemplar seleccionat per prestar.
     */
    var exemplarToLoan by remember { mutableStateOf<Long?>(null) }

    /**
     * Diàleg de confirmació de préstec.
     */
    var showLoanDialog by remember { mutableStateOf(false) }

    // Determinar si l'usuari és admin
    val isAdmin = loginState.authResponse?.rol == 2
    val currentUserId = loginState.authResponse?.id

    // ========== Càrrega Inicial ==========

    /**
     * Carrega llibres i exemplars en iniciar la pantalla.
     * S'executa una sola vegada.
     */
    LaunchedEffect(Unit) {
        delay(200) // Assegurar que el token està llest
        if (TokenManager.hasToken()) {
            bookViewModel.loadLlibres()
            bookViewModel.loadExemplars()
        } else {
            // Si no hi ha token, tornar al login
            navController.navigate(AppScreens.LoginScreen.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    // ========== Gestió d'Èxit de Préstec ==========

    LaunchedEffect(createLoanState.success) {
        if (createLoanState.success) {
            Toast.makeText(
                context,
                createLoanState.successMessage ?: "Préstec creat correctament",
                Toast.LENGTH_SHORT
            ).show()

            // Refrescar llibres i exemplars
            bookViewModel.loadLlibres()
            bookViewModel.loadExemplars()

            // Reiniciar estats
            loanViewModel.resetForms()
            showLoanDialog = false
            exemplarToLoan = null
            selectedBook = null
        }
    }

    // ========== Mostrar Errors de Préstec ==========

    createLoanState.error?.let { error ->
        LaunchedEffect(error) {
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            loanViewModel.clearErrors()
        }
    }

    // ========== Filtrat de Llibres ==========

    /**
     * Llista de llibres filtrada segons el text de cerca.
     * Busca coincidències en títol o nom d'autor.
     */
    val filteredBooks = remember(llibresState.llibres, searchText) {
        if (searchText.isBlank()) {
            llibresState.llibres
        } else {
            llibresState.llibres.filter { llibre ->
                llibre.titol.contains(searchText, ignoreCase = true) ||
                        llibre.autor?.nom?.contains(searchText, ignoreCase = true) == true
            }
        }
    }

    // ========== UI Principal ==========

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Catàleg de Llibres") },
                navigationIcon = {
                    if (!isNavigating) {
                        IconButton(onClick = {
                            if (!isNavigating) {
                                isNavigating = true
                                val userId = loginState.authResponse?.id ?: 0L
                                navController.navigate(
                                    AppScreens.UserProfileScreen.createRoute(userId)
                                ) {
                                    popUpTo(AppScreens.BooksScreen.route) {
                                        inclusive = true
                                    }
                                    launchSingleTop = true
                                }
                            }
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Tornar"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                // ========== Estat: Carregant ==========
                llibresState.isLoading || exemplarsState.isLoading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(50.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Carregant catàleg...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // ========== Estat: Error ==========
                llibresState.error != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = llibresState.error!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = {
                            bookViewModel.loadLlibres()
                            bookViewModel.loadExemplars()
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Tornar a intentar")
                        }
                    }
                }

                // ========== Estat: Llista Buida ==========
                llibresState.llibres.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "No hi ha llibres disponibles",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "El catàleg està buit",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // ========== Estat: Mostrar Llibres ==========
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // ========== Barra de Cerca ==========
                        OutlinedTextField(
                            value = searchText,
                            onValueChange = { searchText = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Cerca per títol o autor...") },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null)
                            },
                            trailingIcon = {
                                if (searchText.isNotEmpty()) {
                                    IconButton(onClick = { searchText = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Netejar")
                                    }
                                }
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        Spacer(Modifier.height(8.dp))

                        // ========== Info Header ==========
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (searchText.isBlank()) {
                                    "${llibresState.llibres.size} llibres"
                                } else {
                                    "${filteredBooks.size} resultats"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Botó per refrescar
                            IconButton(onClick = {
                                bookViewModel.loadLlibres()
                                bookViewModel.loadExemplars()
                            }) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Refrescar",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // ========== Llista de Llibres ==========
                        if (filteredBooks.isEmpty()) {
                            // No hi ha resultats de cerca
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.SearchOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "No s'han trobat resultats",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    "Prova amb altres paraules clau",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(filteredBooks) { llibre ->
                                    BookCard(
                                        llibre = llibre,
                                        exemplars = exemplarsState.exemplars,
                                        onClick = { selectedBook = llibre }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ========== Diàleg de Detalls del Llibre ==========
    selectedBook?.let { llibre ->
        BookDetailsDialog(
            llibre = llibre,
            exemplars = exemplarsState.exemplars,
            onDismiss = { selectedBook = null },
            onLoanClick = if (isAdmin || true) { // Permetre a tots (backend validarà)
                { exemplarId ->
                    exemplarToLoan = exemplarId
                    showLoanDialog = true
                }
            } else null
        )
    }

    // ========== Diàleg de Confirmació de Préstec ==========
    if (showLoanDialog && exemplarToLoan != null && currentUserId != null) {
        val exemplar = exemplarsState.exemplars.find { it.id == exemplarToLoan }

        AlertDialog(
            onDismissRequest = {
                showLoanDialog = false
                exemplarToLoan = null
            },
            title = { Text("Confirmar Préstec") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Estàs segur que vols prestar aquest llibre?")
                    Spacer(Modifier.height(4.dp))

                    exemplar?.let {
                        Text(
                            text = "📚 ${it.llibre?.titol ?: "Desconegut"}",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "✍️ ${it.llibre?.autor?.nom ?: "Desconegut"}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "📍 Ubicació: ${it.lloc}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (!isAdmin) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "ℹ️ El préstec es registrarà al teu nom",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Crear préstec
                        loanViewModel.createLoan(
                            usuariId = currentUserId,
                            exemplarId = exemplarToLoan!!
                        )
                    },
                    enabled = !createLoanState.isSubmitting
                ) {
                    if (createLoanState.isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Confirmar")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showLoanDialog = false
                        exemplarToLoan = null
                    },
                    enabled = !createLoanState.isSubmitting
                ) {
                    Text("Cancel·lar")
                }
            }
        )
    }
}

/**
 * Card de llibre amb informació i disponibilitat.
 *
 * **Descripció:**
 * Component reutilitzable que mostra un llibre del catàleg amb
 * tota la informació bibliogràfica i l'estat de disponibilitat
 * basat en els exemplars físics.
 *
 * @param llibre Llibre a mostrar
 * @param exemplars Llista de tots els exemplars per calcular disponibilitat
 * @param onClick Callback quan es fa clic a la card
 *
 * @author Oscar
 * @since 1.0
 */
@Composable
fun BookCard(
    llibre: Llibre,
    exemplars: List<Exemplar>,
    onClick: () -> Unit
) {
    // Filtrar exemplars d'aquest llibre
    val bookExemplars = exemplars.filter { it.llibre?.id == llibre.id }

    // Calcular disponibilitat
    val totalExemplars = bookExemplars.size
    val exemplarsLliures = bookExemplars.count { it.reservat == "lliure" }
    val exemplarsPrestats = bookExemplars.count { it.reservat == "prestat" }
    val exemplarsReservats = bookExemplars.count { it.reservat == "reservat" }

    val hasAvailability = exemplarsLliures > 0

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // ========== Header amb Títol i Badge de Disponibilitat ==========
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = llibre.titol,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(4.dp))

                    // Autor
                    if (llibre.autor != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = llibre.autor.nom,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Badge de disponibilitat
                AvailabilityBadge(
                    hasAvailability = hasAvailability,
                    totalExemplars = totalExemplars
                )
            }

            Spacer(Modifier.height(12.dp))

            HorizontalDivider()

            Spacer(Modifier.height(12.dp))

            // ========== Informació del Llibre ==========
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Editorial
                InfoChip(
                    icon = Icons.Default.Business,
                    text = llibre.editorial
                )

                // Pàgines
                InfoChip(
                    icon = Icons.Default.Description,
                    text = "${llibre.pagines} pàg."
                )
            }

            Spacer(Modifier.height(8.dp))

            // ========== Comptador d'Exemplars ==========
            if (totalExemplars > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Exemplars:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Lliures
                        if (exemplarsLliures > 0) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = "$exemplarsLliures lliure${if (exemplarsLliures != 1) "s" else ""}",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        // Prestats
                        if (exemplarsPrestats > 0) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = "$exemplarsPrestats prestat${if (exemplarsPrestats != 1) "s" else ""}",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }

                        // Reservats
                        if (exemplarsReservats > 0) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    text = "$exemplarsReservats reservat${if (exemplarsReservats != 1) "s" else ""}",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = "Sense exemplars físics",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * Badge d'estat de disponibilitat.
 *
 * **Descripció:**
 * Mostra un badge visual indicant si el llibre està disponible (lliure)
 * o no disponible (tot prestat/reservat).
 *
 * @param hasAvailability Si té exemplars lliures disponibles
 * @param totalExemplars Nombre total d'exemplars del llibre
 *
 * @author Oscar
 * @since 1.0
 */
@Composable
fun AvailabilityBadge(
    hasAvailability: Boolean,
    totalExemplars: Int
) {
    if (totalExemplars == 0) {
        // Sense exemplars
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.small
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Default.Block,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Sense exemplars",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else if (hasAvailability) {
        // Disponible (Lliure)
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = MaterialTheme.shapes.small
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "LLIURE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    } else {
        // No disponible (Prestat/Reservat)
        Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            shape = MaterialTheme.shapes.small
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Default.Cancel,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "PRESTAT",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

/**
 * Chip informatiu petit amb icona i text.
 *
 * **Descripció:**
 * Component per mostrar informació curta del llibre (editorial, pàgines, ISBN).
 *
 * @param icon Icona representativa
 * @param text Text a mostrar
 *
 * @author Oscar
 * @since 1.0
 */
@Composable
fun InfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Diàleg amb detalls complets del llibre.
 *
 * **Descripció:**
 * Mostra tota la informació del llibre incloent l'estat detallat
 * de tots els seus exemplars físics amb ubicació i disponibilitat.
 * Inclou opció per prestar el llibre si hi ha exemplars disponibles.
 *
 * @param llibre Llibre a mostrar
 * @param exemplars Llista de tots els exemplars
 * @param onDismiss Callback per tancar el diàleg
 * @param onLoanClick Callback per iniciar préstec amb l'ID de l'exemplar
 *
 * @author Oscar
 * @since 1.0
 */
@Composable
fun BookDetailsDialog(
    llibre: Llibre,
    exemplars: List<Exemplar>,
    onDismiss: () -> Unit,
    onLoanClick: ((Long) -> Unit)? = null
) {
    // Filtrar exemplars d'aquest llibre
    val bookExemplars = exemplars.filter { it.llibre?.id == llibre.id }
    val hasAvailableExemplars = bookExemplars.any { it.reservat == "lliure" }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = llibre.titol,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (llibre.autor != null) {
                    Text(
                        text = "per ${llibre.autor.nom}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HorizontalDivider()

                // Informació bibliogràfica
                DetailRow("ISBN", llibre.isbn)
                DetailRow("Editorial", llibre.editorial)
                DetailRow("Pàgines", "${llibre.pagines}")

                HorizontalDivider()

                // Exemplars físics
                Text(
                    text = "Exemplars Físics:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                if (bookExemplars.isEmpty()) {
                    Text(
                        text = "Aquest llibre no té exemplars físics disponibles a la biblioteca.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        bookExemplars.forEach { exemplar ->
                            ExemplarItem(
                                exemplar = exemplar,
                                onLoanClick = if (exemplar.reservat == "lliure" && onLoanClick != null) {
                                    { onLoanClick(exemplar.id!!) }
                                } else null
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Tancar")
            }
        }
    )
}

/**
 * Fila de detall amb etiqueta i valor.
 *
 * @param label Etiqueta del camp
 * @param value Valor del camp
 *
 * @author Oscar
 * @since 1.0
 */
@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * Item d'exemplar físic amb estat i ubicació.
 *
 * **Descripció:**
 * Mostra la informació d'un exemplar físic individual:
 * ubicació, estat (lliure/prestat/reservat) i botó per prestar si està lliure.
 *
 * @param exemplar Exemplar a mostrar
 * @param onLoanClick Callback per prestar aquest exemplar (null si no disponible)
 *
 * @author Oscar
 * @since 1.0
 */
@Composable
fun ExemplarItem(
    exemplar: Exemplar,
    onLoanClick: (() -> Unit)? = null
) {
    val (statusColor, statusText, statusIcon) = when (exemplar.reservat) {
        "lliure" -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            "Lliure",
            Icons.Default.CheckCircle
        )
        "prestat" -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            "Prestat",
            Icons.Default.Cancel
        )
        "reservat" -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            "Reservat",
            Icons.Default.BookmarkAdded
        )
        else -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            "Desconegut",
            Icons.Default.Help
        )
    }

    Surface(
        color = statusColor,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = exemplar.lloc,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = "ID: ${exemplar.id}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Badge d'estat o botó de préstec
            if (onLoanClick != null && exemplar.reservat == "lliure") {
                // Botó de préstec per exemplars lliures
                Button(
                    onClick = onLoanClick,
                    modifier = Modifier.height(36.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Prestar",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            } else {
                // Badge d'estat normal
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            statusIcon,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}