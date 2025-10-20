package com.oscar.bibliosedaos.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.oscar.bibliosedaos.data.network.TokenManager
import com.oscar.bibliosedaos.navigation.AppScreens
import com.oscar.bibliosedaos.ui.viewmodels.AuthViewModel
import kotlinx.coroutines.delay

/**
 * Pantalla unificada de perfil d'usuari.
 *
 * **Descripció:**
 * Pantalla principal més completa de l'aplicació que s'adapta dinàmicament
 * segons el rol de l'usuari i si està veient el seu propi perfil o el d'un altre.
 *
 * @param userId ID de l'usuari el perfil del qual es mostrarà
 * @param navController Controlador de navegació per a transicions
 * @param authViewModel ViewModel per gestionar dades d'usuari
 *
 * @author Oscar
 * @since 1.0
 * @see User
 * @see AuthViewModel.loadUserProfile
 * @see EditProfileScreen
 * @see AdminHomeScreen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userId: Long,
    navController: NavController,
    authViewModel: AuthViewModel,
) {
    // ========== Estats Observables ==========

    /**
     * DNI de prova (no utilitzat en aquesta pantalla).
     * Mantingut per compatibilitat amb el ViewModel.
     */
    val midni by authViewModel.midni.collectAsState()

    /**
     * Estat del perfil de l'usuari.
     * Conté: isLoading, user, error
     */
    val userProfileState by authViewModel.userProfileState.collectAsState()

    /**
     * Estat del login per obtenir l'ID de l'usuari actual.
     */
    val loginState by authViewModel.loginUiState.collectAsState()

    val context = LocalContext.current

    // ========== Detecció de Perfil Propi ==========

    /**
     * ID de l'usuari actualment autenticat.
     */
    val currentUserId = loginState.authResponse?.id

    /**
     * Determina si s'està veient el propi perfil.
     * true = perfil propi (no mostrar botó "Volver")
     * false = perfil d'un altre usuari (mostrar botó "Volver")
     */
    val isViewingOwnProfile = currentUserId == userId

    // ========== Càrrega del Perfil ==========

    /**
     * Carrega el perfil de l'usuari en iniciar la pantalla.
     * S'executa cada vegada que canvia userId.
     *
     * **Procés:**
     * 1. Delay de 200ms per assegurar que el token està llest
     * 2. Verificar si existeix token
     * 3. Si existeix: carregar perfil
     * 4. Si no existeix: navegar a login
     */
    LaunchedEffect(key1 = userId) {
        delay(200) // Assegurar que el token està llest
        if (TokenManager.hasToken()) {
            android.util.Log.d("ProfileScreen", "Carregant perfil de l'usuari: $userId")
            authViewModel.loadUserProfile(userId)
        } else {
            android.util.Log.e("ProfileScreen", "No hi ha token disponible")
            // Navegar a login si no hi ha token
            navController.navigate(AppScreens.LoginScreen.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    // ========== UI ==========

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (userProfileState.user?.rol == 2) "Panell d'Administrador"
                        else "El Meu Perfil"
                    )
                },
                navigationIcon = {
                    /**
                     * Botó "Volver" només si NO és el propi perfil.
                     * Permet als admins tornar a la llista després de veure
                     * el perfil d'un altre usuari.
                     */
                    if (!isViewingOwnProfile) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Tornar"
                            )
                        }
                    }
                },
                actions = {
                    /**
                     * Botó de logout sempre visible.
                     * Neteja el token i navega al login.
                     */
                    IconButton(onClick = {
                        authViewModel.logout()
                        navController.navigate(AppScreens.LoginScreen.route) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    }) {
                        Icon(Icons.Default.ExitToApp, "Tancar Sessió")
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
            when {
                // ========== Estat: Carregant ==========
                userProfileState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(50.dp)
                            .align(Alignment.Center)
                    )
                }

                // ========== Estat: Error ==========
                userProfileState.error != null -> {
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
                            text = userProfileState.error!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                // ========== Estat: Perfil Carregat ==========
                userProfileState.user != null -> {
                    val user = userProfileState.user!!
                    val isAdmin = user.rol == 2  // Admin és 2

                    /**
                     * Columna scrollable amb tot el contingut del perfil.
                     */
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // ========== Card de Perfil ==========

                        /**
                         * Card amb informació bàsica de l'usuari.
                         * Mostra: avatar, nick, nom complet, badge rol, ID
                         */
                        ProfileCard(
                            nick = user.nick,
                            nombre = user.nombre,
                            apellido1 = user.apellido1 ?: "",
                            apellido2 = user.apellido2 ?: "",
                            userId = user.id,
                            isAdmin = isAdmin
                        )

                        // ========== Secció de Gestió de Compte ==========

                        /**
                         * Opcions de gestió del compte (comunes per tots).
                         * Inclou: Editar Perfil, Canviar Contrasenya (TODO)
                         */
                        SectionCard(
                            title = "El Meu Compte",
                            icon = Icons.Default.Person
                        ) {
                            OptionItem(
                                icon = Icons.Default.Edit,
                                title = "Editar Perfil",
                                subtitle = "Modificar dades personals",
                                onClick = {
                                    navController.navigate(AppScreens.EditProfileScreen.route)
                                }
                            )

                            Divider()

                            OptionItem(
                                icon = Icons.Default.Lock,
                                title = "Canviar Contrasenya",
                                subtitle = "Actualitzar contrasenya d'accés",
                                onClick = {
                                    // TODO: REQ 1 - Implementar ChangePasswordScreen
                                    Toast.makeText(
                                        context,
                                        "Funcionalitat pendent d'implementar",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
                        }

                        // ========== Opcions segons Rol ==========

                        if (isAdmin) {
                            /**
                             * Opcions específiques per administradors.
                             */
                            AdminOptionsSection(navController, context)
                        } else {
                            /**
                             * Opcions específiques per usuaris normals.
                             */
                            UserOptionsSection(navController)
                        }

                        // ========== Catàleg de Llibres (Comú) ==========

                        /**
                         * Secció compartida per explorar el catàleg.
                         * Accessible per tots els usuaris autenticats.
                         */
                        SectionCard(
                            title = "Explorar Llibres",
                            icon = Icons.Default.LibraryBooks
                        ) {
                            OptionItem(
                                icon = Icons.Default.Search,
                                title = "Catàleg Complet",
                                subtitle = "Buscar i consultar tots els llibres",
                                onClick = {
                                    // TODO: REQ 7 - Implementar BooksListScreen
                                    Toast.makeText(
                                        context,
                                        "Funcionalitat pendent d'implementar",
                                        Toast.LENGTH_SHORT
                                    ).show()
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
 * Secció d'opcions administratives.
 *
 * **Descripció:**
 * Component que mostra les opcions disponibles només per administradors.
 * Inclou gestió d'usuaris i accés al panell web complet.
 *
 * @param navController Controlador de navegació per a transicions
 * @param context Context d'Android per llançar intents (obrir navegador)
 *
 * @author Oscar
 * @since 1.0
 * @see AdminHomeScreen
 * @see AddUserScreen
 */
@Composable
fun AdminOptionsSection(navController: NavController, context: android.content.Context) {
    // ========== Missatge Informatiu ==========

    /**
     * Card amb informació sobre la funcionalitat limitada
     * de l'app mòbil per administradors.
     */
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    "App mòbil per usuaris",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    "Funcionalitat limitada per administradors. Per gestió completa, usar aplicació escriptori.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }

    // ========== Gestió d'Usuaris ==========

    /**
     * Secció amb opcions CRUD d'usuaris.
     * Color d'error per destacar que són opcions d'administrador.
     */
    SectionCard(
        title = "Gestió d'Usuaris",
        icon = Icons.Default.People,
        containerColor = MaterialTheme.colorScheme.errorContainer
    ) {
        OptionItem(
            icon = Icons.Default.List,
            title = "Veure Tots els Usuaris",
            subtitle = "Llista completa d'usuaris registrats",
            onClick = {
                navController.navigate(AppScreens.AdminHomeScreen.route)
            }
        )

        Divider()

        OptionItem(
            icon = Icons.Default.Search,
            title = "Buscar Usuari",
            subtitle = "Buscar per nick o nom",
            onClick = {
                // TODO: REQ 2 - Implementar SearchUserScreen
                Toast.makeText(
                    context,
                    "Funcionalitat pendent d'implementar",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )

        Divider()

        OptionItem(
            icon = Icons.Default.PersonAdd,
            title = "Afegir Usuari",
            subtitle = "Registrar nou usuari",
            onClick = {
                navController.navigate(AppScreens.AddUserScreen.route)
            }
        )
    }

    // ========== Panel d'Administració Completa ==========

    /**
     * Secció amb enllaç al panell web.
     * Obre el navegador amb l'URL del panell d'administració.
     */
    SectionCard(
        title = "Administració Completa",
        icon = Icons.Default.Computer
    ) {
        OptionItem(
            icon = Icons.Default.OpenInBrowser,
            title = "Obrir Panell Web",
            subtitle = "Accedir a totes les funcions administratives",
            onClick = {
                val intent =
                    Intent(Intent.ACTION_VIEW, Uri.parse("http://localhost:8080/admin"))
                context.startActivity(intent)
            }
        )
    }
}

/**
 * Secció d'opcions per usuaris normals.
 *
 * **Descripció:**
 * Component que mostra les funcionalitats completes disponibles per
 * usuaris normals: préstecs, reserves, ressenyes, notificacions, etc.
 *
 * **Funcionalitats Planificades:**
 *
 * **1. Préstecs:**
 * - Préstecs Actius: Llibres actualment prestats
 * - Reserves: Llibres reservats pendents de recollir
 * - Historial: Préstecs anteriors
 *
 * **2. Ressenyes:**
 * - Meves Ressenyes: Valoracions escrites
 * - Llibres Favorits: Llibres marcats com a favorits
 *
 * **3. Estat de Compte:**
 * - Sancions: Sancions actives i historial
 * - Dates Límit: Pròximes devolucions
 *
 * **4. Notificacions:**
 * - Configurar: Preferències de notificacions per email
 * - Meus Avisos: Notificacions rebudes
 *
 * **Nota:** Totes les funcionalitats estan pendents d'implementar (TODO).
 *
 * @param navController Controlador de navegació per a transicions
 *
 * @author Oscar
 * @since 1.0
 */
@Composable
fun UserOptionsSection(navController: NavController) {
    val context = LocalContext.current

    // ========== Gestió de Préstecs ==========

    SectionCard(
        title = "Els Meus Préstecs",
        icon = Icons.Default.MenuBook,
        containerColor = MaterialTheme.colorScheme.primaryContainer
    ) {
        OptionItem(
            icon = Icons.Default.Book,
            title = "Préstecs Actius",
            subtitle = "Veure llibres que tinc prestats",
            onClick = {
                // TODO: REQ 4 - Implementar MyLoansScreen
                Toast.makeText(context, "Funcionalitat pendent", Toast.LENGTH_SHORT).show()
            }
        )

        Divider()

        OptionItem(
            icon = Icons.Default.Bookmark,
            title = "Les Meves Reserves",
            subtitle = "Veure llibres reservats",
            onClick = {
                // TODO: REQ 4 - Implementar MyReservationsScreen
                Toast.makeText(context, "Funcionalitat pendent", Toast.LENGTH_SHORT).show()
            }
        )

        Divider()

        OptionItem(
            icon = Icons.Default.History,
            title = "Historial de Préstecs",
            subtitle = "Veure préstecs anteriors",
            onClick = {
                // TODO: REQ 4 - Implementar LoanHistoryScreen
                Toast.makeText(context, "Funcionalitat pendent", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // ========== Sistema de Ressenyes ==========

    SectionCard(
        title = "Les Meves Ressenyes",
        icon = Icons.Default.Star
    ) {
        OptionItem(
            icon = Icons.Default.RateReview,
            title = "Les Meves Ressenyes",
            subtitle = "Veure i editar ressenyes escrites",
            onClick = {
                // TODO: REQ 6 - Implementar MyReviewsScreen
                Toast.makeText(context, "Funcionalitat pendent", Toast.LENGTH_SHORT).show()
            }
        )

        Divider()

        OptionItem(
            icon = Icons.Default.StarBorder,
            title = "Llibres Favorits",
            subtitle = "Veure llibres que m'han agradat",
            onClick = {
                // TODO: REQ 6 - Implementar FavoriteBooksScreen
                Toast.makeText(context, "Funcionalitat pendent", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // ========== Estat de Compte ==========

    SectionCard(
        title = "Estat del Meu Compte",
        icon = Icons.Default.Warning
    ) {
        OptionItem(
            icon = Icons.Default.Info,
            title = "Estat de Sancions",
            subtitle = "Veure si tinc sancions actives",
            onClick = {
                // TODO: REQ 5 - Implementar MySanctionsScreen
                Toast.makeText(context, "Funcionalitat pendent", Toast.LENGTH_SHORT).show()
            }
        )

        Divider()

        OptionItem(
            icon = Icons.Default.EventAvailable,
            title = "Data Límit de Devolucions",
            subtitle = "Veure dates d'entrega pròximes",
            onClick = {
                // TODO: REQ 4 - Mostrar a MyLoansScreen
                Toast.makeText(context, "Funcionalitat pendent", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // ========== Notificacions ==========

    SectionCard(
        title = "Notificacions",
        icon = Icons.Default.Notifications
    ) {
        OptionItem(
            icon = Icons.Default.Email,
            title = "Configurar Notificacions",
            subtitle = "Rebre avisos per email",
            onClick = {
                // TODO: REQ 8 - Implementar NotificationSettingsScreen
                Toast.makeText(context, "Funcionalitat pendent", Toast.LENGTH_SHORT).show()
            }
        )

        Divider()

        OptionItem(
            icon = Icons.Default.NotificationImportant,
            title = "Els Meus Avisos",
            subtitle = "Veure notificacions rebudes",
            onClick = {
                // TODO: REQ 8 - Implementar MyNotificationsScreen
                Toast.makeText(context, "Funcionalitat pendent", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

/**
 * Component reutilitzable per seccions d'opcions.
 *
 * **Descripció:**
 * Card que agrupa opcions relacionades sota un títol i icona comú.
 * Utilitzat per organitzar visualment les diferents seccions del perfil.
 *
 *
 * @param title Títol de la secció
 * @param icon Icona representativa de la secció
 * @param containerColor Color de fons del card (per defecte: surface)
 * @param content Contingut composable de la secció (OptionItems)
 *
 * @author Oscar
 * @since 1.0
 * @see OptionItem
 */
@Composable
fun SectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            /**
             * Header de la secció amb icona i títol.
             */
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Divider()

            /**
             * Contingut de la secció (OptionItems).
             */
            content()
        }
    }
}

/**
 * Card de perfil d'usuari.
 *
 * **Descripció:**
 * Component que mostra la informació bàsica i distintius de l'usuari
 *
 * @param nick Nom d'usuari
 * @param nombre Nom real
 * @param apellido1 Primer cognom
 * @param apellido2 Segon cognom
 * @param userId ID de l'usuari
 * @param isAdmin Si l'usuari és administrador
 *
 * @author Oscar
 * @since 1.0
 */
@Composable
fun ProfileCard(
    nick: String,
    nombre: String,
    apellido1: String,
    apellido2: String,
    userId: Long,
    isAdmin: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isAdmin)
                MaterialTheme.colorScheme.errorContainer
            else
                MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            /**
             * Icona gran de l'usuari.
             */
            Icon(
                if (isAdmin) Icons.Default.AdminPanelSettings else Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = if (isAdmin)
                    MaterialTheme.colorScheme.onErrorContainer
                else
                    MaterialTheme.colorScheme.onPrimaryContainer
            )

            /**
             * Nick de l'usuari (destacat).
             */
            Text(
                text = nick,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (isAdmin)
                    MaterialTheme.colorScheme.onErrorContainer
                else
                    MaterialTheme.colorScheme.onPrimaryContainer
            )

            /**
             * Nom complet.
             */
            Text(
                text = "$nombre $apellido1 $apellido2".trim(),
                style = MaterialTheme.typography.bodyLarge,
                color = if (isAdmin)
                    MaterialTheme.colorScheme.onErrorContainer
                else
                    MaterialTheme.colorScheme.onPrimaryContainer
            )

            /**
             * Badge amb el rol.
             */
            Badge(
                containerColor = if (isAdmin)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.primary
            ) {
                Text(
                    text = if (isAdmin) "ADMINISTRADOR" else "USUARI",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            /**
             * ID de l'usuari.
             */
            Text(
                text = "ID: $userId",
                style = MaterialTheme.typography.bodySmall,
                color = if (isAdmin)
                    MaterialTheme.colorScheme.onErrorContainer
                else
                    MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

/**
 * Item d'opció clicable amb icona i descripció.
 *
 * **Descripció:**
 * Component reutilitzable per opcions dins de SectionCard.
 * Mostra icona, títol, subtítol i fletxa indicadora.
 *
 * @param icon Icona de l'opció
 * @param title Títol de l'opció
 * @param subtitle Descripció breu de l'opció
 * @param onClick Acció a executar en fer clic
 *
 * @author Oscar
 * @since 1.0
 */
@Composable
fun OptionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = {
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        },
        leadingContent = {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingContent = {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}