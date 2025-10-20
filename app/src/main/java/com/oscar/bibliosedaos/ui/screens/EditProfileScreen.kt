package com.oscar.bibliosedaos.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.oscar.bibliosedaos.ui.viewmodels.AuthViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Pantalla per editar el perfil de l'usuari actual.
 *
 * **Descripció:**
 * Formulari d'edició que permet als usuaris modificar les seves dades personals
 * amb validació en temps real i detecció automàtica de canvis.
 *
 * @param navController Controlador de navegació per tornar a ProfileScreen
 * @param authViewModel ViewModel amb l'estat de l'usuari i mètodes d'actualització
 *
 * @author Oscar
 * @since 1.0
 * @see AuthViewModel.updateUserProfile
 * @see User
 * @see ProfileScreen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    navController: NavController,
    authViewModel: AuthViewModel
) {
    // ========== Context i Scopes ==========

    val context = LocalContext.current
    val userProfileState by authViewModel.userProfileState.collectAsState()
    val currentUser = userProfileState.user
    val coroutineScope = rememberCoroutineScope()

    // ========== Estats Locals per als Camps ==========

    /**
     * Nick de l'usuari.
     * S'inicialitza amb el valor actual de currentUser.
     */
    var nick by remember { mutableStateOf("") }

    /**
     * Nom de l'usuari.
     */
    var nombre by remember { mutableStateOf("") }

    /**
     * Primer cognom.
     */
    var apellido1 by remember { mutableStateOf("") }

    /**
     * Segon cognom (opcional).
     */
    var apellido2 by remember { mutableStateOf("") }

    /**
     * Indica si s'està guardant actualment.
     */
    var isSaving by remember { mutableStateOf(false) }

    /**
     * Indica si hi ha canvis sense guardar.
     * Es calcula automàticament comparant amb currentUser.
     */
    var hasChanges by remember { mutableStateOf(false) }

    // ========== Estats de Validació ==========

    /**
     * Missatge d'error per al camp nick.
     */
    var nickError by remember { mutableStateOf<String?>(null) }

    /**
     * Missatge d'error per al camp nombre.
     */
    var nombreError by remember { mutableStateOf<String?>(null) }

    /**
     * Missatge d'error per al camp apellido1.
     */
    var apellido1Error by remember { mutableStateOf<String?>(null) }

    // ========== Inicialització dels Camps ==========

    /**
     * Inicialitza els camps del formulari quan es carrega l'usuari.
     * S'executa cada vegada que currentUser canvia.
     */
    LaunchedEffect(currentUser) {
        currentUser?.let {
            nick = it.nick
            nombre = it.nombre
            apellido1 = it.apellido1 ?: ""
            apellido2 = it.apellido2 ?: ""
        }
    }

    // ========== Detecció de Canvis ==========

    /**
     * Detecta si hi ha canvis sense guardar.
     * Compara els valors actuals amb els de currentUser.
     */
    LaunchedEffect(nick, nombre, apellido1, apellido2, currentUser) {
        if (currentUser != null) {
            hasChanges = nick != currentUser.nick ||
                    nombre != currentUser.nombre ||
                    apellido1 != (currentUser.apellido1 ?: "") ||
                    apellido2 != (currentUser.apellido2 ?: "")
        }
    }

    // ========== Validacions en Temps Real ==========

    /**
     * Validació en temps real del nick.
     */
    LaunchedEffect(nick) {
        nickError = when {
            nick.isBlank() -> "El nick és obligatori"
            nick.length < 3 -> "El nick ha de tenir almenys 3 caràcters"
            nick.length > 50 -> "El nick no pot tenir més de 50 caràcters"
            !nick.matches(Regex("^[a-zA-Z0-9_]+$")) -> "Només lletres, números i guions baixos"
            else -> null
        }
    }

    /**
     * Validació en temps real del nombre.
     */
    LaunchedEffect(nombre) {
        nombreError = when {
            nombre.isBlank() -> "El nom és obligatori"
            nombre.length < 2 -> "El nom ha de tenir almenys 2 caràcters"
            nombre.length > 100 -> "El nom és massa llarg"
            else -> null
        }
    }

    /**
     * Validació en temps real del primer cognom.
     */
    LaunchedEffect(apellido1) {
        apellido1Error = when {
            apellido1.isBlank() -> "El primer cognom és obligatori"
            apellido1.length < 2 -> "Ha de tenir almenys 2 caràcters"
            apellido1.length > 100 -> "És massa llarg"
            else -> null
        }
    }

    // ========== Gestió d'Errors de Càrrega ==========

    /**
     * Si no hi ha usuari carregat, mostrar error i tornar enrere.
     */
    if (currentUser == null) {
        LaunchedEffect(Unit) {
            Toast.makeText(context, "Error: No s'ha pogut carregar l'usuari", Toast.LENGTH_LONG).show()
            navController.popBackStack()
        }
        return
    }

    // ========== UI ==========

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editar Perfil") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Tornar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
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
            // ========== Card Informativa ==========

            /**
             * Card amb informació de l'usuari que s'està editant.
             * Mostra ID i rol (camps no editables).
             */
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            "Editant perfil",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            "ID: ${currentUser.id} • Rol: ${if (currentUser.rol == 2) "Admin" else "Usuari"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            // ========== Formulari d'Edició ==========

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Informació Personal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    // Camp Nick
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
                                Text(
                                    text = nickError!!,
                                    color = MaterialTheme.colorScheme.error
                                )
                            } else {
                                Text("Només lletres, números i guions baixos")
                            }
                        }
                    )

                    // Camp Nombre
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
                                Text(
                                    text = nombreError!!,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    )

                    // Camp Primer Cognom
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
                                Text(
                                    text = apellido1Error!!,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    )

                    // Camp Segon Cognom (Opcional)
                    OutlinedTextField(
                        value = apellido2,
                        onValueChange = { if (it.length <= 100) apellido2 = it },
                        label = { Text("Segon Cognom") },
                        placeholder = { Text("Ex: López (opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isSaving,
                        supportingText = { Text("Opcional") }
                    )
                }
            }

            // ========== Advertència de Canvis ==========

            /**
             * Card que apareix quan hi ha canvis sense guardar.
             */
            if (hasChanges) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Save,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Tens canvis sense guardar",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ========== Botó Guardar ==========

            /**
             * Botó per guardar els canvis.
             * Només habilitat si hi ha canvis i no hi ha errors.
             */
            Button(
                onClick = {
                    // Validació final
                    if (nickError != null || nombreError != null || apellido1Error != null) {
                        Toast.makeText(
                            context,
                            "Si us plau, corregeix els errors abans de guardar",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@Button
                    }

                    isSaving = true
                    val updatedUser = currentUser.copy(
                        nick = nick.trim(),
                        nombre = nombre.trim(),
                        apellido1 = apellido1.trim(),
                        apellido2 = apellido2.trim().ifEmpty { null }
                    )

                    coroutineScope.launch {
                        authViewModel.updateUserProfile(currentUser.id, updatedUser) { success, message ->
                            isSaving = false

                            if (success) {
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                coroutineScope.launch {
                                    delay(300)
                                    navController.popBackStack()
                                }
                            } else {
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !isSaving &&
                        hasChanges &&
                        nickError == null &&
                        nombreError == null &&
                        apellido1Error == null
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
                    Text("GUARDAR CANVIS")
                }
            }

            // ========== Botó Cancel·lar ==========

            OutlinedButton(
                onClick = {
                    if (hasChanges) {
                        Toast.makeText(
                            context,
                            "Canvis descartats",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    navController.popBackStack()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving
            ) {
                Text("CANCEL·LAR")
            }

            // ========== Nota Informativa ==========

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
                        "• El rol no pot ser modificat\n" +
                                "• L'ID d'usuari és permanent\n" +
                                "• Els camps marcats amb * són obligatoris",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}