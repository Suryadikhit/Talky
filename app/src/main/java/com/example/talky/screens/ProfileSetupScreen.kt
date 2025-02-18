package com.example.talky.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.talky.components.ProfileImagePicker
import com.example.talky.navigation.Screen
import com.example.talky.viewmodels.AuthViewModel

@Composable
fun ProfileSetupScreen(
    navController: NavController,
    viewModel: AuthViewModel,
    phoneNumber: String
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    var profileDrawableRes by remember { mutableStateOf<Int?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val displayName = when {
        firstName.isNotEmpty() && lastName.isNotEmpty() -> "$firstName $lastName"
        firstName.isNotEmpty() -> firstName
        else -> ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1D21))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Set up your profile",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFE5E3E8),
            modifier = Modifier
                .align(Alignment.Start)
                .padding(top = 40.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Profiles are visible to people you message, contacts, and groups.",
            fontSize = 14.sp,
            color = Color(0xFFBEBFC4),
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(40.dp))

        ProfileImagePicker(
            onImagePicked = { uri ->
                profileImageUri = uri
                profileDrawableRes = null
            },
            onDefaultImagePicked = { drawableRes ->
                profileDrawableRes = drawableRes
                profileImageUri = null
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (displayName.isNotEmpty()) {
            Text(
                text = displayName,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFE5E3E8),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        BasicTextField(
            value = firstName,
            onValueChange = { firstName = it },
            textStyle = TextStyle(fontSize = 16.sp, color = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = if (firstName.isEmpty() && errorMessage != null) Color.Red else Color.Gray,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(12.dp),
            singleLine = true,
            cursorBrush = SolidColor(Color.White),
            decorationBox = { innerTextField ->
                Column {
                    Box(Modifier.fillMaxWidth()) {
                        if (firstName.isEmpty()) {
                            Text("Enter First Name", color = Color.Gray)
                        }
                        innerTextField()
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        BasicTextField(
            value = lastName,
            onValueChange = { lastName = it },
            textStyle = TextStyle(fontSize = 16.sp, color = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                .padding(12.dp),
            singleLine = true,
            cursorBrush = SolidColor(Color.White),
            decorationBox = { innerTextField ->
                Column {
                    Box(Modifier.fillMaxWidth()) {
                        if (lastName.isEmpty()) {
                            Text("Enter Last Name", color = Color.Gray)
                        }
                        innerTextField()
                    }
                }
            }
        )


        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                errorMessage = null
                val fullName = "$firstName $lastName".trim()

                if (fullName.isEmpty()) {
                    errorMessage = "First name is required!"
                    return@Button
                }

                isSaving = true
                viewModel.saveUserProfile(
                    fullName,
                    phoneNumber,
                    profileImageUri,
                    profileDrawableRes
                ) { success ->
                    isSaving = false
                    if (success) {
                        navController.navigate(Screen.Main.route)
                    } else {
                        errorMessage = "Failed to save profile. Please try again."
                    }
                }
            },
            enabled = !isSaving,
            colors = ButtonDefaults.buttonColors(Color(0xFF464C5C)),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(
                text = if (isSaving) "Saving..." else "Continue",
                color = Color(0xFFDCE1FC) // ✅ Set text color correctly
            )

        }
    }
}
