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
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.oscar.bibliosedaos.data.network.TokenManager
import com.oscar.bibliosedaos.navigation.AppScreens
import com.oscar.bibliosedaos.ui.viewmodels.AuthViewModel
import kotlinx.coroutines.delay
import com.oscar.bibliosedaos.data.network.User

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

    val currentUserIsAdmin = (loginState.authResponse?.rol == 2)

    // Variable per gestionar el diàleg de restablir contrasenya (per admins)
    var showResetPasswordDialog by remember { mutableStateOf(false) }

    // Variable per gestionar el diàleg d'eliminar usuari (per admins)
    var showDeleteUserDialog by remember { mutableStateOf(false) }

    //Variable per gestionar el diàleg d'actualitzar usuari (per admins)
    var showUpdateUserDialog by remember { mutableStateOf(false) }
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
            TopAppBar(title = {
                Text(
                    if (userProfileState.user?.rol == 2) "Panell d'Administrador"
                    else "El Meu Perfil"
                )
            }, navigationIcon = {
                /**
                 * Botó "Volver" només si NO és el propi perfil.
                 * Permet als admins tornar a la llista després de veure
                 * el perfil d'un altre usuari.
                 */
                if (!isViewingOwnProfile) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Tornar"
                        )
                    }
                }
            }, actions = {
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
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, "Tancar Sessió")
                }
            })
        }) { paddingValues ->
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
                            .padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // ========== Card de Perfil ==========

                        /**
                         * Card amb informació bàsica de l'usuari.
                         * Mostra: avatar, nick, nom complet, badge rol, ID
                         */
                        ProfileCard(
                            nick = user.nick,
                            nombre = user.nom,
                            apellido1 = user.cognom1 ?: "",
                            apellido2 = user.cognom2 ?: "",
                            userId = user.id,
                            isAdmin = isAdmin
                        )

                        // ========== Secció de Gestió de Compte ==========

                        /**
                         * Opcions de gestió del compte (comunes per tots).
                         * Inclou: Editar Perfil, Canviar Contrasenya
                         */
                        SectionCard(
                            title = "El Meu Compte", icon = Icons.Default.Person
                        ) {
                            OptionItem(
                                icon = Icons.Default.Edit,
                                title = "Editar Perfil",
                                subtitle = "Modificar dades personals",
                                onClick = {
                                    navController.navigate(AppScreens.EditProfileScreen.route)
                                })

                            HorizontalDivider()

                            OptionItem(
                                icon = Icons.Default.Lock,
                                title = "Canviar Contrasenya",
                                subtitle = "Actualitzar contrasenya d'accés",
                                onClick = {
                                    // Navegar a la pantalla de canviar contrasenya
                                    navController.navigate("change_password")
                                })
                        }

                        // ========== Botó Restablir Contrasenya (només per admins veient altres perfils) ==========
                        if (currentUserIsAdmin && !isViewingOwnProfile) {
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedButton(
                                onClick = {
                                    showResetPasswordDialog = true
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 0.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.tertiary
                                ),
                                border = ButtonDefaults.outlinedButtonBorder
                            ) {
                                Icon(
                                    Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("RESTABLIR CONTRASENYA D'AQUEST USUARI")
                            }

                            // ========== Botó Eliminar Usuari (només per admins veient altres perfils) ==========

                                                     OutlinedButton(
                                onClick = {
                                    showDeleteUserDialog = true
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 0.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(
                                        MaterialTheme.colorScheme.error
                                    )
                                )
                            ) {
                                Icon(
                                    Icons.Default.DeleteForever,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )

                                Text("ELIMINAR AQUEST USUARI")
                            }

                            //  NOU BOTÓ: EDITAR DADES D'AQUEST USUARI
                            OutlinedButton(
                                onClick = {
                                    // 1. L'usuari ja està carregat en userProfileState.user

                                    // 2. NAVEGAR DIRECTAMENT A LA PANTALLA D'EDICIÓ
                                    navController.navigate(AppScreens.EditProfileScreen.route)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 0.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary // Utilitzar color primari
                                ),
                                border = ButtonDefaults.outlinedButtonBorder
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )

                                Text("EDITAR DADES D'AQUEST USUARI")
                            }
                        }

                        // ========== Seccions segons el rol de l'usuari ==========
                        /**
                         * Mostra opcions específiques segons si és admin o usuari normal.
                         */
                        if (currentUserIsAdmin) {
                            // Seccions per administradors
                            AdminUserSection(
                                navController = navController,
                                context = context,
                                onDeleteClick = {
                                    showDeleteUserDialog = true
                                },// Passar el callback
                                onUpdateClick = {
                                    showUpdateUserDialog = true
                                }//Passar el callback per actualitzar
                            )
                        } else {
                            // Seccions per usuaris normals
                            NormalUserSection(navController, context)
                        }
                    }
                }
            }
        }
    }

    // ========== Diàleg per Restablir Contrasenya (Admins) ==========
    if (showResetPasswordDialog) {
        var newPassword by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }

        AlertDialog(onDismissRequest = { showResetPasswordDialog = false }, title = {
            Text("Restablir Contrasenya")
        }, text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Estàs a punt de restablir la contrasenya de l'usuari ${userProfileState.user?.nick}",
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("Nova Contrasenya") },
                    visualTransformation = PasswordVisualTransformation(),
                    isError = newPassword.isNotEmpty() && newPassword.length < 6,
                    supportingText = {
                        if (newPassword.isNotEmpty() && newPassword.length < 6) {
                            Text("Mínim 6 caràcters")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirmar Contrasenya") },
                    visualTransformation = PasswordVisualTransformation(),
                    isError = confirmPassword.isNotEmpty() && confirmPassword != newPassword,
                    supportingText = {
                        if (confirmPassword.isNotEmpty() && confirmPassword != newPassword) {
                            Text("Les contrasenyes no coincideixen")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }, confirmButton = {
            TextButton(
                onClick = {
                    if (newPassword.length >= 6 && newPassword == confirmPassword) {
                        authViewModel.resetUserPassword(
                            userId = userId,
                            newPassword = newPassword,
                            onResult = { success, message ->
                                if (success) {
                                    showResetPasswordDialog = false
                                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                }
                            })
                    }
                }, enabled = newPassword.length >= 6 && newPassword == confirmPassword
            ) {
                Text("RESTABLIR")
            }
        }, dismissButton = {
            TextButton(onClick = { showResetPasswordDialog = false }) {
                Text("CANCEL·LAR")
            }
        })
    }

    // ========== Diàleg d'Eliminació d'Usuari (Admins) ==========
    if (showDeleteUserDialog) {
        var nifToDelete by remember { mutableStateOf("") }
        var confirmationText by remember { mutableStateOf("") }
        var userFound by remember { mutableStateOf<User?>(null) }
        var isSearching by remember { mutableStateOf(false) }
        var searchError by remember { mutableStateOf<String?>(null) }
        var showConfirmation by remember { mutableStateOf(false) }
        val requiredText = "ELIMINAR"

        // Observar l'estat de cerca
        val userSearchState by authViewModel.userSearchState.collectAsState()

        if (!showConfirmation) {
            // PRIMER PAS: Demanar NIF de l'usuari a eliminar
            AlertDialog(onDismissRequest = {
                showDeleteUserDialog = false
                nifToDelete = ""
                userFound = null
                searchError = null
            }, icon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            }, title = {
                Text(
                    "Eliminar Usuari", fontWeight = FontWeight.Bold
                )
            }, text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Introdueix el NIF/DNI de l'usuari que vols eliminar:",
                        style = MaterialTheme.typography.bodyLarge
                    )

                    OutlinedTextField(
                        value = nifToDelete,
                        onValueChange = {
                            nifToDelete = it.uppercase()
                            searchError = null
                        },
                        label = { Text("NIF/DNI") },
                        placeholder = { Text("12345678A") },
                        isError = searchError != null,
                        supportingText = {
                            searchError?.let {
                                Text(
                                    it, color = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isSearching
                    )

                    if (isSearching) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Mostrar informació de l'usuari trobat
                    userFound?.let { user ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(
                                    alpha = 0.5f
                                )
                            ), modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    "✅ Usuari trobat:",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text("👤 Nick: ${user.nick}")
                                Text("📛 Nom: ${user.nom} ${user.cognom1 ?: ""}")
                                Text("🆔 ID: ${user.id}")
                                Text("👥 Rol: ${if (user.rol == 2) "Administrador" else "Usuari"}")
                            }
                        }

                        // Comprovació de seguretat: no pots eliminar-te a tu mateix
                        if (user.id == currentUserId) {
                            LaunchedEffect(Unit) {
                                // Aquest bloc només s'executa si la condició és certa.
                                // S'executa només una vegada gràcies a key=Unit.
                                authViewModel.clearSearch()
                            }
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                ), modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "⚠️ No pots eliminar-te a tu mateix!",
                                    modifier = Modifier.padding(12.dp),
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }, confirmButton = {
                if (userFound == null) {
                    // Botó de cercar
                    Button(
                        onClick = {
                            if (nifToDelete.isNotBlank()) {
                                isSearching = true
                                searchError = null
                                authViewModel.searchUserByNif(nifToDelete) // Iniciar cerca
                            }
                        }, enabled = nifToDelete.isNotBlank() && !isSearching
                    ) {
                        if (isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Cercar Usuari")
                        }
                    }

                    // Efecte per observar el resultat de la cerca
                    LaunchedEffect(userSearchState) {
                        if (userSearchState.hasSearched && !userSearchState.isSearching) {
                            isSearching = false
                            if (userSearchState.searchResult != null) {
                                userFound = userSearchState.searchResult
                                searchError = null
                            } else {
                                searchError = userSearchState.error
                                    ?: "No s'ha trobat cap usuari amb aquest NIF"
                            }
                        }
                    }
                } else {
                    // Botó de procedir a eliminar
                    Button(
                        onClick = {
                            if (userFound!!.id != currentUserId) {
                                authViewModel.clearSearch()
                                showConfirmation = true
                            }
                        },
                        enabled = userFound!!.id != currentUserId,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Procedir a Eliminar")
                    }
                }
            }, dismissButton = {
                TextButton(
                    onClick = {
                        authViewModel.clearSearch()
                        showDeleteUserDialog = false
                        nifToDelete = ""
                        userFound = null
                        searchError = null
                    }) {
                    Text("Cancel·lar")
                }
            })
        } else {
            // SEGON PAS: Confirmar eliminació
            AlertDialog(onDismissRequest = {
                showConfirmation = false
                confirmationText = ""
            }, icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
            }, title = {
                Text(
                    "⚠️ Confirmar Eliminació",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }, text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Estàs a punt d'ELIMINAR PERMANENTMENT l'usuari:",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "👤 ${userFound?.nick ?: ""}", fontWeight = FontWeight.Bold
                            )
                            Text("📛 ${userFound?.nom ?: ""} ${userFound?.cognom1 ?: ""}")
                            Text("🆔 ID: ${userFound?.id ?: ""}")
                            Text("📄 NIF: ${userFound?.nif ?: ""}")
                        }
                    }

                    Text(
                        "⚠️ ATENCIÓ: Aquesta acció NO es pot desfer.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        "Per confirmar l'eliminació, escriu la paraula ELIMINAR:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    OutlinedTextField(
                        value = confirmationText,
                        onValueChange = { confirmationText = it.uppercase() },
                        label = { Text("Escriu: $requiredText") },
                        placeholder = { Text(requiredText) },
                        isError = confirmationText.isNotEmpty() && confirmationText != requiredText,
                        supportingText = {
                            if (confirmationText.isNotEmpty() && confirmationText != requiredText) {
                                Text(
                                    "Has d'escriure exactament: $requiredText",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (confirmationText == requiredText) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }, confirmButton = {

                Button(
                    onClick = {
                        userFound?.let { user ->
                            authViewModel.deleteUser(
                                userId = user.id, onResult = { success, message ->
                                    if (success) {
                                        showDeleteUserDialog = false
                                        showConfirmation = false
                                        Toast.makeText(
                                            context, "✅ $message", Toast.LENGTH_LONG
                                        ).show()
                                        userFound = null
                                        nifToDelete = ""
                                        // 1. Obtenir l'ID de l'usuari actiu (l'administrador)
                                        val currentUserId =
                                            authViewModel.loginUiState.value.authResponse?.id ?: 0L

                                        // 2. Netejar la pila fins a la Home de l'Admin
                                        navController.popBackStack(
                                            route = AppScreens.AdminHomeScreen.route,
                                            inclusive = false // Manté AdminHomeScreen
                                        )

                                        // 3. Navegar al perfil de l'usuari actiu (l'administrador)
                                        navController.navigate(
                                            AppScreens.UserProfileScreen.route.replace(
                                                "{userId}",
                                                currentUserId.toString()
                                            )
                                        ) {
                                            // Assegura que no es creen múltiples instàncies
                                            launchSingleTop = true
                                        }
                                    } else {
                                        Toast.makeText(
                                            context, "❌ $message", Toast.LENGTH_LONG
                                        ).show()
                                    }
                                })
                        }
                    },
                    enabled = confirmationText == requiredText,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ELIMINAR DEFINITIVAMENT")
                }
            }, dismissButton = {
                TextButton(
                    onClick = {
                        showConfirmation = false
                        confirmationText = ""
                    }) {
                    Text("Tornar")
                }
            })
        }
    }

    // ========== Diàleg d'Actualització d'Usuari per NIF (Admins) ==========
    if (showUpdateUserDialog) {
        var nifToSearch by remember { mutableStateOf("") }
        val userSearchState by authViewModel.userSearchState.collectAsState()
        var isSearching by remember { mutableStateOf(false) }
        var searchError by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()

        AlertDialog(
            onDismissRequest = {
                showUpdateUserDialog = false
                authViewModel.clearSearch()
                nifToSearch = ""
            },
            icon = {
                Icon(
                    Icons.Default.ManageAccounts,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text("Actualitzar Usuari per NIF", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "Introdueix el NIF de l'usuari que vols editar:",
                        style = MaterialTheme.typography.bodyLarge
                    )

                    OutlinedTextField(
                        value = nifToSearch,
                        onValueChange = {
                            nifToSearch = it.uppercase()
                            searchError = null
                        },
                        label = { Text("NIF/DNI") },
                        placeholder = { Text("12345678A") },
                        isError = searchError != null,
                        supportingText = {
                            searchError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isSearching
                    )

                    if (isSearching) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (nifToSearch.isNotBlank()) {
                            isSearching = true
                            searchError = null
                            authViewModel.searchUserByNif(nifToSearch)
                        }
                    },
                    enabled = nifToSearch.isNotBlank() && !isSearching
                ) {
                    Text(if (isSearching) "Cercant..." else "Cercar Usuari")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showUpdateUserDialog = false
                    authViewModel.clearSearch()
                    nifToSearch = ""
                }) {
                    Text("Cancel·lar")
                }
            }
        )

        // Efecte per observar el resultat de la cerca i navegar
        LaunchedEffect(userSearchState) {
            if (userSearchState.hasSearched && !userSearchState.isSearching) {
                isSearching = false
                if (userSearchState.searchResult != null) {
                    // Usuari trobat!
                    val user = userSearchState.searchResult!!

                    // 1. Netejar l'estat de cerca global
                    authViewModel.clearSearch()

                    // 2. Tancar el diàleg
                    showUpdateUserDialog = false


                    // 3. NAVEGAR A EDIT PROFILE
                    navController.navigate(AppScreens.UserProfileScreen.createRoute(user.id))

                    // Nota: ProfileScreen cridarà automàticament a loadUserProfile(user.id),
                    // i des d'allà l'administrador podrà navegar a EditProfileScreen.route.
                    // (ProfileScreen ha de tenir l'opció 'Editar Perfil')


                    Toast.makeText(
                        context,
                        "Usuari ${user.nick} trobat. Obrint perfil...",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    // Usuari no trobat o error
                    searchError =
                        userSearchState.error ?: "No s'ha trobat cap usuari amb aquest NIF"
                }
            }
        }
    }
}

/**
 * Seccions d'opcions per administradors.
 *
 * **Descripció:**
 * Component que mostra les opcions disponibles per administradors:
 * gestió d'usuaris i accés al panell web.
 *
 * **Funcionalitats:**
 * - Veure tots els usuaris
 * - Afegir nou usuari
 * - Enllaç al panell web d'administració
 *
 * @param navController Controlador per navegació
 * @param context Context per obrir el navegador
 *
 * @author Oscar
 * @since 1.0
 * @see AdminHomeScreen
 * @see AddUserScreen
 */
@Composable
fun AdminUserSection(
    navController: NavController,
    context: android.content.Context,
    onDeleteClick: () -> Unit = {}, // Nou paràmetre per gestionar el clic d'eliminar
    onUpdateClick: () -> Unit = {},
) {
    // ========== Gestió d'Usuaris ==========

    /**
     * Card per opcions de gestió d'usuaris.
     * Només disponible per administradors.
     */
    SectionCard(
        title = "Gestió d'Usuaris",
        icon = Icons.Default.Group,
        containerColor = MaterialTheme.colorScheme.errorContainer
    ) {
        OptionItem(
            icon = Icons.Default.SupervisorAccount,
            title = "Veure Tots els Usuaris",
            subtitle = "Gestionar usuaris del sistema",
            onClick = {
                navController.navigate(AppScreens.AdminHomeScreen.route)
            })

        HorizontalDivider()

        OptionItem(
            icon = Icons.Default.PersonAdd,
            title = "Afegir Usuari",
            subtitle = "Registrar nou usuari",
            onClick = {
                navController.navigate(AppScreens.AddUserScreen.route)
            })



        HorizontalDivider()

        OptionItem(
            icon = Icons.Default.Badge,
            title = "Cercar d'Usuaris",
            subtitle = "Trobar usuari pel seu NIF o per la seva ID",
            onClick = {
                navController.navigate(AppScreens.UserSearchScreen.route)
            })

        HorizontalDivider()

        OptionItem(
            icon = Icons.Default.DeleteSweep,
            title = "Eliminar Usuari",
            subtitle = "Eliminar un usuari del sistema",
            onClick = onDeleteClick // Utilitzar el callback passat com a paràmetre
        )

        HorizontalDivider()


    }

    // ========== Panel d'Administració Completa ==========

    /**
     * Secció amb enllaç al panell web.
     * Obre el navegador amb l'URL del panell d'administració.
     */
    SectionCard(
        title = "Administració Completa", icon = Icons.Default.Computer
    ) {
        OptionItem(
            icon = Icons.Default.OpenInBrowser,
            title = "Obrir Panell Web",
            subtitle = "Accedir a totes les funcions administratives",
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://localhost:8080/admin"))
                context.startActivity(intent)
            })
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
 * - Ressenyes Pendents: Llibres per valorar
 *
 * **3. Notificacions:**
 * - Avisos de Devolució: Recordatoris de dates límit
 * - Notificacions Generals: Avisos del sistema
 *
 * @param navController Controlador per navegació
 * @param context Context per mostrar missatges Toast
 *
 * @author Oscar
 * @since 1.0
 */
@Composable
fun NormalUserSection(
    navController: NavController,
    context: android.content.Context,
) {
    val context = LocalContext.current

    // ========== Gestió de Préstecs ==========

    SectionCard(
        title = "Els Meus Préstecs",
        icon = Icons.AutoMirrored.Filled.MenuBook,
        containerColor = MaterialTheme.colorScheme.primaryContainer
    ) {
        OptionItem(
            icon = Icons.Default.Book,
            title = "Préstecs Actius",
            subtitle = "Veure llibres que tinc prestats",
            onClick = {
                // TODO: REQ 4 - Implementar MyLoansScreen
                Toast.makeText(context, "Funcionalitat pendent", Toast.LENGTH_SHORT).show()
            })

        HorizontalDivider()

        OptionItem(
            icon = Icons.Default.Bookmark,
            title = "Les Meves Reserves",
            subtitle = "Veure llibres reservats",
            onClick = {
                // TODO: REQ 4 - Implementar MyReservationsScreen
                Toast.makeText(context, "Funcionalitat pendent", Toast.LENGTH_SHORT).show()
            })

        HorizontalDivider()

        OptionItem(
            icon = Icons.Default.History,
            title = "Historial de Préstecs",
            subtitle = "Veure préstecs anteriors",
            onClick = {
                // TODO: REQ 4 - Implementar LoanHistoryScreen
                Toast.makeText(context, "Funcionalitat pendent", Toast.LENGTH_SHORT).show()
            })
    }

    // ========== Sistema de Ressenyes ==========

    SectionCard(
        title = "Les Meves Ressenyes", icon = Icons.Default.Star
    ) {
        OptionItem(
            icon = Icons.Default.RateReview,
            title = "Les Meves Ressenyes",
            subtitle = "Veure i editar ressenyes escrites",
            onClick = {
                // TODO: REQ 6 - Implementar MyReviewsScreen
                Toast.makeText(context, "Funcionalitat pendent", Toast.LENGTH_SHORT).show()
            })

        HorizontalDivider()

        OptionItem(
            icon = Icons.Default.Create,
            title = "Llibres per Valorar",
            subtitle = "Escriure ressenyes pendents",
            onClick = {
                // TODO: REQ 6 - Implementar PendingReviewsScreen
                Toast.makeText(context, "Funcionalitat pendent", Toast.LENGTH_SHORT).show()
            })

        HorizontalDivider()

        OptionItem(
            icon = Icons.AutoMirrored.Filled.TrendingUp,
            title = "Ressenyes Populars",
            subtitle = "Veure llibres més ben valorats",
            onClick = {
                // TODO: REQ 6 - Implementar PopularBooksScreen
                Toast.makeText(context, "Funcionalitat pendent", Toast.LENGTH_SHORT).show()
            })
    }

    // ========== Centre de Notificacions ==========

    SectionCard(
        title = "Notificacions", icon = Icons.Default.Notifications
    ) {
        OptionItem(
            icon = Icons.Default.Schedule,
            title = "Avisos de Devolució",
            subtitle = "Veure dates límit properes",
            onClick = {
                // TODO: REQ 8 - Implementar ReturnRemindersScreen
                Toast.makeText(context, "Funcionalitat pendent", Toast.LENGTH_SHORT).show()
            })

        HorizontalDivider()

        OptionItem(
            icon = Icons.Default.NotificationImportant,
            title = "Els Meus Avisos",
            subtitle = "Veure notificacions rebudes",
            onClick = {
                // TODO: REQ 8 - Implementar MyNotificationsScreen
                Toast.makeText(context, "Funcionalitat pendent", Toast.LENGTH_SHORT).show()
            })
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

            HorizontalDivider()

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
            containerColor = if (isAdmin) MaterialTheme.colorScheme.errorContainer
            else MaterialTheme.colorScheme.primaryContainer
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
                tint = if (isAdmin) MaterialTheme.colorScheme.onErrorContainer
                else MaterialTheme.colorScheme.onPrimaryContainer
            )

            /**
             * Nick de l'usuari (destacat).
             */
            Text(
                text = nick,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (isAdmin) MaterialTheme.colorScheme.onErrorContainer
                else MaterialTheme.colorScheme.onPrimaryContainer
            )

            /**
             * Nom complet.
             */
            Text(
                text = "$nombre $apellido1 $apellido2".trim(),
                style = MaterialTheme.typography.bodyLarge,
                color = if (isAdmin) MaterialTheme.colorScheme.onErrorContainer
                else MaterialTheme.colorScheme.onPrimaryContainer
            )

            /**
             * Badge amb el rol.
             */
            Badge(
                containerColor = if (isAdmin) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.primary
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
                color = if (isAdmin) MaterialTheme.colorScheme.onErrorContainer
                else MaterialTheme.colorScheme.onPrimaryContainer
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
        headlineContent = { Text(title) }, supportingContent = {
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }, leadingContent = {
            Icon(
                icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary
            )
        }, trailingContent = {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }, modifier = Modifier.clickable(onClick = onClick)
    )
}