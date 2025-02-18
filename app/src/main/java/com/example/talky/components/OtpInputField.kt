package com.example.talky.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun OtpInputField(otp: String, onOtpChange: (String) -> Unit) {
    val otpLength = 6

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    // ✅ Request focus when OTP screen is loaded
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    // ✅ State to control cursor blinking
    var cursorVisible by remember { mutableStateOf(true) }

    // ✅ Infinite blinking effect using LaunchedEffect
    LaunchedEffect(cursorVisible) {
        while (true) {
            delay(500)
            cursorVisible = !cursorVisible
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            repeat(otpLength) { index ->
                val isFocused = otp.length == index // ✅ Highlights the current box
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .border(
                            2.dp,
                            if (isFocused) Color(0xFFB6C5FA) else Color.Gray,
                            RoundedCornerShape(8.dp)
                        )
                        .background(Color(0xFF303133)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = otp.getOrNull(index)?.toString()
                            ?: if (isFocused && cursorVisible) "|" else "", // ✅ Cursor blinks inside focused box
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFBEBFC4)
                    )
                }
            }
        }

        // ✅ Hidden TextField for handling user input
        BasicTextField(
            value = otp,
            onValueChange = { newOtp ->
                if (newOtp.length <= otpLength && newOtp.all { it.isDigit() }) {
                    onOtpChange(newOtp)
                }
            },
            textStyle = androidx.compose.ui.text.TextStyle(color = Color.Transparent), // ✅ Hides unwanted blinking dot
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            modifier = Modifier
                .focusRequester(focusRequester)
                .fillMaxWidth()
                .height(1.dp) // ✅ Keeps it functional but invisible
                .background(Color.Transparent),
            cursorBrush = SolidColor(Color.White),
        )

    }
}
