package com.oscar.bibliosedaos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
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
import kotlinx.coroutines.launch

/**
 * Pantalla per crear nous usuaris amb TOTS els camps obligatoris.
 * Només accessible per administradors.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddUserScreen(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    // ========== ESTATS PER CADA CAMP ==========

    // Camps bàsics
    var nick by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var nombre by remember { mutableStateOf("") }
    var cognom1 by remember { mutableStateOf("") }
    var cognom2 by remember { mutableStateOf("") }

    // Camps de document
    var nif by remember { mutableStateOf("") }

    // Camps de contacte
    var email by remember { mutableStateOf("") }
    var tlf by remember { mutableStateOf("") }

    // Camps d'adreça
    var carrer by remember { mutableStateOf("") }
    var localitat by remember { mutableStateOf("") }
    var cp by remember { mutableStateOf("") }
    var provincia by remember { mutableStateOf("") }

    // Rol
    var rol by remember { mutableStateOf(1) } // Per defecte usuari normal

    // Estats observables
    val createUserState by authViewModel.createUserState.collectAsState()
    val loginState by authViewModel.loginUiState.collectAsState()

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Variable per rastrejar si s'ha enviat el formulari
    var hasSubmitted by remember { mutableStateOf(false) }

    // Flag per prevenir doble clic al botó enrere
    var isNavigating by remember { mutableStateOf(false) }

    // ========== VALIDACIONS ==========

    val isNickValid = nick.length in 3..10 && nick.matches(Regex("^[a-zA-Z0-9_]+$"))
    val isPasswordValid = password.length >= 6
    val isNombreValid = nombre.length >= 2
    val isApellido1Valid = cognom1.length >= 2
    val isNifValid = nif.matches(Regex("^[0-9]{8}[A-Z]$"))
    val isEmailValid = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    val isTlfValid = tlf.matches(Regex("^[0-9]{9}$"))
    val isCpValid = cp.matches(Regex("^[0-9]{5}$"))
    val isCarrerValid = carrer.isNotBlank()
    val isLocalitatValid = localitat.isNotBlank()
    val isProvinciaValid = provincia.isNotBlank()

    val isFormValid = isNickValid && isPasswordValid && isNombreValid &&
            isApellido1Valid && isNifValid && isEmailValid &&
            isTlfValid && isCpValid && isCarrerValid &&
            isLocalitatValid && isProvinciaValid && !createUserState.isCreating

    // ========== GESTIÓ DE RESPOSTES ==========

    LaunchedEffect(createUserState.isCreating, createUserState.error) {
        if (hasSubmitted && !createUserState.isCreating) {
            if (createUserState.error == null && createUserState.success) {
                // Èxit: mostra missatge i navega
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Usuari creat correctament",
                        duration = SnackbarDuration.Short
                    )
                }
                hasSubmitted = false
                if (!isNavigating) {
                    isNavigating = true
                    val adminId = loginState.authResponse?.id ?: 0L
                    navController.navigate(
                        AppScreens.UserProfileScreen.createRoute(adminId)
                    ) {
                        popUpTo(AppScreens.AddUserScreen.route) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                }
            } else {
                // Error: només mostra l'error (ja es gestiona en un altre LaunchedEffect)
                hasSubmitted = false
            }
        }
    }

    LaunchedEffect(createUserState.error) {
        createUserState.error?.let { error ->
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = error,
                    duration = SnackbarDuration.Long,
                    actionLabel = "Tancar"
                )
            }
        }
    }

    // ========== UI ==========

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crear Nou Usuari") },
                navigationIcon = {
                    if (!isNavigating) {
                        IconButton(onClick = {
                            if (!isNavigating) {
                                isNavigating = true
                                // Navegar al perfil de l'admin
                                val adminId = loginState.authResponse?.id ?: 0L
                                navController.navigate(
                                    AppScreens.UserProfileScreen.createRoute(adminId)
                                ) {
                                    popUpTo(AppScreens.AddUserScreen.route) {
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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ========== SECCIÓ: DADES BÀSIQUES ==========
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
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
                                Text("3-10 caràcters, només lletres, números i _")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Contrasenya*") },
                        leadingIcon = { Icon(Icons.Default.Lock, null) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.VisibilityOff
                                    else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible)
                            VisualTransformation.None else PasswordVisualTransformation(),
                        isError = password.isNotEmpty() && !isPasswordValid,
                        supportingText = {
                            if (password.isNotEmpty() && !isPasswordValid) {
                                Text("Mínim 6 caràcters")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = nombre,
                        onValueChange = { nombre = it },
                        label = { Text("Nom*") },
                        isError = nombre.isNotEmpty() && !isNombreValid,
                        supportingText = {
                            if (nombre.isNotEmpty() && !isNombreValid) {
                                Text("Mínim 2 caràcters")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = cognom1,
                        onValueChange = { cognom1 = it },
                        label = { Text("Primer Cognom*") },
                        isError = cognom1.isNotEmpty() && !isApellido1Valid,
                        supportingText = {
                            if (cognom1.isNotEmpty() && !isApellido1Valid) {
                                Text("Mínim 2 caràcters")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = cognom2,
                        onValueChange = { cognom2 = it },
                        label = { Text("Segon Cognom (opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Selector de rol
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Rol:", modifier = Modifier.weight(1f))
                        RadioButton(
                            selected = rol == 1,
                            onClick = { rol = 1 }
                        )
                        Text("Usuari")
                        Spacer(modifier = Modifier.width(16.dp))
                        RadioButton(
                            selected = rol == 2,
                            onClick = { rol = 2 }
                        )
                        Text("Admin")
                    }
                }
            }

            // ========== SECCIÓ: DOCUMENTACIÓ ==========
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
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
                        placeholder = { Text("12345678A") },
                        leadingIcon = { Icon(Icons.Default.Badge, null) },
                        isError = nif.isNotEmpty() && !isNifValid,
                        supportingText = {
                            if (nif.isNotEmpty() && !isNifValid) {
                                Text("Format: 8 números + 1 lletra")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            // ========== SECCIÓ: CONTACTE ==========
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
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
                        placeholder = { Text("usuari@exemple.com") },
                        leadingIcon = { Icon(Icons.Default.Email, null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        isError = email.isNotEmpty() && !isEmailValid,
                        supportingText = {
                            if (email.isNotEmpty() && !isEmailValid) {
                                Text("Format d'email invàlid")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = tlf,
                        onValueChange = { if (it.all { char -> char.isDigit() }) tlf = it },
                        label = { Text("Telèfon*") },
                        placeholder = { Text("666777888") },
                        leadingIcon = { Icon(Icons.Default.Phone, null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        isError = tlf.isNotEmpty() && !isTlfValid,
                        supportingText = {
                            if (tlf.isNotEmpty() && !isTlfValid) {
                                Text("Ha de tenir exactament 9 dígits")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            // ========== SECCIÓ: ADREÇA ==========
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
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
                        placeholder = { Text("Carrer Major, 1") },
                        leadingIcon = { Icon(Icons.Default.Home, null) },
                        isError = carrer.isEmpty() && cognom1.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
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
                            placeholder = { Text("08001") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = cp.isNotEmpty() && !isCpValid,
                            modifier = Modifier.weight(0.4f),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = localitat,
                            onValueChange = { localitat = it },
                            label = { Text("Localitat*") },
                            placeholder = { Text("Barcelona") },
                            isError = localitat.isEmpty() && cognom1.isNotEmpty(),
                            modifier = Modifier.weight(0.6f),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = provincia,
                        onValueChange = { provincia = it },
                        label = { Text("Província*") },
                        placeholder = { Text("Barcelona") },
                        leadingIcon = { Icon(Icons.Default.LocationOn, null) },
                        isError = provincia.isEmpty() && cognom1.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            // ========== BOTÓ CREAR ==========
            Button(
                onClick = {
                    hasSubmitted = true
                    authViewModel.createUser(
                        nick = nick,
                        password = password,
                        nom = nombre,
                        cognom1 = cognom1,
                        cognom2 = cognom2.ifEmpty { null },
                        rol = rol,
                        nif = nif,
                        email = email,
                        tlf = tlf,
                        carrer = carrer,
                        localitat = localitat,
                        cp = cp,
                        provincia = provincia
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = isFormValid
            ) {
                if (createUserState.isCreating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Default.PersonAdd, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("CREAR USUARI")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}