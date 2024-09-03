package com.farzin.videocallapp.navigation

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.farzin.videocallapp.ui.screens.home_screen.HomeScreen
import com.farzin.videocallapp.ui.screens.permission_screen.PermissionScreen
import com.farzin.videocallapp.ui.screens.video_call_screen.VideoCallScreen

@Composable
fun NavGraph(navController: NavHostController) {

    val context = LocalContext.current
    val hasCameraPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    NavHost(
        navController = navController,
        startDestination = if (hasCameraPermission) Screens.Home.route else Screens.Permission.route
    ) {

        composable(route = Screens.Permission.route) {
            PermissionScreen(navController = navController)
        }

        composable(route = Screens.Home.route) {
            HomeScreen(navController = navController)
        }

        composable(route = Screens.VideoCall.route) {
            VideoCallScreen(navController = navController)
        }


    }
}