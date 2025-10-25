package com.oscar.bibliosedaos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import com.oscar.bibliosedaos.ui.screens.*
import com.oscar.bibliosedaos.ui.theme.BibliotecaCloudTheme
import com.oscar.bibliosedaos.ui.viewmodels.AuthViewModel
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch


class MainActivity : ComponentActivity() {
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



@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val hasToken = TokenManager.hasToken()
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

        composable(AppScreens.EditProfileScreen.route) {
            EditProfileScreen(
                navController = navController,
                authViewModel = authViewModel
            )
        }
        composable(AppScreens.ChangePasswordScreen.route) {
            ChangePasswordScreen(
                navController = navController,
                authViewModel = authViewModel
            )
        }
        // ========== PANTALLES FUTURES ==========
        composable(AppScreens.BooksScreen.route) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                androidx.compose.foundation.layout.Box(
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    androidx.compose.material3.Text("Pantalla de Libros - Próximamente")
                }
            }
        }
    }
}