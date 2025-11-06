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
import com.oscar.bibliosedaos.ui.screens.admin.AdminHomeScreen
import com.oscar.bibliosedaos.ui.screens.admin.AddUserScreen
import com.oscar.bibliosedaos.ui.screens.admin.UserSearchScreen
import com.oscar.bibliosedaos.ui.screens.admin.OverdueLoansScreen
import com.oscar.bibliosedaos.ui.theme.BibliotecaCloudTheme
import com.oscar.bibliosedaos.ui.viewmodels.AuthViewModel
import androidx.lifecycle.lifecycleScope
import com.oscar.bibliosedaos.ui.viewmodels.BookViewModel
import com.oscar.bibliosedaos.ui.viewmodels.LoanViewModel
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
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
     * Logout automàtic quan l'app es destrueix
     * (usuari tanca l'app o el sistema el finalitza)
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



@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val hasToken = TokenManager.hasToken()
    val bookViewModel: BookViewModel = viewModel()
    val loanViewModel: LoanViewModel = viewModel()
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
    }
}