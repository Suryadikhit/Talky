package com.example.talky.screens

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.talky.components.BottomNavBar
import com.example.talky.components.ChatCard
import com.example.talky.components.ChatItem
import com.example.talky.components.TopRow
import com.example.talky.navigation.Screen
import com.example.talky.viewmodels.AuthViewModel
import com.example.talky.viewmodels.ChatViewModel

@Composable
fun ChatListScreen(
    authViewModel: AuthViewModel,
    chatViewModel: ChatViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("TalkyPrefs", Context.MODE_PRIVATE) }

    val username by produceState(initialValue = sharedPreferences.getString("username", "User") ?: "User") {
        authViewModel.getUserName { name ->
            if (!name.isNullOrEmpty() && name != value) {
                value = name
                sharedPreferences.edit().putString("username", name).apply()
            }
        }
    }

    val chatList by chatViewModel.chatList.collectAsState()

    // ✅ Cache for user names & last messages
    val userNameCache = remember { mutableMapOf<String, String>() }
    val lastMessageCache = remember { mutableMapOf<String, String>() }

    Scaffold(bottomBar = { BottomNavBar(navController) }) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1C1D21))
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopRow(username, onSearchClick = {}, onMenuClick = {})

                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(chatList) { chat ->
                        val currentUserId = authViewModel.getCurrentUserId()
                        val otherUserId = chat.participants.firstOrNull { it != currentUserId } ?: "Unknown"

                        // ✅ Fetch user name once & store it in cache
                        val otherUserName by produceState(initialValue = userNameCache[otherUserId] ?: "Loading...") {
                            if (!userNameCache.containsKey(otherUserId)) {
                                chatViewModel.getUserNameLive(otherUserId).collect { name ->
                                    userNameCache[otherUserId] = name
                                    value = name
                                }
                            }
                        }

                        // ✅ Fetch last message once & store it in cache
                        val lastMessage by produceState(initialValue = lastMessageCache[chat.chatId] ?: "No messages yet") {
                            if (!lastMessageCache.containsKey(chat.chatId)) {
                                chatViewModel.getLastMessageLive(chat.chatId).collect { msg ->
                                    lastMessageCache[chat.chatId] = msg
                                    value = msg
                                }
                            }
                        }

                        ChatCard(
                            chat = ChatItem(
                                name = otherUserName,
                                lastMessage = lastMessage,
                                timestamp = chatViewModel.formatTimestamp(chat.lastMessageTime)
                            ),
                            onClick = {
                                navController.navigate("chat_Screen/${Uri.encode(otherUserId)}/${Uri.encode(otherUserName)}")
                            }
                        )
                    }
                }
            }

            // Floating Action Button (FAB)
            Surface(
                modifier = Modifier
                    .clickable { navController.navigate(Screen.Newmessage.route) }
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .size(50.dp),
                shape = MaterialTheme.shapes.medium,
                shadowElevation = 4.dp,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Image(
                    painter = painterResource(id = com.example.talky.R.drawable.pencil),
                    contentDescription = "New Chat",
                    modifier = Modifier.padding(15.dp)
                )
            }
        }
    }
}
