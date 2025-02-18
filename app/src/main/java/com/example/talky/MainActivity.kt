package com.example.talky

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.talky.navigation.NavGraph
import com.example.talky.viewmodels.AuthPreferences
import com.example.talky.viewmodels.AuthViewModel

class MainActivity : ComponentActivity() {
    private lateinit var authViewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("MainActivity", "onCreate called")

        try {
            authViewModel = AuthViewModel(authPreferences = AuthPreferences(this))
            Log.d("MainActivity", "AuthViewModel initialized successfully")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error initializing AuthViewModel: ${e.message}")
            return
        }

        setContent {
            val navController = rememberNavController()
            Log.d("MainActivity", "NavController initialized")

            LaunchedEffect(Unit) {
                try {
                    Log.d("MainActivity", "LaunchedEffect triggered, checking user authentication")
                    authViewModel.checkUserAuth(navController)
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error during authentication check: ${e.message}")
                }
            }

            TalkyApp(navController, authViewModel)
        }
    }
}


@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun TalkyApp(navController: NavHostController, viewModel: AuthViewModel) {
    Log.d("TalkyApp", "TalkyApp Composable called")

    Scaffold {
        Log.d("TalkyApp", "Scaffold initialized")
        // Navigating using NavGraph
        NavGraph(navController, viewModel)
    }
}
