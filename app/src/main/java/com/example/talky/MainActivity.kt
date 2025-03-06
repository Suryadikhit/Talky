package com.example.talky

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.talky.navigation.NavGraph
import com.example.talky.viewmodels.AuthPreferences
import com.example.talky.viewmodels.AuthViewModel
import com.example.talky.viewmodels.ChatViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var authPreferences: AuthPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        println("com.example.talky.viewmodels.AuthPreferences injected: ${authPreferences.isUserLoggedIn()}")
        setContent {
            val navController = rememberNavController()
            TalkyApp(navController)
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun TalkyApp(navController: NavHostController) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val chatViewModel: ChatViewModel = hiltViewModel()

    LaunchedEffect(authViewModel) {
        authViewModel.checkUserAuth(navController)
    }

    Scaffold {
        NavGraph(navController, authViewModel, chatViewModel)
    }
}
