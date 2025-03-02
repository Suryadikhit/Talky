package com.example.talky.screens

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.talky.R
import com.example.talky.viewmodels.AuthViewModel

@Composable
fun LoginScreen(navController: NavController, viewModel: AuthViewModel) {
    val context = LocalContext.current
    val activity = context as? Activity
    val keyboardController = LocalSoftwareKeyboardController.current
    val otpState by viewModel.otpState.collectAsState()

    var phoneNumber by rememberSaveable { mutableStateOf("") }
    var selectedCountry by rememberSaveable { mutableStateOf("India") }
    var countryCode by rememberSaveable { mutableStateOf("+91") }
    var selectedFlag by rememberSaveable { mutableIntStateOf(R.drawable.flag) }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    val countryList = listOf(
        Triple("United States", "+1", R.drawable.us),
        Triple("India", "+91", R.drawable.flag),
        Triple("Canada", "+1", R.drawable.canada)
    )

    // ✅ Fixed: Navigate with both verificationId & phoneNumber
    LaunchedEffect(otpState.verificationId) {
        otpState.verificationId?.let { verificationId ->
            navController.navigate("otp_screen/$verificationId/${phoneNumber}") {
                launchSingleTop = true
            }
        }
    }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }
    LaunchedEffect(otpState.otpError) {
        otpState.otpError?.let { errorMessage ->
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1D21))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Phone Number",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFE5E3E8),
            modifier = Modifier
                .align(Alignment.Start)
                .padding(top = 40.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "You will receive a verification code. Carrier rates may apply.",
            fontSize = 14.sp,
            color = Color(0xFFBEBFC4),
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Country Picker
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF303133), shape = RoundedCornerShape(8.dp))
                .clickable { isDropdownExpanded = true }
                .padding(12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = selectedFlag),
                    contentDescription = "Flag",
                    modifier = Modifier.size(24.dp),
                    tint = Color.Unspecified
                )
                Spacer(modifier = Modifier.width(16.dp))

                Text(text = selectedCountry, fontSize = 16.sp, color = Color(0xFFBEBFC4))

                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    painter = painterResource(id = R.drawable.dropdown),
                    contentDescription = "Dropdown",
                    modifier = Modifier.size(24.dp),
                    tint = Color.Unspecified
                )
            }

            DropdownMenu(
                expanded = isDropdownExpanded,
                onDismissRequest = { isDropdownExpanded = false }
            ) {
                countryList.forEach { (name, code, icon) ->
                    DropdownMenuItem(
                        text = { Text(name) },
                        onClick = {
                            selectedCountry = name
                            countryCode = code
                            selectedFlag = icon
                            isDropdownExpanded = false // ✅ Fixed: Close menu on selection
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Phone Number Input
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(65.dp)
                .background(Color(0xFF303133), shape = RoundedCornerShape(8.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .background(Color.White, shape = RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(text = countryCode, fontSize = 18.sp, color = Color(0xFFBEBFC4))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF303133), shape = RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                BasicTextField(
                    value = phoneNumber,
                    onValueChange = { input ->
                        phoneNumber = input.filter { it.isDigit() } // ✅ Allow only digits
                    },
                    textStyle = TextStyle(fontSize = 18.sp, color = Color(0xFFBEBFC4)),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester), // ✅ Fill full width
                    cursorBrush = SolidColor(Color.White), // ✅ Change cursor color
                    decorationBox = { innerTextField ->
                        if (phoneNumber.isEmpty()) {
                            Text("Enter your number", color = Color.Gray)
                        }
                        innerTextField()
                    }
                )

            }

        }

        Spacer(modifier = Modifier.height(20.dp))

        // ✅ Fixed: Show loading while verifying
        Button(
            onClick = {
                val fullPhoneNumber = "$countryCode$phoneNumber"
                viewModel.sendOtp(fullPhoneNumber, activity, navController)
            },
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(Color(0xFF464C5C)),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = phoneNumber.length in 6..15 && !otpState.isSendingOtp // Disable when loading
        ) {
            if (otpState.isSendingOtp) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp) // Slightly smaller loader
                    )
                    Spacer(modifier = Modifier.width(8.dp)) // Space between loader & text
                    Text("Sending Otp...", fontSize = 18.sp, color = Color(0xFFE2E1E6))
                }
            } else {
                Text("Next", fontSize = 18.sp, color = Color(0xFFDCE1FC))
            }
        }

    }
}
