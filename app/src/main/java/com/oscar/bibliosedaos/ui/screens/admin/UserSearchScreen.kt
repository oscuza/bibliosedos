package com.oscar.bibliosedaos.ui.screens.admin

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.oscar.bibliosedaos.data.network.User
import com.oscar.bibliosedaos.navigation.AppScreens
import com.oscar.bibliosedaos.ui.screens.profile.UserInfoRow
import com.oscar.bibliosedaos.ui.viewmodels.AuthViewModel

/**
 * Pantalla de cerca d'usuaris per ID o NIF.
 *
 * **Descripció:**
 * Pantalla dedicada que permet a l'administrador cercar usuaris específics
 * mitjançant el seu ID (identificador numèric) o NIF (document d'identitat).
 * No mostra cap usuari fins que l'administrador realitzi activament una cerca.
 *
 * **Funcionalitats:**
 * - 🔢 Cerca per ID (número únic de l'usuari)
 * - 🪪 Cerca per NIF/DNI (document d'identitat)
 * - ✅ Validació en temps real dels camps
 * - 📊 Mostra de resultats amb informació completa
 * - 🔗 Navegació al perfil complet de l'usuari trobat
 * - 🧹 Neteja de cerca per començar una nova
 *
 * **Comportament:**
 * - Pantalla buida inicial amb missatge informatiu
 * - Només mostra resultats després d'una cerca activa
 * - Missatges d'error clars si no es troba l'usuari
 *
 * **Navegació:**
 * - **Entrada:** Des de AdminHomeScreen (card "Cercar Usuari")
 * - **Sortida:** ProfileScreen amb l'userId trobat
 *
 * @param navController Controlador de navegació
 * @param authViewModel ViewModel per gestionar la cerca
 *
 * @author Oscar
 * @since 1.0
 * @see AuthViewModel.searchUserById
 * @see AuthViewModel.searchUserByNif
 * @see AdminHomeScreen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSearchScreen(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    // ========== Estats Locals ==========

    /**
     * Text introduït al camp de cerca.
     */
    var searchQuery by remember { mutableStateOf("") }

    /**
     * Tipus de cerca seleccionat.
     * 0 = Cerca per ID, 1 = Cerca per NIF
     */
    var searchType by remember { mutableIntStateOf(0) }

    // ========== Estats Observables ==========

    /**
     * Estat de la cerca des del ViewModel.
     */
    val searchState by authViewModel.userSearchState.collectAsState()

    /**
     * Estat del login per obtenir l'ID de l'admin.
     */
    val loginState by authViewModel.loginUiState.collectAsState()

    // Flag per prevenir doble clic al botó enrere
    var isNavigating by remember { mutableStateOf(false) }

    // ========== Neteja de Resultats en Entrar ==========

    /**
     * Neteja els resultats de cerca anteriors cada vegada que s'entra a la pantalla.
     * Això assegura que l'admin comenci sempre amb una pantalla neta.
     */
    LaunchedEffect(Unit) {
        authViewModel.clearSearch()
    }

    // ========== UI ==========

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cercar Usuari") },
                navigationIcon = {
                    if (!isNavigating) {
                        IconButton(onClick = {
                            if (!isNavigating) {
                                isNavigating = true
                                authViewModel.clearSearch()
                                // Navegar al perfil de l'admin
                                val adminId = loginState.authResponse?.id ?: 0L
                                navController.navigate(
                                    AppScreens.UserProfileScreen.createRoute(adminId)
                                ) {
                                    popUpTo(AppScreens.UserSearchScreen.route) {
                                        inclusive = true
                                    }
                                    launchSingleTop = true
                                }
                            }
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                "Tornar"
                            )
                        }
                    }
                },
                actions = {
                    // Botó per netejar cerca si s'ha realitzat alguna
                    if (searchState.hasSearched) {
                        IconButton(
                            onClick = {
                                searchQuery = ""
                                authViewModel.clearSearch()
                            }
                        ) {
                            Icon(Icons.Default.Clear, "Netejar cerca")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ========== Card Informativa ==========

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Introdueix un ID o NIF per cercar un usuari específic",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // ========== Selector de Tipus de Cerca ==========

            Text(
                text = "Tipus de cerca",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Tabs per seleccionar tipus de cerca
            TabRow(selectedTabIndex = searchType) {
                Tab(
                    selected = searchType == 0,
                    onClick = {
                        searchType = 0
                        searchQuery = ""
                        authViewModel.clearSearch()
                    },
                    text = { Text("Per ID") },
                    icon = { Icon(Icons.Default.Numbers, null) }
                )
                Tab(
                    selected = searchType == 1,
                    onClick = {
                        searchType = 1
                        searchQuery = ""
                        authViewModel.clearSearch()
                    },
                    text = { Text("Per NIF") },
                    icon = { Icon(Icons.Default.Badge, null) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ========== Camp de Cerca ==========

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(if (searchType == 0) "ID de l'usuari" else "NIF de l'usuari")
                },
                placeholder = {
                    Text(if (searchType == 0) "Exemple: 42" else "Exemple: 12345678A")
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, "Cercar")
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, "Esborrar")
                        }
                    }
                },
                singleLine = true,
                enabled = !searchState.isSearching
            )

            // ========== Botó de Cerca ==========

            Button(
                onClick = {
                    if (searchType == 0) {
                        authViewModel.searchUserById(searchQuery)
                    } else {
                        authViewModel.searchUserByNif(searchQuery)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !searchState.isSearching && searchQuery.isNotBlank()
            ) {
                if (searchState.isSearching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Search, null)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (searchState.isSearching) "Cercant..." else "Cercar")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ========== Resultats de la Cerca ==========

            when {
                // Estat: Cercant
                searchState.isSearching -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = "Cercant usuari...",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }

                // Estat: Error
                searchState.error != null -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "No s'ha trobat",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            Text(
                                text = searchState.error!!,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Estat: Usuari Trobat
                searchState.searchResult != null -> {
                    val user = searchState.searchResult!!

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Header
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Usuari trobat",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }

                            HorizontalDivider()

                            // Informació de l'usuari
                            UserSearchResultCard(user = user)

                            // Botó per veure perfil complet
                            Button(
                                onClick = {
                                    navController.navigate(
                                        AppScreens.UserProfileScreen.createRoute(user.id)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Visibility, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Veure Perfil Complet")
                            }
                        }
                    }
                }

                // Estat: Inicial (no s'ha cercat res)
                !searchState.hasSearched -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                Icons.Default.PersonSearch,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "Introdueix ${if (searchType == 0) "un ID" else "un NIF"} i prem Cercar",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Card que mostra la informació resumida d'un usuari trobat.
 *
 * @param user Usuari a mostrar
 *
 * @author Oscar
 * @since 1.0
 */
@Composable
private fun UserSearchResultCard(user: User) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Nick amb badge de rol
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                if (user.rol == 2) Icons.Default.AdminPanelSettings
                else Icons.Default.Person,
                contentDescription = null,
                tint = if (user.rol == 2)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.primary
            )
            Text(
                text = user.nick,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            if (user.rol == 2) {
                Badge(
                    containerColor = MaterialTheme.colorScheme.error
                ) {
                    Text("Admin")
                }
            }
        }

        HorizontalDivider()

        // Informació detallada (utilitza UserInfoRow de UserProfileScreen)
        UserInfoRow("ID", user.id.toString())
        UserInfoRow("Nom", user.nom)
        UserInfoRow("Cognoms", "${user.cognom1 ?: ""} ${user.cognom2 ?: ""}".trim())
        user.nif?.let { UserInfoRow("NIF", it) }
        user.email?.let { UserInfoRow("Email", it) }
        user.tlf?.let { UserInfoRow("Telèfon", it) }
        UserInfoRow("Rol", if (user.rol == 2) "Administrador" else "Usuari")
    }
}