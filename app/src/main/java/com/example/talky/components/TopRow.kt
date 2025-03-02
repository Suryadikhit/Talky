package com.example.talky.components



import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.talky.R

@Composable
fun TopRow(
    username: String,
    onSearchClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.chat),
            contentDescription = "App Icon",
            modifier = Modifier.size(30.dp),
            contentScale = ContentScale.Crop
        )

        Text(
            text = username,
            fontSize = 20.sp,
            color =  Color(0xFFE1E1E6),
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        )

        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Search",
            tint = Color(0xFFBEBFC4), // Corrected color usage
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
