package com.example.talky.screens

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.talky.components.OtpInputField
import com.example.talky.navigation.Screen
import com.example.talky.viewmodels.AuthViewModel

@Composable
fun OtpScreen(
    navController: NavController,
    verificationId: String?,
    phoneNumber: String,
    viewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val otpState by viewModel.otpState.collectAsState()

    LaunchedEffect(Unit) {
        if (verificationId != null && otpState.resendToken != null) {
            viewModel.setVerificationDetails(verificationId, otpState.resendToken!!)
        }
    }

    LaunchedEffect(otpState.loginSuccess) {
        if (otpState.loginSuccess) {
            navController.navigate("profile_setup_screen/$phoneNumber") {
                popUpTo(Screen.Login.route) { inclusive = true }
            }
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
            "Enter OTP",
            fontSize = 30.sp,
            color = Color(0xFFE5E3E8),
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(top = 40.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "We've sent a verification code to $phoneNumber",
            fontSize = 18.sp,
            modifier = Modifier.align(Alignment.Start),
            color = Color(0xFFBEBFC4),
        )

        Spacer(modifier = Modifier.height(40.dp))

        OtpInputField(
            otp = otpState.otpCode,
            onOtpChange = { viewModel.updateOtpCode(it) }
        )


        // ✅ Show error message if OTP verification fails
        if (!otpState.otpError.isNullOrEmpty()) {
            Text(
                text = otpState.otpError!!,
                color = Color.Red,
                fontSize = 14.sp,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(top = 8.dp)
            )
        }


        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = { viewModel.verifyOtp(activity, navController) },
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(Color(0xFF464C5C)),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = !otpState.isVerifying && otpState.otpCode.length == 6 // Ensure OTP length is 6 and not verifying
        ) {
            Text(
                when {
                    otpState.isSendingOtp -> "Sending OTP..."
                    otpState.isVerifying -> "Verifying..."
                    else -> "Verify"
                },
                fontSize = 18.sp,
                color = Color(0xFFDCE1FC)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

// Resend OTP logic
        if (otpState.canResend) {
            Text(
                "Resend OTP",
                color = Color(0xFFB6C5FA),
                fontSize = 16.sp,
                modifier = Modifier.clickable {
                    if (phoneNumber.isNotEmpty()) {
                        activity?.let {
                            viewModel.resendOtp(phoneNumber, it, navController)
                            viewModel.startTimer()
                        }
                    }
                }
            )
        } else {
            Text(
                "Resend OTP in ${otpState.timerSeconds} sec",
                color = Color(0xFF6E6F73),
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

// "Wrong number?" functionality
        Text(
            "Wrong number?",
            color = Color(0xFFB6C5FA),
            fontSize = 16.sp,
            modifier = Modifier.clickable {
                Log.d("OtpScreen", "Wrong number clicked! Resetting state & navigating to login.")
                viewModel.resetOtpState() // Reset OTP state
                navController.navigate(Screen.Login.route) {
                    popUpTo(0) { inclusive = true } // Clears entire backstack
                }
            }
        )
    }
}