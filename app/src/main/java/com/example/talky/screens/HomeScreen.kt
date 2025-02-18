package com.example.talky.screens

import android.annotation.SuppressLint
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.talky.navigation.Screen
import com.example.talky.viewmodels.AuthViewModel

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun HomeScreen(viewModel: AuthViewModel) {
    val bottomNavController = rememberNavController() // ✅ Separate NavController for BottomBar

    Scaffold(
        bottomBar = { BottomNavBar(bottomNavController) }
    ) {
        BottomNavGraph(bottomNavController, viewModel) // ✅ Nested navigation
    }
}

@Composable
fun BottomNavGraph(navController: NavHostController, viewModel: AuthViewModel) {
    NavHost(navController = navController, startDestination = Screen.Chats.route) {
        composable(Screen.Chats.route) { ChatsScreen(viewModel) }
        composable(Screen.Calls.route) { CallsScreen() }
        composable(Screen.Stories.route) { StoriesScreen() }
    }
}

@Composable
fun BottomNavBar(navController: NavHostController) {
    val bottomNavItems = listOf(Screen.Chats, Screen.Calls, Screen.Stories)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(modifier = Modifier.height(85.dp)) { // 🔹 Standard height for better UI
        bottomNavItems.forEach { screen ->
            val isSelected = currentRoute == screen.route

            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.3f else 1.0f,
                animationSpec = tween(durationMillis = 300), label = "iconScale"
            )

            val iconColor by animateColorAsState(
                targetValue = if (isSelected) Color.Blue else Color.Gray,
                animationSpec = tween(durationMillis = 300), label = "iconColor"
            )

            NavigationBarItem(
                icon = {
                    Icon(
                        painter = painterResource(id = screen.icon),
                        contentDescription = screen.title,
                        modifier = Modifier.size((24 * scale).dp),
                        tint = iconColor
                    )
                },
                label = { Text(screen.title) },
                selected = isSelected,
                onClick = {
                    if (!isSelected) {
                        navController.navigate(screen.route) {
                            launchSingleTop = true // ✅ Prevents duplicate navigation
                            restoreState = true // ✅ Restores previous state
                        }
                    }
                }
            )
        }
    }
}
