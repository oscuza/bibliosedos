package com.oscar.bibliosedaos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.oscar.bibliosedaos.navigation.AppScreens
import com.oscar.bibliosedaos.ui.viewmodels.AuthViewModel

/**
 * Pantalla de perfil d'usuari (versió simplificada).

 *  Aquesta pantalla està sent gradualment reemplaçada per [ProfileScreen].
 *  ProfileScreen ofereix una experiència més completa i adaptativa segons el rol.
 * - Funcional però amb funcionalitat bàsica
 * - S'està migrant a ProfileScreen per consistència
 *
 * **Funcionalitats Bàsiques:**
 * - Mostrar informació de l'usuari (nick, nom, cognoms, ID, rol)
 * - Botó per editar perfil
 * - Botó per tancar sessió

 *
 * @param userId Identificador de l'usuari a mostrar
 * @param navController Controlador de navegació
 * @param authViewModel ViewModel per gestió de dades
 *
 * @author Oscar
 * @since 1.0
 * @deprecated Utilitzar [ProfileScreen] per funcionalitat completa
 * @see ProfileScreen
 * @see EditProfileScreen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    userId: Long,
    navController: NavController,
    authViewModel: AuthViewModel
) {
    // ========== Estats Observables ==========

    /**
     * Estat del perfil de l'usuari.
     * Conté: isLoading, user, error
     */
    val userProfileState by authViewModel.userProfileState.collectAsState()

    // ========== Càrrega del Perfil ==========

    /**
     * Carrega el perfil de l'usuari en iniciar la pantalla.
     * S'executa una sola vegada (key = Unit).
     */
    LaunchedEffect(Unit) {
        authViewModel.loadUserProfile(userId)
    }

    // ========== UI ==========

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("El Meu Perfil") },
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
                    /**
                     * Indicador de càrrega centrat.
                     */
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(50.dp)
                            .align(Alignment.Center)
                    )
                }

                // ========== Estat: Error ==========
                userProfileState.error != null -> {
                    /**
                     * Missatge d'error centrat.
                     */
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
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

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // ========== Card de Perfil ==========

                        /**
                         * Card amb la informació de l'usuari.
                         * Mostra: nick, nom, cognoms, ID, rol
                         */
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                /**
                                 * Header amb nick i icona.
                                 */
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = user.nick,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Divider()

                                /**
                                 * Files d'informació amb format label: valor.
                                 */
                                UserInfoRow("Nom", user.nombre)
                                UserInfoRow(
                                    "Cognoms",
                                    "${user.apellido1 ?: ""} ${user.apellido2 ?: ""}".trim()
                                )
                                UserInfoRow("ID", user.id.toString())
                                UserInfoRow(
                                    "Rol",
                                    if (user.rol == 0) "Usuari" else "Administrador"
                                )
                            }
                        }

                        // ========== Botó Editar Perfil ==========

                        /**
                         * Botó per navegar a la pantalla d'edició.
                         */
                        Button(
                            onClick = {
                                navController.navigate(AppScreens.EditProfileScreen.route)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Editar Perfil")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Component reutilitzable per mostrar files d'informació.
 *
 * **Descripció:**
 * Mostra una fila amb una etiqueta (label) i un valor en un format consistent.
 * S'utilitza per mostrar camps de dades de l'usuari de forma organitzada.
 *
 * @param label Etiqueta del camp (esquerra)
 * @param value Valor del camp (dreta)
 *
 * @author Oscar
 * @since 1.0
 */
@Composable
fun UserInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        /**
         * Label del camp (esquerra).
         * Color atenuado per diferenciar-lo del valor.
         */
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        /**
         * Valor del camp (dreta).
         * Font destacada per facilitar la lectura.
         */
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}