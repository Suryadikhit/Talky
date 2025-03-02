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
import com.example.talky.screens.ChatListScreen
import com.example.talky.screens.ChatScreen
import com.example.talky.screens.IntroScreen
import com.example.talky.screens.LoginScreen
import com.example.talky.screens.NewMessageScreen
import com.example.talky.screens.OtpScreen
import com.example.talky.screens.ProfileSetupScreen
import com.example.talky.screens.SplashScreen
import com.example.talky.screens.StoriesScreen
import com.example.talky.viewmodels.AuthViewModel
import com.example.talky.viewmodels.ChatViewModel
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed class Screen(val route: String, val title: String = "", val icon: Int = 0) {
    data object Splash : Screen("splash_screen")
    data object Intro : Screen("intro_screen")
    data object Login : Screen("login_screen")
    data object Otp : Screen("otp_screen/{verificationId}/{phoneNumber}")
    data object ProfileSetup : Screen("profile_setup_screen/{phoneNumber}")
    data object ChatList : Screen("chats_screen", "Chats", R.drawable.chat)
    data object Calls : Screen("calls_screen", "Calls", R.drawable.phone)
    data object Stories : Screen("stories_screen", "Stories", R.drawable.story)
    data object Newmessage : Screen("newmessage_screen", "New Message")

    // ✅ Chat screen route with userId and userName as arguments
    data object Chat :
        Screen("chat_Screen/{userId}/{userName}") // changed `chat_Screen` to `chat_screen`

}


@Composable
fun NavGraph(
    navController: NavHostController,
    authviewModel: AuthViewModel,
    chatViewModel: ChatViewModel
) {
    val isAuthenticated by authviewModel.isAuthenticated.collectAsState()
    val startDestination = if (isAuthenticated) Screen.ChatList.route else Screen.Intro.route

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Splash.route) { SplashScreen(navController) }
        composable(Screen.Intro.route) { IntroScreen(navController) }
        composable(Screen.Login.route) { LoginScreen(navController, authviewModel) }

        composable(
            route = Screen.Otp.route,
            arguments = listOf(
                navArgument("verificationId") { defaultValue = "" },
                navArgument("phoneNumber") { defaultValue = "" }
            )
        ) { backStackEntry ->
            val verificationId = backStackEntry.arguments?.getString("verificationId") ?: ""
            val phoneNumber = backStackEntry.arguments?.getString("phoneNumber") ?: ""
            OtpScreen(navController, verificationId, phoneNumber, authviewModel)
        }

        composable(
            route = Screen.ProfileSetup.route,
            arguments = listOf(
                navArgument("phoneNumber") { defaultValue = "" }
            )
        ) { backStackEntry ->
            val phoneNumber = backStackEntry.arguments?.getString("phoneNumber") ?: ""
            ProfileSetupScreen(navController, authviewModel, phoneNumber)
        }

        // ✅ Bottom Navigation Screens
        composable(Screen.ChatList.route) {
            ChatListScreen(
                authviewModel,
                chatViewModel,
                navController
            )
        }

        composable(Screen.Calls.route) { CallsScreen(navController) }
        composable(Screen.Stories.route) { StoriesScreen(navController) }

        composable(Screen.Newmessage.route) {
            NewMessageScreen(
                authviewModel,
                onBackClick = { navController.popBackStack() },
                onMenuClick = { /* Handle menu actions */ },
                onUserClick = { userId, userName ->

                    navController.navigate(
                        "chat_Screen/${
                            URLEncoder.encode(
                                userId,
                                StandardCharsets.UTF_8.name()
                            )
                        }/${URLEncoder.encode(userName, StandardCharsets.UTF_8.name())}"
                    )
                }
            )
        }

        // ✅ Chat Screen Navigation
        composable(
            route = Screen.Chat.route,
            arguments = listOf(
                navArgument("userId") { defaultValue = "" },
                navArgument("userName") { defaultValue = "" }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")
                ?.takeIf { it.isNotBlank() }
                ?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.name()) } ?: "Unknown"

            val userName = backStackEntry.arguments?.getString("userName")
                ?.takeIf { it.isNotBlank() }
                ?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.name()) } ?: "User"

            val profileImage by chatViewModel.getUserProfileImage(userId).collectAsState(initial = "")

            ChatScreen(
                chatViewModel = chatViewModel,
                otherUserId = userId,
                otherUserName = userName,
                onEmojiClick = { /* Open Emoji Picker */ },
                onCameraClick = { /* Open Camera */ },
                onMicClick = { /* Handle mic action */ },
                onCallClick = { /* Handle call action */ },
                onVideoCallClick = {},
                profileImageUrl = profileImage, // ✅ Now it dynamically fetches the image
            )
    }
}
}


