package com.oscar.bibliosedaos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.oscar.bibliosedaos.navigation.AppScreens
import com.oscar.bibliosedaos.ui.viewmodels.AuthViewModel

/**
 * Pantalla per canviar la contrasenya de l'usuari.
 * Inclou validacions de seguretat i confirmació de contrasenya.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
) {
    // Estats per als camps
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    // Estats de visibilitat de contrasenyes
    var currentPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    // Estats de control
    var isChanging by remember { mutableStateOf(false) }
    var changeMessage by remember { mutableStateOf<String?>(null) }
    var changeSuccess by remember { mutableStateOf(false) }

    // Obtenir dades de l'usuari actual
    val userProfileState by authViewModel.userProfileState.collectAsState()
    val loginState by authViewModel.loginUiState.collectAsState()
    val userId = userProfileState.user?.id ?: 0L

    // Flag per prevenir doble clic al botó enrere
    var isNavigating by remember { mutableStateOf(false) }

    // Validacions
    val isCurrentPasswordValid = currentPassword.length >= 6
    val isNewPasswordValid = newPassword.length >= 6
    val isPasswordStrong = newPassword.length >= 8 &&
            newPassword.any { it.isDigit() } &&
            newPassword.any { it.isLetter() }
    val doPasswordsMatch = newPassword == confirmPassword && newPassword.isNotEmpty()
    val isDifferentPassword = newPassword != currentPassword && newPassword.isNotEmpty()

    val isFormValid = isCurrentPasswordValid &&
            isNewPasswordValid &&
            doPasswordsMatch &&
            isDifferentPassword &&
            !isChanging

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Canviar Contrasenya") },
                navigationIcon = {
                    if (!isNavigating) {
                        IconButton(onClick = {
                            if (!isNavigating) {
                                isNavigating = true
                                // Obtenir l'ID de l'usuari actual i navegar al seu perfil
                                val currentUserId = loginState.authResponse?.id ?: 0L
                                navController.navigate(
                                    AppScreens.UserProfileScreen.createRoute(currentUserId)
                                ) {
                                    // Eliminar ChangePasswordScreen de la pila
                                    popUpTo(AppScreens.ChangePasswordScreen.route) {
                                        inclusive = true
                                    }
                                    launchSingleTop = true
                                }
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Tornar")
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Informació de seguretat
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Column {
                        Text(
                            "Recomanacions de seguretat:",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "• Mínim 6 caràcters (recomanat 8+)\n" +
                                    "• Combina lletres i números\n" +
                                    "• No reutilitzis contrasenyes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Formulari
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Contrasenya actual
                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        label = { Text("Contrasenya Actual*") },
                        leadingIcon = { Icon(Icons.Default.Lock, null) },
                        trailingIcon = {
                            IconButton(onClick = {
                                currentPasswordVisible = !currentPasswordVisible
                            }) {
                                Icon(
                                    if (currentPasswordVisible) Icons.Default.VisibilityOff
                                    else Icons.Default.Visibility,
                                    contentDescription = "Mostrar contrasenya"
                                )
                            }
                        },
                        visualTransformation = if (currentPasswordVisible)
                            VisualTransformation.None
                        else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        isError = currentPassword.isNotEmpty() && !isCurrentPasswordValid,
                        supportingText = {
                            if (currentPassword.isNotEmpty() && !isCurrentPasswordValid) {
                                Text("Mínim 6 caràcters", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isChanging,
                        singleLine = true
                    )

                    HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

                    // Nova contrasenya
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("Nova Contrasenya*") },
                        leadingIcon = { Icon(Icons.Default.VpnKey, null) },
                        trailingIcon = {
                            IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                                Icon(
                                    if (newPasswordVisible) Icons.Default.VisibilityOff
                                    else Icons.Default.Visibility,
                                    contentDescription = "Mostrar contrasenya"
                                )
                            }
                        },
                        visualTransformation = if (newPasswordVisible)
                            VisualTransformation.None
                        else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        isError = newPassword.isNotEmpty() && !isNewPasswordValid,
                        supportingText = {
                            when {
                                newPassword.isNotEmpty() && !isNewPasswordValid -> {
                                    Text(
                                        "Mínim 6 caràcters",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }

                                newPassword.isNotEmpty() && newPassword == currentPassword -> {
                                    Text(
                                        "La nova contrasenya ha de ser diferent",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }

                                newPassword.isNotEmpty() && isPasswordStrong -> {
                                    Text(
                                        "✓ Contrasenya forta",
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                newPassword.isNotEmpty() && isNewPasswordValid -> {
                                    Text(
                                        "Contrasenya acceptable",
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isChanging,
                        singleLine = true
                    )

                    // Confirmar nova contrasenya
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirmar Nova Contrasenya*") },
                        leadingIcon = { Icon(Icons.Default.CheckCircle, null) },
                        trailingIcon = {
                            IconButton(onClick = {
                                confirmPasswordVisible = !confirmPasswordVisible
                            }) {
                                Icon(
                                    if (confirmPasswordVisible) Icons.Default.VisibilityOff
                                    else Icons.Default.Visibility,
                                    contentDescription = "Mostrar contrasenya"
                                )
                            }
                        },
                        visualTransformation = if (confirmPasswordVisible)
                            VisualTransformation.None
                        else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        isError = confirmPassword.isNotEmpty() && !doPasswordsMatch,
                        supportingText = {
                            when {
                                confirmPassword.isNotEmpty() && !doPasswordsMatch -> {
                                    Text(
                                        "Les contrasenyes no coincideixen",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }

                                confirmPassword.isNotEmpty() && doPasswordsMatch -> {
                                    Text(
                                        "✓ Les contrasenyes coincideixen",
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isChanging,
                        singleLine = true
                    )

                    // Indicador de força de contrasenya
                    if (newPassword.isNotEmpty()) {
                        Column {
                            Text(
                                "Força de la contrasenya:",
                                style = MaterialTheme.typography.bodySmall
                            )
                            LinearProgressIndicator(
                                progress = {
                                    when {
                                        newPassword.length >= 12 &&
                                                newPassword.any { it.isDigit() } &&
                                                newPassword.any { it.isLetter() } &&
                                                newPassword.any { !it.isLetterOrDigit() } -> 1.0f

                                        isPasswordStrong -> 0.7f
                                        isNewPasswordValid -> 0.4f
                                        else -> 0.2f
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                color = when {
                                    isPasswordStrong -> MaterialTheme.colorScheme.primary
                                    isNewPasswordValid -> MaterialTheme.colorScheme.tertiary
                                    else -> MaterialTheme.colorScheme.error
                                },
                                trackColor = ProgressIndicatorDefaults.linearTrackColor,
                                strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
                            )
                        }
                    }
                }
            }

            // Botó de canviar contrasenya
            Button(
                onClick = {
                    isChanging = true
                    changeMessage = null
                    changeSuccess = false

                    authViewModel.changePassword(
                        userId = userId,
                        currentPassword = currentPassword,
                        newPassword = newPassword,
                        onResult = { success, message ->
                            isChanging = false
                            changeMessage = message
                            changeSuccess = success
                            if (success) {
                                // Netejar camps si és exitós
                                currentPassword = ""
                                newPassword = ""
                                confirmPassword = ""
                            }
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = isFormValid
            ) {
                if (isChanging) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Default.Security, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("CANVIAR CONTRASENYA")
                }
            }

            // Missatge de resultat
            changeMessage?.let { message ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (changeSuccess)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = message,
                            color = if (changeSuccess)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onErrorContainer
                        )

                        if (changeSuccess) {
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(
                                onClick = {
                                    if (!isNavigating) {
                                        isNavigating = true
                                        // Obtenir l'ID de l'usuari actual i navegar al seu perfil
                                        val currentUserId = loginState.authResponse?.id ?: 0L
                                        navController.navigate(
                                            AppScreens.UserProfileScreen.createRoute(currentUserId)
                                        ) {
                                            // Eliminar ChangePasswordScreen de la pila
                                            popUpTo(AppScreens.ChangePasswordScreen.route) {
                                                inclusive = true
                                            }
                                            launchSingleTop = true
                                        }
                                    }
                                },
                                modifier = Modifier.align(Alignment.End),
                                enabled = !isNavigating
                            ) {
                                Text("TORNAR AL PERFIL")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}