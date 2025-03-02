package com.example.talky.components



import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import com.example.talky.R
import com.example.talky.viewmodels.Message
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatBubble(message: Message, isCurrentUser: Boolean) {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val formattedTime = remember(message.timestamp) {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        sdf.format(Date(message.timestamp))
    }


    val isLongMessage = message.text.length > 30  // Adjust threshold as needed

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = if (isCurrentUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (isCurrentUser) Color(0xFF1B75FF) else Color(0xFF272A30),
            modifier = Modifier
                .widthIn(min = 50.dp, max = 0.75f * screenWidth) // Dynamic width
                .wrapContentWidth()
        ) {
            if (isLongMessage) {
                // Long message: Text in one row, time/status in a separate row below
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = message.text,
                        color = if (isCurrentUser) Color.White else Color(0xFFE2E1E6),
                        fontSize = 16.sp,
                        modifier = Modifier.wrapContentWidth()
                    )

                    Spacer(modifier = Modifier.height(5.dp)) // Space between text & timestamp

                    Row(
                        modifier = Modifier.align(Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formattedTime,
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )

                        if (isCurrentUser) {
                            Spacer(modifier = Modifier.width(4.dp))

                            val (statusIcon, statusColor) = when (message.status) {
                                "seen" -> R.drawable.doubletick to Color.Blue
                                "delivered" -> R.drawable.doubletick to Color.Gray
                                "sent" -> R.drawable.singletick to Color.White
                                else -> R.drawable.singletick to Color.White
                            }

                            Icon(
                                painter = painterResource(id = statusIcon),
                                contentDescription = "Message status",
                                tint = statusColor,
                                modifier = Modifier.size(16.dp)
                            )

                        }
                    }
                }
            } else {
                // Short message: Everything in one row
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = message.text,
                        color = if (isCurrentUser) Color.White else Color(0xFFE2E1E6),
                        fontSize = 16.sp,
                        modifier = Modifier.wrapContentWidth()
                    )

                    Spacer(modifier = Modifier.width(5.dp)) // Space between text & time

                    Text(
                        text = formattedTime,
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )

                    if (isCurrentUser) {
                        Spacer(modifier = Modifier.width(4.dp))
                        val (statusIcon, statusColor) = when (message.status) {
                            "seen" -> R.drawable.doubletick to Color.Blue
                            "delivered" -> R.drawable.doubletick to Color.Gray
                            "sent" -> R.drawable.singletick to Color.White
                            else -> R.drawable.singletick to Color.White
                        }

                        Icon(
                            painter = painterResource(id = statusIcon),
                            contentDescription = "Message status",
                            tint = statusColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
