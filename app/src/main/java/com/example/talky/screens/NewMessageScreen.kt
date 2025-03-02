package com.example.talky.screens

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.talky.viewmodels.AuthViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewMessageScreen(
    authViewModel: AuthViewModel,
    onBackClick: () -> Unit,
    onMenuClick: () -> Unit,
    onUserClick: (String, String) -> Unit
) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(false) }
    var contactList by remember { mutableStateOf<List<Pair<String, String?>>>(emptyList()) }
    val currentUserId = authViewModel.getCurrentUserId()

    val requestPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            hasPermission = isGranted
        }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            hasPermission = true
        } else {
            requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission && contactList.isEmpty()) {
            fetchAndStoreContacts(context, authViewModel) { fetchedContacts ->
                getUsersWithAccounts(fetchedContacts, currentUserId) { matchedContacts ->
                    contactList = matchedContacts
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize() .background(Color(0xFF1C1D21))) {
        TopAppBar(
            title = { Text(text = "New Message", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start) },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = onMenuClick) {
                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Menu")
                }
            }
        )

        if (hasPermission) {
            LazyColumn {
                items(contactList) { (name, userId) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .clickable { userId?.let { onUserClick(it, name) } },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(model = "https://example.com/default_profile_pic.png", contentDescription = "Profile Pic", modifier = Modifier.size(55.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(text = name, fontSize = 18.sp, color = Color(0xFFE2E1E6))
                        Spacer(modifier = Modifier.weight(1f))
                        Button(
                            onClick = { userId?.let { onUserClick(it, name) } },
                            modifier = Modifier
                                .padding(4.dp) // Reduce padding around the button
                                .size(width = 80.dp, height = 36.dp), // Adjust width and height
                            contentPadding = PaddingValues(0.dp) // Reduce internal padding
                        ) {
                            Text("Message", color = Color(0xFFE2E1E6), fontSize = 14.sp)
                        }

                    }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Permission required to access contacts", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

fun fetchAndStoreContacts(context: Context, authViewModel: AuthViewModel, callback: (List<String>) -> Unit) {
    val contactList = LinkedHashSet<String>() // Avoid duplicates
    val resolver: ContentResolver = context.contentResolver
    val cursor = resolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
        null,
        null,
        null
    )

    cursor?.use {
        while (it.moveToNext()) {
            val phoneNumber = it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
                .replace(Regex("[^0-9+]"), "") // Normalize phone number
            contactList.add(phoneNumber)
        }
    }

    val currentUserId = authViewModel.getCurrentUserId()
    if (currentUserId != null) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(currentUserId)
            .set(mapOf("contacts" to contactList.toList()), SetOptions.merge()) // Merge to avoid overwriting
            .addOnSuccessListener { Log.d("Firestore", "Contacts saved!") }
            .addOnFailureListener { Log.e("Firestore", "Failed to save contacts", it) }
    }

    callback(contactList.toList()) // Return fetched contacts
}

fun getUsersWithAccounts(contactNumbers: List<String>, currentUserId: String?, callback: (List<Pair<String, String>>) -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val registeredUsersRef = db.collection("users")
    val batchSize = 10
    val matchingUsers = mutableListOf<Pair<String, String>>()

    if (contactNumbers.isEmpty()) {
        callback(emptyList())
        return
    }

    val batchedQueries = contactNumbers.chunked(batchSize)

    batchedQueries.forEachIndexed { index, batch ->
        registeredUsersRef.whereIn("phoneNumber", batch)
            .get()
            .addOnSuccessListener { userDocs ->
                userDocs.forEach { doc ->
                    val name = doc.getString("name")
                    val userId = doc.id
                    if (userId != currentUserId) {
                        name?.let { matchingUsers.add(it to userId) }
                    }
                }
                if (index == batchedQueries.lastIndex) {
                    callback(matchingUsers)
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error fetching registered users", e)
                callback(emptyList())
            }
    }
}
