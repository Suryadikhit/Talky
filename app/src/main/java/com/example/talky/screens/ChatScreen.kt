package com.example.talky.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.talky.components.ChatBubble
import com.example.talky.components.MessageInputField
import com.example.talky.viewmodels.ChatViewModel
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    otherUserId: String,
    otherUserName: String,
    profileImageUrl: String,
    onCallClick: () -> Unit,
    onVideoCallClick: () -> Unit,
    onEmojiClick: () -> Unit,
    onCameraClick: () -> Unit,
    onMicClick: () -> Unit,
    chatViewModel: ChatViewModel
) {
    val currentUserId = chatViewModel.auth.currentUser?.uid ?: return
    var messageText by remember { mutableStateOf("") }
    var chatId by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()

    // ✅ Observe "isTyping" from Firestore
    val isTyping by remember(chatId) {
        chatId?.let { chatViewModel.getTypingStatus(it, otherUserId) } ?: MutableStateFlow(false)
    }.collectAsState()

    val isScrolled by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
    }
    val topBarColor = if (isScrolled) Color(0xFF272A30) else Color(0xFF1C1D21)

    // ✅ Optimize System Bar Color Update
    val systemUiController = rememberSystemUiController()
    SideEffect {
        systemUiController.setStatusBarColor(topBarColor)
    }

    Log.d("ChatScreen", "Opening chat with: $otherUserName ($otherUserId)")

    // ✅ Fetch or Create Chat Room
    LaunchedEffect(otherUserId) {
        chatViewModel.createOrGetChatRoom(otherUserId) { fetchedChatId ->
            chatId = fetchedChatId
            if (fetchedChatId.isNotEmpty()) {
                chatViewModel.listenForMessages(fetchedChatId)
                chatViewModel.markMessagesAsSeen(fetchedChatId)
            }
        }
    }

    // ✅ Observe Messages
    val messages by chatId?.let { chatViewModel.getMessages(it).collectAsState(initial = emptyList()) } ?: remember { mutableStateOf(emptyList()) }

    // ✅ Auto Scroll on New Message (Avoid Flickering)
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            delay(100)  // Prevent UI flickering
            listState.scrollToItem(messages.lastIndex.coerceAtLeast(0))
            chatId?.let { chatViewModel.markMessagesAsSeen(it) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1C1D21))
    ) {
        // ✅ Top Bar
        TopAppBar(
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = profileImageUrl,
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color.Gray)
                    )

                    Column(
                        modifier = Modifier.padding(start = 14.dp)
                    ) {
                        Text(
                            text = otherUserName,
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp),
                            color = Color(0xFFE6E1E8)
                        )

                        if (isTyping) {
                            Text(
                                text = "Typing...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFE6E1E8)
                            )
                        }
                    }
                }
            },
            actions = {
                IconButton(onClick = onCallClick) {
                    Icon(
                        painter = painterResource(id = com.example.talky.R.drawable.videocall),
                        contentDescription = "Video Call",
                        tint = Color(0xFFE6E1E8),
                        modifier = Modifier.size(28.dp)
                    )
                }
                IconButton(onClick = onVideoCallClick) {
                    Icon(
                        painter = painterResource(id = com.example.talky.R.drawable.call),
                        contentDescription = "Call",
                        tint = Color(0xFFE6E1E8),
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = topBarColor,  // Dynamic Color Change
                titleContentColor = Color(0xFFE6E1E8)
            )
        )

        // ✅ Chat Messages
        LazyColumn(
            modifier = Modifier.weight(1f),
            state = listState
        ) {
            items(messages) { message ->
                ChatBubble(message, isCurrentUser = message.senderId == currentUserId)
            }
        }

        // ✅ Message Input Field
        MessageInputField(
            messageText = messageText,
            onMessageChange = { it ->
                messageText = it
                chatId?.let { chatViewModel.handleTyping(it) }
            },
            onSendClick = {
                chatId?.let {
                    chatViewModel.sendMessage(it, messageText, receiverId = otherUserId)
                }
                messageText = ""
            },
            onEmojiClick = onEmojiClick,
            onCameraClick = onCameraClick,
            onMicClick = onMicClick
        )
    }
}
