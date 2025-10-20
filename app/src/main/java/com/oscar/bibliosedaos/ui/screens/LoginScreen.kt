package com.oscar.bibliosedaos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.oscar.bibliosedaos.navigation.AppScreens
import com.oscar.bibliosedaos.ui.viewmodels.AuthViewModel

/**
 * Pantalla d'inici de sessió del sistema de biblioteca.
 *
 * **Descripció:**
 * Punt d'entrada principal de l'aplicació. Permet als usuaris autenticar-se
 * mitjançant credencials (nick i contrasenya) per accedir al sistema segons
 * el seu rol assignat.
 *
 * @param navController Controlador de navegació per transicions entre pantalles
 * @param authViewModel ViewModel que gestiona la lògica d'autenticació
 *
 * @author Oscar
 * @since 1.0
 * @see AuthViewModel.login
 * @see LoginUiState
 * @see ProfileScreen
 */
@Composable
fun LoginScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
) {
    // ========== Estats Locals ==========

    /**
     * Nick d'usuari introduït al formulari.
     * Estat local gestionat amb remember.
     */
    var nick by remember { mutableStateOf("") }

    /**
     * Contrasenya introduïda al formulari.
     * Estat local gestionat amb remember.
     */
    var password by remember { mutableStateOf("") }

    /**
     * Controla la visibilitat de la contrasenya.
     * false = oculta (PasswordVisualTransformation)
     * true = visible (VisualTransformation.None)
     */
    var passwordVisible by remember { mutableStateOf(false) }

    // ========== Estats Observables del ViewModel ==========

    /**
     * Observa l'estat del login des del ViewModel.
     * Conté: isLoading, loginSuccess, error, authResponse
     */
    val uiState by authViewModel.loginUiState.collectAsState()

    // ========== Navegació Automàtica després del Login ==========

    /**
     * Effect que escolta els canvis en l'estat del login.
     * Quan el login és exitós, navega automàticament al perfil de l'usuari.
     *
     * **Comportament:**
     * - Només s'executa quan loginSuccess passa a true
     * - Obté l'ID de l'usuari de authResponse
     * - Navega a ProfileScreen amb l'userId
     * - Neteja la pila de navegació (popUpTo amb inclusive=true)
     *   per evitar que l'usuari torni al login amb el botó enrere
     */
    LaunchedEffect(uiState.loginSuccess) {
        if (uiState.loginSuccess) {
            val user = uiState.authResponse
            if (user != null) {
                // Tots els usuaris van a ProfileScreen
                // La pantalla s'adapta segons el rol
                navController.navigate(
                    AppScreens.UserProfileScreen.createRoute(user.id)
                ) {
                    popUpTo(AppScreens.LoginScreen.route) { inclusive = true }
                }
            }
        }
    }

    // ========== UI ==========

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        /**
         * Card principal que conté tot el formulari de login.
         * Elevació de 8dp per efecte de profunditat.
         */
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ========== Títol de l'Aplicació ==========

                /**
                 * Títol principal amb emoji de llibre.
                 * Font size: 25sp, Bold, Color: Primary
                 */
                Text(
                    "📚 App BiblioSedaos",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 25.sp
                )

                /**
                 * Subtítol descriptiu.
                 */
                Text(
                    "Iniciar Sessió",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // ========== Camp d'Usuari ==========

                /**
                 * Camp de text per al nick d'usuari.
                 *
                 * **Característiques:**
                 * - Single line (no permet salts de línia)
                 * - Es deshabilita durant isLoading
                 * - No té validació en temps real
                 */
                OutlinedTextField(
                    value = nick,
                    onValueChange = { nick = it },
                    label = { Text("Usuari") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading
                )

                // ========== Camp de Contrasenya ==========

                /**
                 * Camp de text per a la contrasenya amb toggle de visibilitat.
                 *
                 * **Característiques:**
                 * - PasswordVisualTransformation per ocultar text
                 * - Botó trailing per mostrar/ocultar contrasenya
                 * - Keyboard type: Password
                 * - Single line
                 * - Es deshabilita durant isLoading
                 */
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contrasenya") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible)
                        VisualTransformation.None
                    else
                        PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        /**
                         * Botó per alternar la visibilitat de la contrasenya.
                         * Icona canvia entre Visibility i VisibilityOff.
                         */
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible)
                                    Icons.Filled.Visibility
                                else
                                    Icons.Filled.VisibilityOff,
                                contentDescription = if (passwordVisible)
                                    "Ocultar contrasenya"
                                else
                                    "Mostrar contrasenya"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading
                )

                Spacer(modifier = Modifier.height(8.dp))

                // ========== Botó d'Inici de Sessió ==========

                /**
                 * Botó per iniciar sessió.
                 *
                 * **Condicions d'Habilitació:**
                 * - No està en procés de login (isLoading = false)
                 * - Nick no està buit
                 * - Password no està buida
                 *
                 * **Comportament durant Login:**
                 * - Mostra CircularProgressIndicator
                 * - Text canvia a "ENTRANT..."
                 * - Deshabilitat fins que acabi el procés
                 */
                Button(
                    onClick = {
                        if (nick.isNotBlank() && password.isNotBlank()) {
                            authViewModel.login(nick, password)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = !uiState.isLoading && nick.isNotBlank() && password.isNotBlank()
                ) {
                    if (uiState.isLoading) {
                        // Mostrar indicador de càrrega
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        // Mostrar text normal
                        Text("ENTRAR", style = MaterialTheme.typography.titleMedium)
                    }
                }

                // ========== Missatges d'Error ==========

                /**
                 * Card vermell que apareix quan hi ha un error.
                 * Mostra el missatge d'error proporcionat pel ViewModel.
                 *
                 * **Tipus d'Errors:**
                 * - Credencials incorrectes (401/403)
                 * - Servei no disponible (404)
                 * - Error al servidor (500)
                 * - Problemes de connexió
                 */
                uiState.error?.let { error ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}