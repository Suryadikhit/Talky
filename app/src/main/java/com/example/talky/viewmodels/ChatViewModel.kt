package com.example.talky.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class Message(
    val senderId: String = "",
    val receiverId: String = "",
    val text: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "sent"
)

data class Chat(
    val chatId: String = "",
    val participants: List<String> = emptyList(),
    val lastMessage: String? = null,
    val lastMessageTime: Long? = 0L,
    val otherUserName: String? = null,
    val unreadCount: Int = 0
)

@HiltViewModel
class ChatViewModel @Inject constructor() : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()

    private val _messagesMap = mutableMapOf<String, MutableStateFlow<List<Message>>>()
    private val messageListeners = mutableSetOf<String>()

    fun getMessages(chatId: String): StateFlow<List<Message>> {
        return _messagesMap.getOrPut(chatId) { MutableStateFlow(emptyList()) }
    }

    private val _chatList = MutableStateFlow<List<Chat>>(emptyList())
    val chatList: StateFlow<List<Chat>> = _chatList

    init {
        listenForUserChats()
    }

    private fun listenForUserChats() {
        val currentUserId = auth.currentUser?.uid ?: return

        db.collection("chats")
            .whereArrayContains("participants", currentUserId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("ChatViewModel", "Error fetching chats: ${e.message}")
                    return@addSnapshotListener
                }

                val chatList = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Chat::class.java)?.copy(chatId = doc.id)
                } ?: emptyList()

                _chatList.value = chatList
            }
    }

    fun createOrGetChatRoom(otherUserId: String, onChatCreated: (String) -> Unit) {
        val currentUserId = auth.currentUser?.uid ?: return
        val participants = listOf(currentUserId, otherUserId)

        db.collection("chats")
            .whereArrayContains("participants", currentUserId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val existingChat = querySnapshot.documents.firstOrNull { doc ->
                    val chatParticipants = doc.get("participants") as? List<*>
                    chatParticipants?.contains(otherUserId) == true
                }

                if (existingChat != null) {
                    onChatCreated(existingChat.id)
                } else {
                    val newChatRef = db.collection("chats").document()
                    val chat = Chat(newChatRef.id, participants)

                    newChatRef.set(chat).addOnSuccessListener {
                        onChatCreated(newChatRef.id)
                    }
                }
            }
    }



    private val messageListenerRegistrations = mutableMapOf<String, ListenerRegistration>()

    fun listenForMessages(chatId: String) {
        if (messageListeners.contains(chatId)) return
        messageListeners.add(chatId)

        val chatMessagesFlow = _messagesMap.getOrPut(chatId) { MutableStateFlow(emptyList()) }

        val listener = db.collection("chats").document(chatId).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("ChatViewModel", "Error fetching messages: ${e.message}")
                    return@addSnapshotListener
                }

                val messagesList =
                    snapshot?.documents?.mapNotNull { it.toObject(Message::class.java) }
                        ?: emptyList()
                chatMessagesFlow.value = messagesList

                val recipientId =
                    messagesList.firstOrNull { it.receiverId != auth.currentUser?.uid }?.receiverId
                recipientId?.let { listenForUserOnlineStatus(it) }
            }

        messageListenerRegistrations[chatId] = listener
    }


    fun sendMessage(chatId: String, messageText: String, receiverId: String) {
        val senderId = auth.currentUser?.uid ?: return
        val message = Message(
            senderId = senderId,
            receiverId = receiverId,
            text = messageText,
            timestamp = System.currentTimeMillis(),
            status = "sent"
        )

        val messageRef = db.collection("chats").document(chatId).collection("messages").document()

        messageRef.set(message).addOnSuccessListener {
            Log.d("ChatViewModel", "Message sent: $messageText")

            // Fetch latest online status before updating delivery
            db.collection("users").document(receiverId).get().addOnSuccessListener { snapshot ->
                val isOnline = snapshot.getBoolean("online") ?: false
                userOnlineCache[receiverId] = isOnline // Update cache with latest value

                if (isOnline) {
                    messageRef.update("status", "delivered").addOnSuccessListener {
                        Log.d("ChatViewModel", "Message delivered: $messageText")
                    }
                }
            }
            updateLastMessage(chatId, messageText)
        }.addOnFailureListener { e ->
            Log.e("ChatViewModel", "Failed to send message: ${e.message}")
        }
    }


    private val userOnlineCache = mutableMapOf<String, Boolean>()

    private fun listenForUserOnlineStatus(userId: String?) {
        if (userId.isNullOrBlank()) {
            Log.e("ChatViewModel", "Invalid userId: $userId")
            return
        }

        db.collection("users").document(userId)
            .addSnapshotListener { snapshot, _ ->
                val isOnline = snapshot?.getBoolean("online") ?: false
                userOnlineCache[userId] = isOnline
                if (isOnline) updateUndeliveredMessages(userId)
            }
    }

    fun getUserProfileImage(userId: String): MutableStateFlow<String> {
        val profileImageFlow = MutableStateFlow("")
        db.collection("users").document(userId).addSnapshotListener { snapshot, _ ->
            if (snapshot != null && snapshot.exists()) {
                val imageUrl = snapshot.getString("profileImageUrl") ?: ""
                profileImageFlow.value = imageUrl
            }
        }
        return profileImageFlow
    }

    fun markMessagesAsSeen(chatId: String) {
        val userId = auth.currentUser?.uid ?: return
        val messagesRef = db.collection("chats").document(chatId).collection("messages")

        messagesRef
            .whereEqualTo("receiverId", userId)
            .whereIn("status", listOf("sent", "delivered"))
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) return@addOnSuccessListener

                db.runBatch { batch ->
                    snapshot.documents.forEach { doc ->
                        batch.update(doc.reference, "status", "seen")
                    }
                }.addOnSuccessListener {
                    Log.d("ChatViewModel", "Messages marked as seen in chat: $chatId")
                }
            }.addOnFailureListener { e ->
                Log.e("ChatViewModel", "Failed to mark messages as seen: ${e.message}")
            }
    }

    private val typingStatusMap = mutableMapOf<String, MutableStateFlow<Boolean>>()

    fun getTypingStatus(chatId: String, otherUserId: String): StateFlow<Boolean> {
        return typingStatusMap.getOrPut("$chatId-$otherUserId") {
            MutableStateFlow(false).apply {
                val typingRef = FirebaseDatabase.getInstance()
                    .getReference("typingStatus")
                    .child(chatId)
                    .child(otherUserId)

                typingRef.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        value = snapshot.getValue(Boolean::class.java) ?: false
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("ChatViewModel", "Error fetching typing status", error.toException())
                    }
                })
            }
        }.asStateFlow()
    }


    private var typingJob: Job? = null



    fun handleTyping(chatId: String) {
        typingJob?.cancel()
        updateTypingStatus(chatId, true)  // Set typing to true

        typingJob = viewModelScope.launch {
            delay(2000)  // Reset after 5 seconds of inactivity
            updateTypingStatus(chatId, false)
        }
    }


    private fun updateTypingStatus(chatId: String, isTyping: Boolean) {
        val currentUserId = auth.currentUser?.uid ?: return
        val typingRef = FirebaseDatabase.getInstance()
            .getReference("typingStatus")
            .child(chatId)
            .child(currentUserId)

        typingRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.getValue(Boolean::class.java) != isTyping) {
                if (isTyping) {
                    typingRef.setValue(true)
                    typingRef.onDisconnect().removeValue()
                } else {
                    typingRef.removeValue()
                }
            }
        }
    }



    private fun updateUndeliveredMessages(receiverId: String) {
        db.collection("chats")
            .whereArrayContains("participants", receiverId)
            .get()
            .addOnSuccessListener { snapshot ->
                snapshot.documents.forEach { chatDoc ->
                    val chatId = chatDoc.id
                    db.collection("chats").document(chatId).collection("messages")
                        .whereEqualTo("receiverId", receiverId)
                        .whereEqualTo("status", "sent")
                        .get()
                        .addOnSuccessListener { messages ->
                            val batch = db.batch()
                            for (doc in messages.documents) {
                                batch.update(doc.reference, "status", "delivered")
                            }
                            batch.commit().addOnSuccessListener {
                                Log.d(
                                    "ChatViewModel",
                                    "Undelivered messages updated to delivered for user: $receiverId"
                                )
                            }
                        }
                }
            }.addOnFailureListener { e ->
                Log.e("ChatViewModel", "Failed to update undelivered messages: ${e.message}")
            }
    }

    private fun updateLastMessage(chatId: String, messageText: String) {
        val timestamp = System.currentTimeMillis()
        val chatRef = db.collection("chats").document(chatId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(chatRef)
            val lastMessageTime = snapshot.getLong("lastMessageTime") ?: 0

            if (timestamp > lastMessageTime) {
                transaction.update(
                    chatRef,
                    "lastMessage",
                    messageText,
                    "lastMessageTime",
                    timestamp
                )
            }
        }
    }

    private val userNameCache = mutableMapOf<String, WeakReference<MutableStateFlow<String>>>()
    private val lastMessageCache = mutableMapOf<String, WeakReference<MutableStateFlow<String>>>()

    fun getUserNameLive(userId: String): StateFlow<String> {
        userNameCache[userId]?.get()?.let { return it }  // ✅ Return cached value if available

        val stateFlow = MutableStateFlow("")
        userNameCache[userId] = WeakReference(stateFlow)  // ✅ Prevent memory leaks

        db.collection("users").document(userId).get()
            .addOnSuccessListener { snapshot ->
                val newName = snapshot.getString("name") ?: "Unknown"
                stateFlow.value = newName
            }

        db.collection("users").document(userId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    val newName = snapshot.getString("name") ?: ""
                    if (stateFlow.value != newName) {
                        stateFlow.value = newName
                    }
                }
            }

        return stateFlow
    }

    fun getLastMessageLive(chatId: String): StateFlow<String> {
        lastMessageCache[chatId]?.get()?.let { return it }  // ✅ Return cached value if available

        val stateFlow = MutableStateFlow("")
        lastMessageCache[chatId] = WeakReference(stateFlow)  // ✅ Prevent memory leaks

        db.collection("chats").document(chatId).collection("messages")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    val messageDoc = snapshot.documents[0]
                    val newMessage = messageDoc.getString("text") ?: ""
                    stateFlow.value = newMessage
                }
            }

        db.collection("chats").document(chatId).collection("messages")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && !snapshot.isEmpty) {
                    val messageDoc = snapshot.documents[0]
                    val newMessage = messageDoc.getString("text") ?: ""
                    if (stateFlow.value != newMessage) {
                        stateFlow.value = newMessage
                    }
                }
            }

        return stateFlow
    }
    fun formatTimestamp(timestamp: Long?): String {
        if (timestamp == null || timestamp == 0L) return ""
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}