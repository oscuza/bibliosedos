package com.oscar.bibliosedaos.ui.screens.loans

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.oscar.bibliosedaos.navigation.AppScreens
import com.oscar.bibliosedaos.ui.viewmodels.AuthViewModel
import com.oscar.bibliosedaos.ui.viewmodels.LoanViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Pantalla de gestió d'usuaris amb préstecs actius.
 *
 * **Descripció:**
 * Pantalla exclusiva per a administradors que mostra un llistat de tots els usuaris
 * que tenen llibres prestats actualment.
 *
 * **Implementació:**
 * Aquesta versió NO requereix modificar el backend. Funciona de la següent manera:
 * 1. Carrega tots els préstecs actius del sistema (usuariId = null)
 * 2. Extreu els usuaris únics d'aquests préstecs
 * 3. Compta quants préstecs té cada usuari
 * 4. Mostra només els usuaris amb almenys un préstec actiu
 *
 * **Funcionalitats:**
 * - Llistat d'usuaris amb préstecs actius ordenats per dies restants
 * - Indicador del nombre de llibres prestats per usuari
 * - Mostra els dies restants per retornar el llibre (el més proper a vencer)
 * - Ordenació automàtica: usuaris amb menys dies restants apareixen primer
 * - Indicadors visuals de urgència (vermell per retards, taronja per <5 dies)
 * - Navegació ràpida als préstecs de cada usuari
 * - Refrescament automàtic al tornar a la pantalla
 * - Colors segons la quantitat de préstecs
 *
 * **Accés:**
 * Només accessible per usuaris amb rol d'administrador (rol=2).
 *
 * **Característiques Visuals:**
 * - Badge amb el número de préstecs actius
 * - Colors diferents segons la quantitat de préstecs:
 *   - 1-2 préstecs: color primari (normal)
 *   - 3-4 préstecs: color tercer (atenció)
 *   - 5+ préstecs: color d'error (alerta)
 *
 * @param navController Controlador de navegació per transicions entre pantalles
 * @param loanViewModel ViewModel per gestionar operacions de préstecs
 * @param authViewModel ViewModel per gestionar informació d'usuaris
 *
 * @author Oscar
 * @since 1.0
 * @see MyLoansScreen
 * @see AdminHomeScreen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersWithLoansScreen(
    navController: NavController,
    loanViewModel: LoanViewModel,
    authViewModel: AuthViewModel
) {
    // ========== Estats Observables ==========

    /**
     * Estat dels préstecs actius (tots els del sistema).
     * Utilitzem aquest estat per processar els usuaris amb préstecs.
     */
    val activeLoansState by loanViewModel.activeLoansState.collectAsState()

    /**
     * Estat del login per verificar que és administrador.
     */
    val loginState by authViewModel.loginUiState.collectAsState()

    // ========== Estats Locals ==========

    /**
     * Flag per prevenir doble clic al botó enrere.
     */
    var isNavigating by remember { mutableStateOf(false) }

    /**
     * Llista processada d'usuaris amb préstecs.
     * Aquesta llista es genera a partir dels préstecs actius.
     */
    var usersWithLoans by remember { mutableStateOf<List<UserWithLoansInfo>>(emptyList()) }

    // ========== Verificació de Permisos ==========

    val isAdmin = loginState.authResponse?.rol == 2
    val currentUserId = loginState.authResponse?.id

    LaunchedEffect(isAdmin) {
        if (!isAdmin) {
            navController.navigate(
                AppScreens.UserProfileScreen.createRoute(currentUserId ?: 0L)
            ) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    // ========== Càrrega Inicial de Dades ==========

    /**
     * Carrega tots els préstecs actius del sistema.
     * Passa null com a usuariId per obtenir tots els préstecs.
     */
    LaunchedEffect(Unit) {
        loanViewModel.loadActiveLoans(usuariId = null) // null = tots els préstecs actius
    }

    // ========== Processament de Dades ==========

    /**
     * Effect que processa els préstecs actius per obtenir
     * la llista d'usuaris únics amb els seus comptadors i dies restants.
     *
     * **Lògica:**
     * 1. Agrupa préstecs per usuari
     * 2. Compta quants préstecs té cada usuari
     * 3. Calcula els dies restants per cada préstec i troba el mínim
     * 4. Crea objectes UserWithLoansInfo
     * 5. Ordena per dies restants (ascendent: menys dies primer)
     */
    LaunchedEffect(activeLoansState.loans) {
        if (activeLoansState.loans.isNotEmpty()) {
            // Agrupa els préstecs per usuari
            val userLoansMap = activeLoansState.loans
                .filter { it.usuari != null }
                .groupBy { it.usuari!!.id }

            // Crear la llista de UserWithLoansInfo amb càlcul de dies restants
            usersWithLoans = userLoansMap.map { (userId, prestecs) ->
                val user = prestecs.first().usuari!!
                
                // Calcular els dies restants per cada préstec i trobar el mínim
                val daysRemainingList = prestecs.mapNotNull { prestec ->
                    calculateDaysRemaining(prestec.dataPrestec)
                }
                val minDaysRemaining = daysRemainingList.minOrNull() ?: 0
                
                UserWithLoansInfo(
                    userId = userId,
                    nick = user.nick,
                    fullName = "${user.nom} ${user.cognom1 ?: ""}".trim(),
                    loanCount = prestecs.size,
                    minDaysRemaining = minDaysRemaining
                )
            }.sortedBy { it.minDaysRemaining } // Ordenar per dies restants (menys dies primer)
        } else {
            usersWithLoans = emptyList()
        }
    }

    // ========== Cicle de Vida i Refrescament ==========

    /**
     * Recarrega els préstecs quan l'usuari torna a la pantalla.
     */
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                loanViewModel.loadActiveLoans(usuariId = null)
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // ========== UI Principal ==========

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Usuaris amb Préstecs")
                        Text(
                            text = "${usersWithLoans.size} usuaris amb llibres prestats",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                },
                navigationIcon = {
                    if (!isNavigating) {
                        IconButton(onClick = {
                            if (!isNavigating) {
                                isNavigating = true
                                navController.navigateUp()
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Enrere")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
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
                activeLoansState.isLoading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(50.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Carregant usuaris amb préstecs...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // ========== Estat: Error ==========
                activeLoansState.error != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Error",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = activeLoansState.error ?: "Error desconegut",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(24.dp))
                        Button(onClick = {
                            loanViewModel.loadActiveLoans(usuariId = null)
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Tornar a intentar")
                        }
                    }
                }

                // ========== Estat: Sense Usuaris amb Préstecs ==========
                usersWithLoans.isEmpty() && !activeLoansState.isLoading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Cap usuari amb préstecs actius",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Tots els llibres han estat retornats",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // ========== Estat: Llista amb Dades ==========
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // ========== Header amb Informació ==========
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "Usuaris amb préstecs",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                            Text(
                                                text = "${usersWithLoans.size}",
                                                style = MaterialTheme.typography.headlineMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }

                                        Column(
                                            horizontalAlignment = Alignment.End
                                        ) {
                                            Text(
                                                text = "Total de préstecs",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                            Text(
                                                text = "${activeLoansState.loans.size}",
                                                style = MaterialTheme.typography.headlineMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                    }

                                    Spacer(Modifier.height(12.dp))
                                    HorizontalDivider()
                                    Spacer(Modifier.height(12.dp))

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Info,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = "Fes clic en un usuari per veure els seus préstecs actius",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }

                        // ========== Llista de Cards d'Usuaris ==========
                        items(usersWithLoans) { userInfo ->
                            UserWithLoansCard(
                                userInfo = userInfo,
                                onClick = {
                                    // Navegar als préstecs de l'usuari
                                    navController.navigate(
                                        AppScreens.MyLoansScreen.createRoute(userInfo.userId)
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
 * Card d'usuari amb informació de préstecs.
 *
 * **Descripció:**
 * Mostra la informació de l'usuari amb un badge indicant el nombre
 * de préstecs actius. El color del badge varia segons la quantitat.
 *
 * @param userInfo Informació de l'usuari amb el comptador de préstecs
 * @param onClick Callback que s'executa en fer clic a la card
 *
 * @author Oscar
 * @since 1.0
 */
@Composable
fun UserWithLoansCard(
    userInfo: UserWithLoansInfo,
    onClick: () -> Unit
) {
    // Determinar el color del badge segons el nombre de préstecs
    val badgeColor = when {
        userInfo.loanCount >= 5 -> MaterialTheme.colorScheme.errorContainer
        userInfo.loanCount >= 3 -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.primaryContainer
    }

    val badgeTextColor = when {
        userInfo.loanCount >= 5 -> MaterialTheme.colorScheme.onErrorContainer
        userInfo.loanCount >= 3 -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ========== Avatar amb Icona ==========

            Surface(
                modifier = Modifier.size(56.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            // ========== Informació de l'Usuari ==========

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = userInfo.fullName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "@${userInfo.nick}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // ========== Badge amb Comptador ==========

            Surface(
                shape = MaterialTheme.shapes.medium,
                color = badgeColor
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = badgeTextColor
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "${userInfo.loanCount}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = badgeTextColor
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            // ========== Icona de Navegació ==========

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Veure préstecs",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Data class que representa la informació d'un usuari amb préstecs.
 *
 * @property userId ID únic de l'usuari
 * @property nick Nick o nom d'usuari
 * @property fullName Nom complet de l'usuari (nom + cognoms)
 * @property loanCount Número de préstecs actius
 * @property minDaysRemaining Dies mínims restants entre tots els préstecs (negatiu si està en retard)
 *
 * @author Oscar
 * @since 1.0
 */
data class UserWithLoansInfo(
    val userId: Long,
    val nick: String,
    val fullName: String,
    val loanCount: Int,
    val minDaysRemaining: Int = 0
)

/**
 * Calcula els dies restants per retornar un llibre.
 * 
 * **Lògica:**
 * - Període de préstec: 30 dies
 * - Calcula la diferència entre la data actual i la data límit (data de préstec + 30 dies)
 * 
 * @param dataPrestec Data del préstec en format "yyyy-MM-dd"
 * @return Nombre de dies restants (negatiu si està en retard)
 */
private fun calculateDaysRemaining(dataPrestec: String?): Int {
    if (dataPrestec == null) return 0
    
    return try {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val prestecDate = dateFormat.parse(dataPrestec) ?: return 0
        
        // Calcular la data límit (30 dies després del préstec)
        val calendar = Calendar.getInstance()
        calendar.time = prestecDate
        calendar.add(Calendar.DAY_OF_YEAR, 30) // Període de préstec: 30 dies
        val limitDate = calendar.time
        
        // Data actual
        val today = Date()
        
        // Calcular diferència en dies
        val diffInMillis = limitDate.time - today.time
        val diffInDays = (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
        
        diffInDays
    } catch (e: Exception) {
        0
    }
}