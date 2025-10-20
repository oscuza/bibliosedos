package com.oscar.bibliosedaos.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.oscar.bibliosedaos.ui.viewmodels.AuthViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Pantalla per afegir un nou usuari al sistema.
 *
 * **Accés:**
 * Només accessible per administradors. Permet crear nous usuaris amb
 * validació completa i en temps real de tots els camps del formulari.
 *
 * @param onNavigateBack Callback per tornar a la pantalla anterior (AdminHomeScreen)
 * @param authViewModel ViewModel per gestionar la creació de l'usuari
 *
 * @author Oscar
 * @since 1.0
 * @see AuthViewModel.createUser
 * @see CreateUserRequest
 * @see AdminHomeScreen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddUserScreen(
    onNavigateBack: () -> Unit,
    authViewModel: AuthViewModel,
) {
    // Context d'Android per mostrar Toasts
    val context = LocalContext.current

    // Scope per executar coroutines
    val coroutineScope = rememberCoroutineScope()

    // ========== Estats del Formulari ==========

    /**
     * Nick de l'usuari.
     * Validació: 3-50 caràcters, alfanumèric + guió baix.
     */
    var nick by remember { mutableStateOf("") }

    /**
     * Contrasenya de l'usuari.
     * Validació: mínim 6 caràcters, màxim 100.
     */
    var password by remember { mutableStateOf("") }

    /**
     * Confirmació de contrasenya.
     * Ha de coincidir exactament amb [password].
     */
    var confirmPassword by remember { mutableStateOf("") }

    /**
     * Nom real de l'usuari.
     * Validació: mínim 2 caràcters, màxim 100.
     */
    var nombre by remember { mutableStateOf("") }

    /**
     * Primer cognom.
     * Validació: mínim 2 caràcters, màxim 100.
     */
    var apellido1 by remember { mutableStateOf("") }

    /**
     * Segon cognom (opcional).
     * Sense validacions, màxim 100 caràcters.
     */
    var apellido2 by remember { mutableStateOf("") }

    /**
     * Rol de l'usuari.
     * 1 = Usuari Normal (valor per defecte)
     * 2 = Administrador
     */
    var rol by remember { mutableStateOf(1) }

    /**
     * Indicador si s'està guardant actualment.
     * Deshabilita tots els camps i botons durant el procés.
     */
    var isSaving by remember { mutableStateOf(false) }

    // ========== Estats de Validació ==========

    /**
     * Missatge d'error per al camp nick.
     * null = camp vàlid, String = missatge d'error a mostrar.
     */
    var nickError by remember { mutableStateOf<String?>(null) }

    /**
     * Missatge d'error per al camp password.
     */
    var passwordError by remember { mutableStateOf<String?>(null) }

    /**
     * Missatge d'error per al camp confirmPassword.
     */
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }

    /**
     * Missatge d'error per al camp nombre.
     */
    var nombreError by remember { mutableStateOf<String?>(null) }

    /**
     * Missatge d'error per al camp apellido1.
     */
    var apellido1Error by remember { mutableStateOf<String?>(null) }

    // ========== Validacions en Temps Real ==========

    /**
     * Validació en temps real del nick.
     * S'executa cada vegada que canvia el valor de [nick].
     */
    LaunchedEffect(nick) {
        nickError = when {
            nick.isEmpty() -> null // No mostrar error si està buit
            nick.length < 3 -> "Mínim 3 caràcters"
            nick.length > 50 -> "Màxim 50 caràcters"
            !nick.matches(Regex("^[a-zA-Z0-9_]+$")) -> "Només lletres, números i _"
            else -> null // Validació correcta
        }
    }

    /**
     * Validació en temps real de la contrasenya.
     * S'executa cada vegada que canvia el valor de [password].
     */
    LaunchedEffect(password) {
        passwordError = when {
            password.isEmpty() -> null
            password.length < 6 -> "Mínim 6 caràcters"
            password.length > 100 -> "Màxim 100 caràcters"
            else -> null
        }
    }

    /**
     * Validació en temps real de la confirmació de contrasenya.
     * S'executa quan canvia [password] o [confirmPassword].
     */
    LaunchedEffect(password, confirmPassword) {
        confirmPasswordError = when {
            confirmPassword.isEmpty() -> null
            confirmPassword != password -> "Les contrasenyes no coincideixen"
            else -> null
        }
    }

    /**
     * Validació en temps real del nombre.
     * S'executa cada vegada que canvia el valor de [nombre].
     */
    LaunchedEffect(nombre) {
        nombreError = when {
            nombre.isEmpty() -> null
            nombre.length < 2 -> "Mínim 2 caràcters"
            nombre.length > 100 -> "Massa llarg"
            else -> null
        }
    }

    /**
     * Validació en temps real del primer cognom.
     * S'executa cada vegada que canvia el valor de [apellido1].
     */
    LaunchedEffect(apellido1) {
        apellido1Error = when {
            apellido1.isEmpty() -> null
            apellido1.length < 2 -> "Mínim 2 caràcters"
            apellido1.length > 100 -> "Massa llarg"
            else -> null
        }
    }

    // ========== UI ==========

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Afegir Nou Usuari") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Tornar enrere"
                        )
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ========== Capçalera Informativa ==========

            /**
             * Card amb icona i text explicatiu de la funcionalitat.
             */
            Card(
                modifier = Modifier.fillMaxWidth(),
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
                        Icons.Default.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Crear nou usuari al sistema",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ========== Camps del Formulari ==========

            /**
             * Camp de text per al nick.
             * Mostra error si [nickError] no és null.
             */
            OutlinedTextField(
                value = nick,
                onValueChange = { if (it.length <= 50) nick = it },
                label = { Text("Usuari (Nick) *") },
                placeholder = { Text("Ex: usuari123") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isSaving,
                isError = nickError != null,
                supportingText = {
                    if (nickError != null) {
                        Text(nickError!!, color = MaterialTheme.colorScheme.error)
                    } else {
                        Text("Només lletres, números i guions baixos")
                    }
                }
            )

            /**
             * Camp de text per a la contrasenya.
             * Utilitza PasswordVisualTransformation per ocultar el text.
             */
            OutlinedTextField(
                value = password,
                onValueChange = { if (it.length <= 100) password = it },
                label = { Text("Contrasenya *") },
                placeholder = { Text("Mínim 6 caràcters") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                enabled = !isSaving,
                isError = passwordError != null,
                supportingText = {
                    if (passwordError != null) {
                        Text(passwordError!!, color = MaterialTheme.colorScheme.error)
                    }
                }
            )

            /**
             * Camp de text per confirmar la contrasenya.
             * Ha de coincidir amb [password].
             */
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { if (it.length <= 100) confirmPassword = it },
                label = { Text("Confirmar Contrasenya *") },
                placeholder = { Text("Repeteix la contrasenya") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                enabled = !isSaving,
                isError = confirmPasswordError != null,
                supportingText = {
                    if (confirmPasswordError != null) {
                        Text(confirmPasswordError!!, color = MaterialTheme.colorScheme.error)
                    }
                }
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            /**
             * Camp de text per al nom.
             */
            OutlinedTextField(
                value = nombre,
                onValueChange = { if (it.length <= 100) nombre = it },
                label = { Text("Nom *") },
                placeholder = { Text("Ex: Joan") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isSaving,
                isError = nombreError != null,
                supportingText = {
                    if (nombreError != null) {
                        Text(nombreError!!, color = MaterialTheme.colorScheme.error)
                    }
                }
            )

            /**
             * Camp de text per al primer cognom.
             */
            OutlinedTextField(
                value = apellido1,
                onValueChange = { if (it.length <= 100) apellido1 = it },
                label = { Text("Primer Cognom *") },
                placeholder = { Text("Ex: García") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isSaving,
                isError = apellido1Error != null,
                supportingText = {
                    if (apellido1Error != null) {
                        Text(apellido1Error!!, color = MaterialTheme.colorScheme.error)
                    }
                }
            )

            /**
             * Camp de text per al segon cognom (opcional).
             */
            OutlinedTextField(
                value = apellido2,
                onValueChange = { if (it.length <= 100) apellido2 = it },
                label = { Text("Segon Cognom") },
                placeholder = { Text("Opcional") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isSaving,
                supportingText = { Text("Camp opcional") }
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // ========== Selector de Rol ==========

            Text(
                "Tipus d'Usuari *",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )

            /**
             * Chips per seleccionar el rol.
             * Només un pot estar seleccionat a la vegada.
             */
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = rol == 1,
                    onClick = { if (!isSaving) rol = 1 },
                    label = { Text("Usuari Normal") },
                    modifier = Modifier.weight(1f),
                    enabled = !isSaving
                )
                FilterChip(
                    selected = rol == 2,
                    onClick = { if (!isSaving) rol = 2 },
                    label = { Text("Administrador") },
                    modifier = Modifier.weight(1f),
                    enabled = !isSaving
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ========== Botó Guardar ==========

            /**
             * Botó per crear l'usuari.
             * Realitza validacions finals abans de cridar al ViewModel.
             */
            Button(
                onClick = {
                    // Validar camps obligatoris
                    if (nick.isBlank()) {
                        Toast.makeText(context, "El nick és obligatori", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (password.isBlank()) {
                        Toast.makeText(context, "La contrasenya és obligatòria", Toast.LENGTH_SHORT)
                            .show()
                        return@Button
                    }
                    if (nombre.isBlank()) {
                        Toast.makeText(context, "El nom és obligatori", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (apellido1.isBlank()) {
                        Toast.makeText(
                            context,
                            "El primer cognom és obligatori",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@Button
                    }

                    // Validar que no hi hagi errors existents
                    if (nickError != null || passwordError != null ||
                        confirmPasswordError != null || nombreError != null ||
                        apellido1Error != null
                    ) {
                        Toast.makeText(
                            context,
                            "Corregeix els errors abans de continuar",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@Button
                    }

                    // Tot correcte: crear usuari
                    isSaving = true

                    coroutineScope.launch {
                        authViewModel.createUser(
                            nick = nick.trim(),
                            password = password,
                            nombre = nombre.trim(),
                            apellido1 = apellido1.trim(),
                            apellido2 = apellido2.trim().ifEmpty { null },
                            rol = rol
                        ) { success, message ->
                            isSaving = false
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()

                            if (success) {
                                // Esperar 300ms i tornar enrere
                                coroutineScope.launch {
                                    delay(300)
                                    onNavigateBack()
                                }
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("GUARDANT...")
                } else {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("CREAR USUARI")
                }
            }

            // ========== Botó Cancel·lar ==========

            /**
             * Botó per cancel·lar i tornar enrere sense guardar.
             */
            OutlinedButton(
                onClick = onNavigateBack,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving
            ) {
                Text("CANCEL·LAR")
            }

            // ========== Nota Informativa ==========

            /**
             * Card amb informació i consells per omplir el formulari.
             */
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Text(
                        "Nota:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "• Els camps marcats amb * són obligatoris\n" +
                                "• El nick ha de ser únic al sistema\n" +
                                "• La contrasenya ha de tenir almenys 6 caràcters\n" +
                                "• Els usuaris normals podran gestionar préstecs\n" +
                                "• Els administradors tindran accés complet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}