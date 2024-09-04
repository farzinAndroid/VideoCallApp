package com.farzin.videocallapp.ui.screens.video_call_screen

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController

@Composable
fun VideoCallScreen(
    navController: NavController,
    roomId:String
) {

    Text(text = "video call : $roomId")

}