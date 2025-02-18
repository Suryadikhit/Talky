package com.example.talky.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.talky.R
import com.example.talky.screens.CallsScreen
import com.example.talky.screens.ChatsScreen
import com.example.talky.screens.HomeScreen
import com.example.talky.screens.IntroScreen
import com.example.talky.screens.LoginScreen
import com.example.talky.screens.OtpScreen
import com.example.talky.screens.ProfileSetupScreen
import com.example.talky.screens.SplashScreen
import com.example.talky.screens.StoriesScreen
import com.example.talky.viewmodels.AuthViewModel

sealed class Screen(val route: String, val title: String = "", val icon: Int = 0) {
    data object Splash : Screen("splash_screen")
    data object Intro : Screen("intro_screen")
    data object Login : Screen("login_screen")
    data object Otp : Screen("otp_screen/{verificationId}/{phoneNumber}")
    data object ProfileSetup : Screen("profile_setup_screen/{phoneNumber}")
    data object Main : Screen("main_screen") // ✅ Main screen
    data object Chats : Screen("chats_screen", "Chats", R.drawable.chat)
    data object Calls : Screen("calls_screen", "Calls", R.drawable.phone)
    data object Stories : Screen("stories_screen", "Stories", R.drawable.story)
}

@Composable
fun NavGraph(navController: NavHostController, viewModel: AuthViewModel) {
    // Observe the authentication status from the ViewModel
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()

    // Set the start destination based on authentication status
    val startDestination = if (isAuthenticated) Screen.Main.route else Screen.Intro.route

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Splash.route) { SplashScreen(navController) }
        composable(Screen.Intro.route) { IntroScreen(navController) }
        composable(Screen.Login.route) { LoginScreen(navController, viewModel) }
        composable(
            route = Screen.Otp.route,
            arguments = listOf(
                navArgument("verificationId") { defaultValue = "" },
                navArgument("phoneNumber") { defaultValue = "" }
            )
        ) { backStackEntry ->
            val verificationId = backStackEntry.arguments?.getString("verificationId") ?: ""
            val phoneNumber = backStackEntry.arguments?.getString("phoneNumber") ?: ""
            OtpScreen(navController, verificationId, phoneNumber, viewModel)
        }
        composable(
            route = Screen.ProfileSetup.route,
            arguments = listOf(
                navArgument("phoneNumber") { defaultValue = "" }
            )
        ) { backStackEntry ->
            val phoneNumber = backStackEntry.arguments?.getString("phoneNumber") ?: ""
            ProfileSetupScreen(navController, viewModel, phoneNumber)
        }
        composable(Screen.Main.route) { HomeScreen(viewModel) }
        composable(Screen.Chats.route) { ChatsScreen(viewModel) }
        composable(Screen.Chats.route) { CallsScreen() }
        composable(Screen.Chats.route) { StoriesScreen() }
    }
}
