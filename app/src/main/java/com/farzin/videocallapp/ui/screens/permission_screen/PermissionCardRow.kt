package com.farzin.videocallapp.ui.screens.permission_screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.farzin.videocallapp.R
import com.farzin.videocallapp.ui.theme.Teal700

@Composable
fun PermissionCardRow(
    modifier: Modifier = Modifier,
    icon:Painter,
    text:String
) {

    Row(
        modifier = modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Absolute.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {

        Image(
            painter = icon,
            contentDescription = "",
            modifier = Modifier
                .size(16.dp),
        )

        Spacer(Modifier.width(8.dp))

        Text(
            text = text,
            fontSize = 14.sp,
            color = Teal700
        )

    }

}