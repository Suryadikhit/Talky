package com.example.talky.components

import android.content.Context
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.request.CachePolicy
import coil.request.ImageRequest

@Composable
fun TopRow(
    username: String,
    profilePicUrl: String?,
    onSearchClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProfileImage(username = username, imageUrl = profilePicUrl)

        Text(
            text = username.ifEmpty { "User" },
            fontSize = 20.sp,
            color = Color(0xFFE1E1E6),
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        )

        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Search",
            tint = Color(0xFFBEBFC4),
            modifier = Modifier
                .size(28.dp)
                .clickable { onSearchClick() }
        )

        Spacer(modifier = Modifier.width(10.dp))

        Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = "Menu",
            tint = Color(0xFFBEBFC4),
            modifier = Modifier
                .size(28.dp)
                .clickable { onMenuClick() }
        )
    }
}

@Composable
fun ProfileImage(username: String, imageUrl: String?) {
    val context = LocalContext.current

    val validUrl = imageUrl?.let { "$it?timestamp=1" }

    // If imageUrl is null, load default avatar
    val data = validUrl

    val imageLoader = remember { createImageLoader(context) }

    val imageRequest = ImageRequest.Builder(context)
        .data(data)
        .diskCacheKey(imageUrl) // Ensure same image isn't reloaded
        .memoryCacheKey(imageUrl)
        .diskCachePolicy(CachePolicy.ENABLED) // Enable disk caching
        .listener(
            onStart = { Log.d("ProfileImage", "Loading image for $username from URL: $validUrl") },
            onSuccess = { _, _ -> Log.d("ProfileImage", "Loaded from cache or network: $validUrl") },
            onError = { _, _ -> Log.e("ProfileImage", "Failed to load image for $username") }
        )
        .build()

    Image(
        painter = rememberAsyncImagePainter(
            model = imageRequest,
            imageLoader = imageLoader
        ),
        contentDescription = "Profile Image",
        modifier = Modifier
            .size(50.dp)
            .clip(CircleShape)
            .background(Color.Gray.copy(alpha = 0.3f)),
        contentScale = ContentScale.Crop
    )
}



fun createImageLoader(context: Context): ImageLoader {
    return ImageLoader.Builder(context)
        .respectCacheHeaders(false) // Ignore cache headers, use our URL versioning
        .diskCachePolicy(CachePolicy.ENABLED)
        .memoryCachePolicy(CachePolicy.ENABLED)
        .build()
}
