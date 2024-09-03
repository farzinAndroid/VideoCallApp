package com.farzin.videocallapp.navigation

sealed class Screens(val route: String) {

    data object Permission : Screens("permission_screen")
    data object Home : Screens("home_screen")
    data object VideoCall : Screens("video_call_screen")

}