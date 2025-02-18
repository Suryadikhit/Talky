package com.example.talky.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.talky.viewmodels.AuthViewModel

@Composable
fun ChatsScreen(viewModel: AuthViewModel) {
    var profileImageUrl by remember { mutableStateOf("default_url") }

    // Fetch the profile image URL asynchronously
    LaunchedEffect(Unit) {
        viewModel.getUserProfileImageUrl { url ->
            profileImageUrl = url
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Row with Profile Picture, Title, Search Icon, and More Options
        TopRow(profileImageUrl)
        // Add other components if needed, e.g., Chat List
    }
}

@Composable
fun TopRow(profileImageUrl: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Display Profile Image without click and dialog
        Box(
            modifier = Modifier
                .size(40.dp) // Set the size of the image
                .clip(CircleShape) // Clip the image to a circle
        ) {
            Image(
                painter = rememberAsyncImagePainter(profileImageUrl),
                contentDescription = "Profile Picture",
                contentScale = ContentScale.Crop, // Crop the image to fit the bounds
                modifier = Modifier.fillMaxSize() // Ensure it fills the box completely
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // "Talky" Title
        Text(
            text = "Talky",
            fontSize = 22.sp,
            modifier = Modifier.weight(1f) // Pushes icons to the right
        )

        // Search Icon (Clickable)
        IconButton(onClick = { /* Handle Search click */ }) {
            Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
        }

        // Triple-dot Menu Icon (Clickable)
        IconButton(onClick = { /* Handle More Options click */ }) {
            Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More Options")
        }
    }
}
