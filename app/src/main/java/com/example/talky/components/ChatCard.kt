package com.example.talky.components



import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.talky.R


data class ChatItem(
    val name: String,
    val lastMessage: String,
    val timestamp: String
)

@Composable
fun ChatCard(chat: ChatItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.man),
            contentDescription = "Profile Image",
            modifier = Modifier
                .size(55.dp)
                .clip(CircleShape)
                .border(1.dp, Color.Gray, CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier
                .weight(1f).padding(bottom = 15.dp),
            verticalArrangement = Arrangement.Top, // Aligns text to top
            horizontalAlignment = Alignment.Start // Aligns text to start
        ) {
            Text(text = chat.name, fontSize = 18.sp, color = Color(0xFFE2E1E6))
            Text(text = chat.lastMessage, fontSize = 14.sp, color = Color(0xFFBEBFC4))
        }

        Text(text = chat.timestamp, fontSize = 12.sp, color = Color(0xFFBEBFC4))

    }
}
