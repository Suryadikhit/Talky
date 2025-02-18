package com.example.talky.screens


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.talky.R

@Composable
fun IntroScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1D21))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.intro), // Add intro_image.png in res/drawable
            contentDescription = "Intro Image",
            modifier = Modifier.size(450.dp)
        )



        Text(
            text = "Take privacy with you.\n" +
                    "Be yourself in every\n" +
                    "message.",
            fontSize = 27.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFE2E1E6),
            textAlign = TextAlign.Center
        )



        Spacer(modifier = Modifier.height(60.dp))

        Button(
            onClick = { navController.navigate("login_screen") },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(Color(0xFF464C5C)),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(text = "Continue", fontSize = 18.sp, color = Color(0xFFDCE1FC))
        }

        Spacer(modifier = Modifier.height(30.dp))

        Text(
            text = "By continuing, you agree to our Terms & Conditions",
            fontSize = 12.sp,
            color = Color(0xFFBEBFC4),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
