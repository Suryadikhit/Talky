package com.example.talky.components

import android.util.Log
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.talky.navigation.Screen

@Composable
fun BottomNavBar(navController: NavController) {
    val bottomNavItems = listOf(Screen.ChatList, Screen.Calls, Screen.Stories)

    var selectedIndex by remember { mutableIntStateOf(0) }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Use a LaunchedEffect with debounce
    LaunchedEffect(currentRoute) {
        currentRoute?.let { route ->
            val newIndex = when (route) {
                Screen.ChatList.route -> 0
                Screen.Calls.route -> 1
                Screen.Stories.route -> 2
                else -> selectedIndex
            }
            if (newIndex != selectedIndex) {  // Prevent unnecessary recompositions
                selectedIndex = newIndex
                Log.d("BottomNavBar", "Updated Current Route: $route, Selected Index: $selectedIndex")
            }
        } ?: Log.d("BottomNavBar", "Ignoring null currentRoute")
    }



    NavigationBar(
        modifier = Modifier.height(80.dp),
        containerColor = Color(0xFF272A30),
        contentColor = Color(0xFFE2E1E6)
    ) {
        bottomNavItems.forEachIndexed { index, screen ->
            val isSelected = index == selectedIndex

            NavigationBarItem(
                icon = {
                    Icon(
                        painter = painterResource(id = screen.icon),
                        contentDescription = screen.title,
                        modifier = Modifier.size(24.dp),
                        tint = if (isSelected) Color(0xFF414659) else Color(0xFFE2E1E6)
                    )
                },
                label = {
                    Text(
                        text = screen.title,
                        color = if (isSelected) Color(0xE2E1E6FF) else Color(0xFFE2E1E6)
                    )
                },
                selected = isSelected,
                onClick = {
                    if (!isSelected) {
                        navController.navigate(screen.route) {
                            popUpTo(Screen.ChatList.route) { inclusive = false }
                            launchSingleTop = true
                            restoreState = true
                        }
                        Log.d("BottomNavBar", "Navigating to: ${screen.route}")
                    }
                }
            )
        }
    }
}
