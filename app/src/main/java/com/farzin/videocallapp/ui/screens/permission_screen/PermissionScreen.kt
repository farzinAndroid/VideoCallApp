package com.farzin.videocallapp.ui.screens.permission_screen

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.farzin.videocallapp.R
import com.farzin.videocallapp.navigation.Screens
import com.farzin.videocallapp.ui.theme.Red700
import com.farzin.videocallapp.ui.theme.Teal700
import com.farzin.videocallapp.ui.theme.VideoCallAppTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionScreen(
    navController: NavController,
) {

    val context = LocalContext.current as Activity

    val permissions = rememberMultiplePermissionsState(listOf(Manifest.permission.CAMERA))

    if (permissions.allPermissionsGranted){
        navController.navigate(Screens.Home.route){
            popUpTo(0){
                inclusive = true
            }
            launchSingleTop = true
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {

        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(20.dp)
                .fillMaxSize()
        ) {
            Text(
                text = "Let's get you connected!",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = Teal700
            )

            Spacer(Modifier.height(16.dp))

            Text(
                text = "To start a video call we need access to you camera and microphone.",
                fontSize = 16.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(10.dp))

            Button(
                onClick = {
                    permissions.launchMultiplePermissionRequest()
                }, colors = ButtonDefaults.buttonColors(
                    containerColor = Teal700
                )
            ) {
                Text(text = "Grand permission now!")
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Why do we need this permission?",
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(16.dp))

            Card(
                modifier = Modifier
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    PermissionCardRow(
                        icon = painterResource(id = R.drawable.camera),
                        text = "Camera : To share your smiling face with others in video calls."
                    )

                    PermissionCardRow(
                        icon = painterResource(id = R.drawable.microphone),
                        text = "Microphone : So others can hear your voice loud and clear."
                    )
                }
            }

            Spacer(Modifier.height(16.dp))


            Text(
                text = "If you denied the permissions or want to enable them later no worries!",
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .apply {
                            data = Uri.fromParts("package", context.packageName, null)
                            addCategory(Intent.CATEGORY_DEFAULT)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_NO_HISTORY or
                                    Intent.FLAG_ACTIVITY_NO_HISTORY or
                                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                        }
                    )
                }, colors = ButtonDefaults.buttonColors(
                    containerColor = Red700
                )
            ) {
                Text(text = "Open app settings")
            }
        }

    }

}

@Preview(showBackground = true)
@Composable
private fun PermissionScreenPrev() {
    VideoCallAppTheme {
        PermissionScreen(rememberNavController())
    }
}