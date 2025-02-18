package com.example.talky.components

import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import com.example.talky.R
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import java.io.File

@Composable
fun ProfileImagePicker(
    onImagePicked: (Uri?) -> Unit,
    onDefaultImagePicked: (Int?) -> Unit
) {
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedDrawableRes by remember { mutableStateOf<Int?>(null) }
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .size(100.dp)
            .clip(CircleShape)
            .background(Color(0xFFF7D7EC).copy(alpha = 0.3f))
            .clickable { showBottomSheet = true },
        contentAlignment = Alignment.Center
    ) {
        if (selectedDrawableRes != null) {
            Image(
                painter = painterResource(id = selectedDrawableRes!!),
                contentDescription = "Profile Picture",
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Image(
                painter = selectedImageUri?.let { uri ->
                    rememberAsyncImagePainter(
                        ImageRequest.Builder(context)
                            .data(uri)
                            .transformations(CircleCropTransformation())
                            .build()
                    )
                } ?: painterResource(id = R.drawable.user),
                contentDescription = "Profile Picture",
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    if (showBottomSheet) {
        ProfileImageBottomSheet(
            onDismiss = { showBottomSheet = false },
            onImageSelected = { newImageUri, drawableRes ->
                if (newImageUri != null) {
                    selectedImageUri = newImageUri
                    selectedDrawableRes = null
                    onImagePicked(newImageUri)
                } else if (drawableRes != null) {
                    selectedDrawableRes = drawableRes
                    selectedImageUri = null
                    onDefaultImagePicked(drawableRes)
                }
                showBottomSheet = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ProfileImageBottomSheet(
    onDismiss: () -> Unit,
    onImageSelected: (Uri?, Int?) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val imageUri = remember { mutableStateOf<Uri?>(null) }

    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
    val galleryPermissionState = rememberPermissionState(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            android.Manifest.permission.READ_MEDIA_IMAGES
        else
            android.Manifest.permission.READ_EXTERNAL_STORAGE
    )

    val cameraLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                imageUri.value?.let { uri ->
                    onImageSelected(uri, null)
                    onDismiss()
                }
            }
        }

    val galleryLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                onImageSelected(it, null)
                onDismiss()
            }
        }

    fun handlePermissionAndLaunch(
        permissionState: com.google.accompanist.permissions.PermissionState,
        launchAction: () -> Unit
    ) {
        when {
            permissionState.status.isGranted -> launchAction()
            permissionState.status.shouldShowRationale -> {
                Toast.makeText(context, "Permission required to proceed", Toast.LENGTH_LONG).show()
            }

            else -> permissionState.launchPermissionRequest()
        }
    }

    LaunchedEffect(cameraPermissionState.status) {
        if (cameraPermissionState.status.isGranted && imageUri.value != null) {
            cameraLauncher.launch(imageUri.value!!)
        }
    }

    LaunchedEffect(galleryPermissionState.status) {
        if (galleryPermissionState.status.isGranted) {
            galleryLauncher.launch("image/*")
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.6f)
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Choose Profile Picture", style = MaterialTheme.typography.titleMedium)

            Spacer(modifier = Modifier.height(50.dp))

            val defaultImages = listOf(
                R.drawable.man,
                R.drawable.girl,
                R.drawable.man,
                R.drawable.girl,
                R.drawable.man,
                R.drawable.girl
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                for (i in defaultImages.chunked(3)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        i.forEach { resId ->
                            Image(
                                painter = painterResource(id = resId),
                                contentDescription = "Default Profile",
                                modifier = Modifier
                                    .size(70.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        onImageSelected(null, resId)
                                        onDismiss()
                                    }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        handlePermissionAndLaunch(galleryPermissionState) {
                            galleryLauncher.launch("image/*")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Choose from Gallery")
                }

                Button(
                    onClick = {
                        handlePermissionAndLaunch(cameraPermissionState) {
                            val file = File(context.cacheDir, "profile_image.jpg")
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.provider",
                                file
                            )
                            imageUri.value = uri
                            cameraLauncher.launch(uri)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Take a Photo")
                }
            }
        }
    }
}
