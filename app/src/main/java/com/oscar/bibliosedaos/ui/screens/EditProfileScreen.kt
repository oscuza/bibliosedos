package com.oscar.bibliosedaos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.oscar.bibliosedaos.navigation.AppScreens
import com.oscar.bibliosedaos.ui.viewmodels.AuthViewModel


/**
 * Pantalla d'edició de perfil d'usuari.
 *
 * Permet als usuaris editar les seves dades personals bàsiques.
 * Gestiona automàticament els camps obligatoris del backend mantenint
 * els valors existents dels camps no editables.
 *
 * Camps Editables:
 * - Nick (amb validació d'unicitat)
 * - Nom
 * - Primer cognom
 * - Segon cognom (opcional)
 *
 * Camps NO Editables (es mostren però no es poden modificar):
 * - ID d'usuari
 * - Rol
 * - NIF, Email, Telèfon (si existeixen)
 *
 * @param navController Controlador de navegació per transicions entre pantalles
 * @param authViewModel ViewModel que gestiona la lògica d'actualització
 *
 * @author Oscar
 * @since 1.0
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
) {
    // ========== ESTATS OBSERVABLES ==========

    /**
     * Estat del perfil de l'usuari des del ViewModel
     */
    val userProfileState by authViewModel.userProfileState.collectAsState()

    // ========== ESTATS LOCALS PER ALS CAMPS EDITABLES ==========

    var nick by remember { mutableStateOf("") }
    var nom by remember { mutableStateOf("") }
    var cognom1 by remember { mutableStateOf("") }
    var cognom2 by remember { mutableStateOf("") }
    var nif by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var tlf by remember { mutableStateOf("") }
    var carrer by remember { mutableStateOf("") }
    var localitat by remember { mutableStateOf("") }
    var cp by remember { mutableStateOf("") }
    var provincia by remember { mutableStateOf("") }

    var isSaving by remember { mutableStateOf(false) }
    var saveMessage by remember { mutableStateOf<String?>(null) }

    // ========== CÀRREGA INICIAL DE DADES ==========

    /**
     * Carrega les dades actuals de l'usuari quan el perfil estigui disponible
     */
    LaunchedEffect(userProfileState.user) {
        userProfileState.user?.let { user ->
            nick = user.nick
            nom = user.nom
            cognom1 = user.cognom1 ?: ""
            cognom2 = user.cognom2 ?: ""
            nif = user.nif ?: ""
            email = user.email ?: ""
            tlf = user.tlf ?: ""
            carrer = user.carrer ?: ""
            localitat = user.localitat ?: ""
            cp = user.cp ?: ""
            provincia = user.provincia ?: ""
        }
    }

    // ========== VALIDACIONS ==========
    val isNickValid = nick.length in 3..10
    val isNomValid = nom.length >= 2
    val isCognom1Valid = cognom1.length >= 2
    val isNifValid = nif.matches(Regex("^[0-9]{8}[A-Z]$"))
    val isEmailValid = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    val isTlfValid = tlf.matches(Regex("^[0-9]{9}$"))
    val isCpValid = cp.matches(Regex("^[0-9]{5}$"))

    val isFormValid = isNickValid && isNomValid && isCognom1Valid &&
            isNifValid && isEmailValid && isTlfValid && isCpValid &&
            carrer.isNotBlank() && localitat.isNotBlank() &&
            provincia.isNotBlank() && !isSaving


    // ========== UI ==========

    Scaffold(
        topBar = {
            /**
             * Barra superior amb títol i botó de tornar
             */
            TopAppBar(
                title = { Text("Editar Perfil") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Tornar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when {
                userProfileState.isLoading -> {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        CircularProgressIndicator()
                    }
                }

                userProfileState.error != null -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = userProfileState.error!!,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                userProfileState.user != null -> {
                    val user = userProfileState.user!!

                    // DADES BÀSIQUES
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "Dades Bàsiques",
                                style = MaterialTheme.typography.titleMedium
                            )

                            OutlinedTextField(
                                value = nick,
                                onValueChange = { nick = it },
                                label = { Text("Nick*") },
                                leadingIcon = { Icon(Icons.Default.Person, null) },
                                isError = nick.isNotEmpty() && !isNickValid,
                                supportingText = {
                                    if (nick.isNotEmpty() && !isNickValid) {
                                        Text("3-10 caràcters")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isSaving,
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = nom,
                                onValueChange = { nom = it },
                                label = { Text("Nom*") },
                                isError = nom.isNotEmpty() && !isNomValid,
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isSaving,
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = cognom1,
                                onValueChange = { cognom1 = it },
                                label = { Text("Primer Cognom*") },
                                isError = cognom1.isNotEmpty() && !isCognom1Valid,
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isSaving,
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = cognom2,
                                onValueChange = { cognom2 = it },
                                label = { Text("Segon Cognom") },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isSaving,
                                singleLine = true
                            )
                        }
                    }

                    // DOCUMENTACIÓ
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "Documentació",
                                style = MaterialTheme.typography.titleMedium
                            )

                            OutlinedTextField(
                                value = nif,
                                onValueChange = { nif = it.uppercase() },
                                label = { Text("NIF/DNI*") },
                                leadingIcon = { Icon(Icons.Default.Badge, null) },
                                isError = nif.isNotEmpty() && !isNifValid,
                                supportingText = {
                                    if (nif.isNotEmpty() && !isNifValid) {
                                        Text("Format: 8 números + 1 lletra")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isSaving,
                                singleLine = true
                            )
                        }
                    }

                    // CONTACTE
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "Contacte",
                                style = MaterialTheme.typography.titleMedium
                            )

                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Email*") },
                                leadingIcon = { Icon(Icons.Default.Email, null) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                isError = email.isNotEmpty() && !isEmailValid,
                                supportingText = {
                                    if (email.isNotEmpty() && !isEmailValid) {
                                        Text("Format d'email invàlid")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isSaving,
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = tlf,
                                onValueChange = { if (it.all { char -> char.isDigit() }) tlf = it },
                                label = { Text("Telèfon*") },
                                leadingIcon = { Icon(Icons.Default.Phone, null) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                isError = tlf.isNotEmpty() && !isTlfValid,
                                supportingText = {
                                    if (tlf.isNotEmpty() && !isTlfValid) {
                                        Text("Exactament 9 dígits")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isSaving,
                                singleLine = true
                            )
                        }
                    }

                    // ADREÇA
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "Adreça",
                                style = MaterialTheme.typography.titleMedium
                            )

                            OutlinedTextField(
                                value = carrer,
                                onValueChange = { carrer = it },
                                label = { Text("Carrer*") },
                                leadingIcon = { Icon(Icons.Default.Home, null) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isSaving,
                                singleLine = true
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = cp,
                                    onValueChange = { if (it.all { char -> char.isDigit() }) cp = it },
                                    label = { Text("CP*") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    isError = cp.isNotEmpty() && !isCpValid,
                                    modifier = Modifier.weight(0.4f),
                                    enabled = !isSaving,
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = localitat,
                                    onValueChange = { localitat = it },
                                    label = { Text("Localitat*") },
                                    modifier = Modifier.weight(0.6f),
                                    enabled = !isSaving,
                                    singleLine = true
                                )
                            }

                            OutlinedTextField(
                                value = provincia,
                                onValueChange = { provincia = it },
                                label = { Text("Província*") },
                                leadingIcon = { Icon(Icons.Default.LocationOn, null) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isSaving,
                                singleLine = true
                            )
                        }
                    }

                    // BOTÓ GUARDAR
                    Button(
                        onClick = {
                            isSaving = true
                            saveMessage = null

                            authViewModel.updateCompleteProfile(
                                userId = user.id,
                                nick = nick,
                                nom = nom,
                                cognom1 = cognom1,
                                cognom2 = cognom2.trim().ifEmpty { null },
                                nif = nif,
                                email = email,
                                tlf = tlf,
                                carrer = carrer,
                                localitat = localitat,
                                cp = cp,
                                provincia = provincia,
                                onResult = { success, message ->
                                    isSaving = false
                                    saveMessage = message
                                    if (success) {

                                        val currentUserId = authViewModel.loginUiState.value.authResponse?.id ?: 0L

                                        navController.navigate(
                                            AppScreens.UserProfileScreen.route
                                                .replace("{userId}", currentUserId.toString())
                                        ) {
                                            // Neteja la pila fins a AdminHomeScreen (que és d'on ve l'accés)
                                            popUpTo(AppScreens.AdminHomeScreen.route) {
                                                inclusive = true
                                            }
                                            launchSingleTop = true
                                        }

                                    }
                                }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = isFormValid
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Default.Save, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("GUARDAR CANVIS")
                        }
                    }

                    // Missatge de resultat
                    saveMessage?.let { message ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (message.contains("Error"))
                                    MaterialTheme.colorScheme.errorContainer
                                else
                                    MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Text(
                                text = message,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        }

    }
}