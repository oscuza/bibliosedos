package com.oscar.bibliosedaos.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.oscar.bibliosedaos.data.network.User
import com.oscar.bibliosedaos.navigation.AppScreens
import com.oscar.bibliosedaos.ui.viewmodels.AuthViewModel

/**
 * Pantalla principal d'administració d'usuaris.
 *
 * **Accés:**
 * Només accessible per usuaris amb rol d'administrador (rol=2).
 * És el centre de control per gestionar tots els usuaris del sistema.

 *
 * **Característiques de Seguretat:**
 * - Impedeix que l'administrador s'elimini a si mateix
 * - Mostra distintiu "Tu" a la pròpia card
 * - Deshabilita el botó d'eliminar per al propi usuari
 * - Diàleg de confirmació abans d'eliminar
 *
 *
 * @param navController Controlador de navegació per a transicions entre pantalles
 * @param authViewModel ViewModel per gestionar operacions d'usuaris
 *
 * @author Oscar
 * @since 1.0
 * @see AuthViewModel.loadAllUsers
 * @see AuthViewModel.deleteUser
 * @see AddUserScreen
 * @see UserProfileScreen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHomeScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
) {
    // ========== Estats Observables ==========

    /**
     * Estat de la llista d'usuaris.
     * Conté: isLoading, users (List<User>), error
     */
    val userListState by authViewModel.userListState.collectAsState()

    /**
     * Estat del login per obtenir l'ID de l'admin actual.
     * Necessari per evitar l'auto-eliminació.
     */
    val loginState by authViewModel.loginUiState.collectAsState()

    // Context d'Android per mostrar Toasts
    val context = LocalContext.current

    // ========== Estats Locals per Diàlegs ==========

    /**
     * Controla la visibilitat del diàleg de confirmació d'eliminació.
     */
    var showDeleteDialog by remember { mutableStateOf(false) }

    /**
     * Controla la visibilitat de l'advertència d'auto-eliminació.
     * (Actualment no s'utilitza, es mostra Toast directament)
     */
    var showSelfDeleteWarning by remember { mutableStateOf(false) }

    /**
     * Usuari que s'està a punt d'eliminar.
     * null si no hi ha cap operació d'eliminació en curs.
     */
    var userToDelete by remember { mutableStateOf<User?>(null) }

    /**
     * ID de l'administrador que està utilitzant l'app.
     * S'utilitza per protecció contra auto-eliminació.
     */
    val currentAdminId = loginState.authResponse?.id


    // Obtenir el propietari del cicle de vida
    val lifecycleOwner = LocalLifecycleOwner.current

    /**
     * Carrega la llista d'usuaris CADA COP que la pantalla
     * es mostra (es posa en ON_RESUME).
     *
     * Fem servir DisposableEffect per afegir un observador al cicle de vida
     * i netejar-lo (onDispose) quan la pantalla es destrueix.
     */

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                authViewModel.loadAllUsers()
            }
        }

        // Afegir l'observador
        lifecycleOwner.lifecycle.addObserver(observer)

        // Netejar l'observador quan la pantalla es destrueix (onDispose)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }


    // ========== Diàleg de Confirmació d'Eliminació ==========

    /**
     * Diàleg modal que demana confirmació abans d'eliminar un usuari.
     * Mostra el nick de l'usuari a eliminar.
     */
    if (showDeleteDialog && userToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirmar Eliminació") },
            text = {
                Text("Estàs segur d'eliminar l'usuari '${userToDelete?.nick}'?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        userToDelete?.let { user ->
                            authViewModel.deleteUser(user.id) { success, message ->
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            }
                        }
                        showDeleteDialog = false
                        userToDelete = null
                    }
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel·lar")
                }
            }
        )
    }

    // ========== UI Principal ==========

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestió d'Usuaris") },
                navigationIcon = {
                    /**
                     * Botó per tornar enrere (a ProfileScreen).
                     */
                    IconButton(onClick = {
                        navController.popBackStack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Tornar enrere"
                        )
                    }
                },
                actions = {
                    /**
                     * Botó de logout a la barra superior.
                     * Neteja el token i navega al login.
                     */
                    IconButton(onClick = {
                        authViewModel.logout()
                        navController.navigate(AppScreens.LoginScreen.route) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, "Tancar Sessió")
                    }
                }
            )
        },
        floatingActionButton = {
            /**
             * Botó d'acció flotant per afegir nous usuaris.
             * Navega a AddUserScreen.
             */
            FloatingActionButton(
                onClick = {
                    navController.navigate(AppScreens.AddUserScreen.route)
                }
            ) {
                Icon(Icons.Default.Add, "Afegir Usuari")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                // ========== Estat: Carregant ==========
                userListState.isLoading -> {
                    /**
                     * Indicador de càrrega centrat mentre es carreguen els usuaris.
                     */
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(50.dp)
                            .align(Alignment.Center)
                    )
                }

                // ========== Estat: Error ==========
                userListState.error != null -> {
                    /**
                     * Missatge d'error amb icona quan falla la càrrega.
                     */
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
                            text = userListState.error!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                // ========== Estat: Llista Buida ==========
                userListState.users.isEmpty() -> {
                    /**
                     * Missatge quan no hi ha usuaris registrats.
                     */
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.PersonOff,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "No hi ha usuaris registrats",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                // ========== Estat: Llista amb Usuaris ==========
                else -> {
                    /**
                     * LazyColumn amb tots els usuaris.
                     * Inclou header amb comptador i cards per cada usuari.
                     */
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Header amb comptador d'usuaris
                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.People,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        "Total: ${userListState.users.size} usuaris",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        // ========== Card de Cerca d'Usuaris ==========
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        navController.navigate(AppScreens.UserSearchScreen.route)
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Search,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Column {
                                            Text(
                                                "Cercar Usuari",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer
                                            )
                                            Text(
                                                "Per ID o NIF",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(
                                                    alpha = 0.7f
                                                )
                                            )
                                        }
                                    }
                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = "Anar a cerca",
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                        }

                        // Llista de cards d'usuaris
                        items(userListState.users) { user ->
                            AdminUserCard(
                                user = user,
                                canDelete = user.id != currentAdminId,
                                onDeleteClick = {
                                    if (user.id == currentAdminId) {
                                        Toast.makeText(
                                            context,
                                            "No pots eliminar-te a tu mateix",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    } else {
                                        userToDelete = user
                                        showDeleteDialog = true
                                    }
                                },
                                onCardClick = {
                                    navController.navigate(
                                        AppScreens.UserProfileScreen.createRoute(user.id)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Card d'usuari per a la vista d'administrador.
 *
 * Component reutilitzable que mostra la informació d'un usuari en format card.
 * Adapta el seu estil i funcionalitat segons si és l'admin actual o un altre usuari.
 *
 * @param user Dades de l'usuari a mostrar
 * @param canDelete Si es permet eliminar aquest usuari (false per al propi admin)
 * @param onDeleteClick Callback que s'executa en fer clic al botó d'eliminar
 * @param onCardClick Callback que s'executa en fer clic a la card
 *
 * @author Oscar
 * @since 1.0
 * @see User
 * @see AdminHomeScreen
 */
@Composable
fun AdminUserCard(
    user: User,
    canDelete: Boolean,
    onDeleteClick: () -> Unit,
    onCardClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ========== Avatar amb Icona segons Rol ==========

            /**
             * Surface circular que conté la icona de l'usuari.
             * Color i icona segons el rol.
             */
            Surface(
                modifier = Modifier.size(48.dp),
                shape = MaterialTheme.shapes.medium,
                color = if (user.rol == 2) // Admin
                    MaterialTheme.colorScheme.errorContainer
                else
                    MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (user.rol == 2) Icons.Default.AdminPanelSettings
                        else Icons.Default.Person,
                        contentDescription = null,
                        tint = if (user.rol == 2)
                            MaterialTheme.colorScheme.onErrorContainer
                        else
                            MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            // ========== Informació de l'Usuari ==========

            /**
             * Columna amb nick, nom complet i rol.
             */
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Nick de l'usuari
                    Text(
                        text = user.nick,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Badge "Tu" si és l'usuari actual
                    if (!canDelete) {
                        Spacer(Modifier.width(8.dp))
                        Badge(
                            containerColor = MaterialTheme.colorScheme.tertiary
                        ) {
                            Text(
                                "Tu",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Nom complet
                Text(
                    text = "${user.nom} ${user.cognom1 ?: ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Rol amb color distintiu
                Text(
                    text = if (user.rol == 2) "Administrador" else "Usuari",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (user.rol == 2)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary
                )
            }

            // ========== Botó Eliminar ==========

            /**
             * Botó d'eliminació que es deshabilita per al propi usuari.
             * Mostra icona de paperera en vermell si està habilitat.
             */
            IconButton(
                onClick = onDeleteClick,
                enabled = canDelete
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = if (canDelete) "Eliminar" else "No pots eliminar-te",
                    tint = if (canDelete)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }
        }
    }
}