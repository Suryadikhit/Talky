package com.example.talky.components



import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.example.talky.R

@Composable
fun MessageInputField(
    messageText: String,
    onMessageChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onEmojiClick: () -> Unit,
    onCameraClick: () -> Unit,
    onMicClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .background(Color(0xFF272A30), shape = RoundedCornerShape(20.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onEmojiClick) {
            Icon(
                painter = painterResource(id = R.drawable.emoji),
                contentDescription = "Emoji",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }

        TextField(
            value = messageText,
            onValueChange = onMessageChange,
            placeholder = { Text("Type message", color = Color(0xFFE2E1E6)) },
            minLines = 1,
            maxLines = 3,
            modifier = Modifier
                .weight(1f)
                .background(Color.Transparent),
            textStyle = TextStyle(color = Color(0xFFE2E1E6)),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                cursorColor = Color.White,
                unfocusedIndicatorColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent
            )
        )

        if (messageText.isBlank()) {
            IconButton(onClick = onCameraClick) {
                Icon(
                    painter = painterResource(id = R.drawable.camera),
                    contentDescription = "Camera",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            IconButton(onClick = onMicClick) {
                Icon(
                    painter = painterResource(id = R.drawable.mic),
                    contentDescription = "Voice Message",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        } else {
            IconButton(onClick = onSendClick) {
                Icon(
                    painter = painterResource(id = R.drawable.send),
                    contentDescription = "Send",
                    tint = Color(0xFF1B75FF),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
