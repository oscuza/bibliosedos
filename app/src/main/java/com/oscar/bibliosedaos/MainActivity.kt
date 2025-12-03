package com.oscar.bibliosedaos

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.oscar.bibliosedaos.data.network.ApiClient
import com.oscar.bibliosedaos.data.network.TokenManager
import com.oscar.bibliosedaos.navigation.AppScreens
import com.oscar.bibliosedaos.ui.screens.auth.LoginScreen
import com.oscar.bibliosedaos.ui.screens.profile.ProfileScreen
import com.oscar.bibliosedaos.ui.screens.profile.EditProfileScreen
import com.oscar.bibliosedaos.ui.screens.profile.ChangePasswordScreen
import com.oscar.bibliosedaos.ui.screens.books.BooksScreen
import com.oscar.bibliosedaos.ui.screens.books.BookManagementScreen
import com.oscar.bibliosedaos.ui.screens.books.AddBookScreen
import com.oscar.bibliosedaos.ui.screens.books.EditBookScreen
import com.oscar.bibliosedaos.ui.screens.books.AddExemplarScreen
import com.oscar.bibliosedaos.ui.screens.loans.MyLoansScreen
import com.oscar.bibliosedaos.ui.screens.loans.UsersWithLoansScreen
import com.oscar.bibliosedaos.ui.screens.loans.LoanHistoryScreen
import com.oscar.bibliosedaos.ui.screens.loans.LoanManagementScreen
import com.oscar.bibliosedaos.ui.screens.admin.AdminHomeScreen
import com.oscar.bibliosedaos.ui.screens.admin.AddUserScreen
import com.oscar.bibliosedaos.ui.screens.admin.UserSearchScreen
import com.oscar.bibliosedaos.ui.screens.admin.OverdueLoansScreen
import com.oscar.bibliosedaos.ui.screens.groups.HorarisScreen
import com.oscar.bibliosedaos.ui.screens.groups.GroupsScreen
import com.oscar.bibliosedaos.ui.screens.groups.GroupDetailScreen
import com.oscar.bibliosedaos.ui.screens.groups.AddEditGroupScreen
import com.oscar.bibliosedaos.ui.theme.BibliotecaCloudTheme
import com.oscar.bibliosedaos.ui.viewmodels.AuthViewModel
import androidx.lifecycle.lifecycleScope
import com.oscar.bibliosedaos.ui.viewmodels.BookViewModel
import com.oscar.bibliosedaos.ui.viewmodels.LoanViewModel
import com.oscar.bibliosedaos.ui.viewmodels.GroupViewModel
import kotlinx.coroutines.launch


/**
 * Activitat principal de l'aplicació Biblioteca Cloud.
 * 
 * Aquesta classe és el punt d'entrada de l'aplicació Android i gestiona el cicle de vida
 * de l'aplicació, incloent la configuració de la interfície d'usuari amb Jetpack Compose
 * i la navegació entre pantalles.
 * 
 * **Funcionalitats Principals:**
 * - Configuració del tema de l'aplicació
 * - Inicialització de la navegació
 * - Gestió del logout automàtic quan l'app es tanca
 * 
 * **Navegació:**
 * Utilitza Jetpack Compose Navigation per gestionar la navegació entre pantalles.
 * La navegació es defineix a [AppNavigation] amb totes les rutes disponibles.
 * 
 * **Gestió de Sessió:**
 * - Realitza logout automàtic quan l'aplicació es destrueix
 * - Neteja el token JWT local per seguretat
 * - Notifica al servidor del logout si és possible
 * 
 * @author Oscar
 * @since 1.0
 * @see AppNavigation
 * @see AppScreens
 * @see TokenManager
 */
class MainActivity : ComponentActivity() {
    
    /**
     * Inicialitza l'activitat i configura la interfície d'usuari.
     * 
     * Aquest mètode s'executa quan l'activitat es crea per primera vegada.
     * Configura el tema de l'aplicació i inicialitza la navegació principal.
     * 
     * **Processos Realitzats:**
     * 1. Crida al mètode super.onCreate() per inicialització base
     * 2. Configura el contingut amb Jetpack Compose
     * 3. Aplica el tema de l'aplicació (BibliotecaCloudTheme)
     * 4. Inicialitza la navegació amb [AppNavigation]
     * 
     * **Requeriments:**
     * Requereix Android API level 26 (Android 8.0) o superior per utilitzar
     * les funcionalitats de Java 8 Time API.
     * 
     * @param savedInstanceState Estat guardat de la instància anterior (null si és primera vegada)
     * 
     * @author Oscar
     * @since 1.0
     */
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BibliotecaCloudTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
    
    /**
     * Neteja recursos i realitza logout automàtic quan l'aplicació es destrueix.
     * 
     * Aquest mètode s'executa quan l'activitat es destrueix, ja sigui per:
     * - L'usuari tanca l'aplicació
     * - El sistema operatiu finalitza l'aplicació per manca de memòria
     * - L'activitat es finalitza manualment
     * 
     * **Processos Realitzats:**
     * 1. Crida al mètode super.onDestroy() per neteja base
     * 2. Comprova si hi ha un token actiu
     * 3. Si hi ha token:
     *    - Intenta notificar al servidor del logout (opcional)
     *    - Neteja el token local sempre (seguretat)
     * 
     * **Seguretat:**
     * El token sempre s'elimina localment, encara que falli la comunicació
     * amb el servidor. Això assegura que no quedi cap token a l'aplicació
     * després de tancar-la.
     * 
     * **Nota:**
     * El logout al servidor és opcional i no bloqueja la neteja local del token.
     * Si hi ha problemes de connexió, el token es neteja igualment per seguretat.
     * 
     * @author Oscar
     * @since 1.0
     * @see TokenManager.hasToken
     * @see TokenManager.clearToken
     */
    override fun onDestroy() {
        super.onDestroy()

        // Tan sol fa logout si n'hi ha token actiu
        if (TokenManager.hasToken()) {
            lifecycleScope.launch {
                try {
                    // Intentar notificar al servidor
                    ApiClient.instance.logout()
                    android.util.Log.d("MainActivity", "Logout enviado al servidor")
                } catch (e: Exception) {
                    // Si falla la conexión, no importa
                    android.util.Log.e("MainActivity", "Error enviando logout: ${e.message}")
                } finally {
                    // Siempre limpiar el token local
                    TokenManager.clearToken()
                }
            }
        }
    }
}



/**
 * Funció composable que gestiona la navegació principal de l'aplicació.
 * 
 * Aquesta funció configura el sistema de navegació de Jetpack Compose Navigation
 * amb totes les pantalles disponibles a l'aplicació. Defineix les rutes i els
 * paràmetres necessaris per cada pantalla.
 * 
 * **Estructura de Navegació:**
 * - **Pantalla Inicial**: LoginScreen (si no hi ha token)
 * - **Pantalles d'Autenticació**: LoginScreen
 * - **Pantalles d'Administrador**: AdminHomeScreen, AddUserScreen, UserSearchScreen
 * - **Pantalles de Perfil**: ProfileScreen (ruta: UserProfileScreen), EditProfileScreen, ChangePasswordScreen
 * - **Pantalles de Llibres**: BooksScreen, BookManagementScreen, AddBookScreen, EditBookScreen, AddExemplarScreen
 * - **Pantalles de Préstecs**: MyLoansScreen, UsersWithLoansScreen, LoanHistoryScreen, OverdueLoansScreen, LoanManagementScreen
 * 
 * **ViewModels Compartits:**
 * - [AuthViewModel]: Gestió d'autenticació i usuaris
 * - [BookViewModel]: Gestió de llibres, autors i exemplars
 * - [LoanViewModel]: Gestió de préstecs
 * 
 * **Gestió de Tokens:**
 * La navegació inicial comprova si hi ha un token actiu per decidir la pantalla
 * de destinació inicial. Actualment sempre comença amb LoginScreen.
 * 
 * **Paràmetres de Ruta:**
 * Algunes pantalles accepten paràmetres dinàmics:
 * - `userId`: Per mostrar perfils d'usuaris específics
 * - `bookId`: Per editar llibres específics
 * 
 * @author Oscar
 * @since 1.0
 * @see AppScreens
 * @see AuthViewModel
 * @see BookViewModel
 * @see LoanViewModel
 */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val asToken = TokenManager.hasToken()
    val bookViewModel: BookViewModel = viewModel()
    val loanViewModel: LoanViewModel = viewModel()
    val groupViewModel: GroupViewModel = viewModel()
    NavHost(
        navController = navController,
        startDestination =
            AppScreens.LoginScreen.route
    ) {
        // ========== PANTALLA DE LOGIN ==========
        composable(AppScreens.LoginScreen.route) {
            LoginScreen(
                navController = navController,
                authViewModel = authViewModel
            )
        }

        // ========== PANTALLES DE ADMIN ==========
        composable(AppScreens.AdminHomeScreen.route) {
            AdminHomeScreen(
                navController = navController,
                authViewModel = authViewModel
            )
        }

        composable(AppScreens.AddUserScreen.route) {
            AddUserScreen(
                navController = navController,
                authViewModel = authViewModel
            )
        }

        // ========== PANTALLA DE CERCA D'USUARIS ==========
        composable(AppScreens.UserSearchScreen.route) {
            UserSearchScreen(
                navController = navController,
                authViewModel = authViewModel
            )
        }

        // ========== PANTALLA DE PERFIL (Compartida: Admin y User) ==========
        composable(
            route = AppScreens.UserProfileScreen.route,
            arguments = listOf(
                navArgument("userId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getLong("userId") ?: 0L
            // ProfileScreen s'adapta automáticament depenent el rol
            ProfileScreen(
                userId = userId,
                navController = navController,
                authViewModel = authViewModel
            )
        }

        composable(
            route = AppScreens.EditProfileScreen.route,
            arguments = listOf(
                navArgument("userId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getLong("userId") ?: -1L
            EditProfileScreen(
                navController = navController,
                authViewModel = authViewModel,
                userId = if (userId != -1L) userId else null
            )
        }
        composable(AppScreens.ChangePasswordScreen.route) {
            ChangePasswordScreen(
                navController = navController,
                authViewModel = authViewModel
            )
        }
        // ========== PANTALLES DE GESTIÓ DE LLIBRES (ADMIN) ==========

// Pantalla principal de gestió del catàleg
        composable(AppScreens.BookManagementScreen.route) {
            BookManagementScreen(
                navController = navController,
                bookViewModel = bookViewModel
            )
        }

// Pantalla per afegir nou llibre
        composable(AppScreens.AddBookScreen.route) {
            AddBookScreen(
                navController = navController,
                bookViewModel = bookViewModel
            )
        }

// Pantalla per editar llibre existent

        composable(
            route = AppScreens.EditBookScreen.route,
            arguments = listOf(
                navArgument("bookId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getLong("bookId") ?: 0L
            EditBookScreen(
                bookId = bookId,
                navController = navController,
                bookViewModel = bookViewModel
            )
        }


// Pantalla per afegir nou exemplar
        composable(AppScreens.AddExemplarScreen.route) {
            AddExemplarScreen(
                navController = navController,
                bookViewModel = bookViewModel
            )
        }

// ========== PANTALLA DE CATÀLEG DE LLIBRES (USUARIS) ==========

        /**
         * Pantalla del catàleg de llibres.
         * Accessible per usuaris normals i administradors.
         * Mostra tots els llibres amb informació de disponibilitat.
         */
        composable(AppScreens.BooksScreen.route) {
            BooksScreen(
                navController = navController,
                bookViewModel = bookViewModel,
                authViewModel = authViewModel,
                loanViewModel = loanViewModel
            )
        }

// ========== PANTALLA DE PRÉSTECS ACTIUS ==========

        /**
         * Pantalla de préstecs actius de l'usuari.
         * Mostra els llibres que l'usuari té prestats actualment.
         * Accepta userId opcional per veure préstecs d'altres usuaris (admin).
         */
        composable(
            route = AppScreens.MyLoansScreen.route,
            arguments = listOf(
                navArgument("userId") {
                    type = NavType.LongType
                    defaultValue = -1L  // -1 indica "usuari actual"
                    nullable = false
                }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getLong("userId") ?: -1L

            MyLoansScreen(
                navController = navController,
                loanViewModel = loanViewModel,
                authViewModel = authViewModel,
                userId = if (userId == -1L) null else userId
            )
        }

        // ========== PANTALLA D'USUARIS AMB PRÉSTECS (ADMIN) ==========

        /**
         * Pantalla que mostra tots els usuaris amb préstecs actius.
         * Només accessible per administradors.
         */
        composable(AppScreens.UsersWithLoansScreen.route) {
            UsersWithLoansScreen(
                navController = navController,
                loanViewModel = loanViewModel,
                authViewModel = authViewModel
            )
        }

        // ========== PANTALLA D'HISTORIAL DE PRÉSTECS ==========

        /**
         * Pantalla de historial complet de préstecs de l'usuari.
         * Mostra tots els préstecs (actius i retornats) d'un usuari.
         * Accepta userId opcional per veure historial d'altres usuaris (admin).
         */
        composable(
            route = AppScreens.LoanHistoryScreen.route,
            arguments = listOf(
                navArgument("userId") {
                    type = NavType.LongType
                    defaultValue = -1L  // -1 indica "usuari actual"
                    nullable = false
                }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getLong("userId") ?: -1L

            LoanHistoryScreen(
                navController = navController,
                loanViewModel = loanViewModel,
                authViewModel = authViewModel,
                userId = if (userId == -1L) null else userId
            )
        }

        // ========== PANTALLA DE PRÉSTECS EN RETARD (ADMIN) ==========

        /**
         * Pantalla que mostra tots els usuaris amb préstecs en retard o retornats tard.
         * Només accessible per administradors.
         */
        composable(AppScreens.OverdueLoansScreen.route) {
            OverdueLoansScreen(
                navController = navController,
                loanViewModel = loanViewModel,
                authViewModel = authViewModel
            )
        }

        // ========== PANTALLA DE GESTIÓ DE PRÉSTECS (ADMIN) ==========

        /**
         * Pantalla principal de gestió de préstecs per administradors.
         * Proporciona accés centralitzat a totes les funcionalitats relacionades amb préstecs.
         * Només accessible per administradors.
         */
        composable(AppScreens.LoanManagementScreen.route) {
            LoanManagementScreen(
                navController = navController,
                loanViewModel = loanViewModel,
                authViewModel = authViewModel
            )
        }

        // ========== PANTALLA DE HORARIS ==========

        /**
         * Pantalla per veure els horaris disponibles de les sales.
         * Accessible per tots els usuaris autenticats.
         */
        composable(AppScreens.HorarisScreen.route) {
            HorarisScreen(
                navController = navController,
                groupViewModel = groupViewModel
            )
        }

        // ========== PANTALLES DE GRUPS DE LECTURA ==========
        composable(AppScreens.GroupsScreen.route) {
            GroupsScreen(
                navController = navController,
                groupViewModel = groupViewModel,
                authViewModel = authViewModel
            )
        }

        composable(
            route = AppScreens.GroupDetail.route,
            arguments = listOf(
                navArgument("grupId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val grupId = backStackEntry.arguments?.getLong("grupId") ?: 0L
            GroupDetailScreen(
                navController = navController,
                grupId = grupId,
                groupViewModel = groupViewModel,
                authViewModel = authViewModel
            )
        }

        composable(AppScreens.AddEditGroup.routeCreate) {
            AddEditGroupScreen(
                navController = navController,
                grupId = null,
                groupViewModel = groupViewModel,
                authViewModel = authViewModel
            )
        }

        composable(
            route = AppScreens.AddEditGroup.route,
            arguments = listOf(
                navArgument("grupId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val grupId = backStackEntry.arguments?.getLong("grupId") ?: 0L
            AddEditGroupScreen(
                navController = navController,
                grupId = grupId,
                groupViewModel = groupViewModel,
                authViewModel = authViewModel
            )
        }
    }
}