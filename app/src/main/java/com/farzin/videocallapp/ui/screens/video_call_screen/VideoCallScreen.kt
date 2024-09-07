package com.farzin.videocallapp.ui.screens.video_call_screen

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import org.webrtc.SurfaceViewRenderer
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoCallScreen(
    navController: NavController,
    roomId: String,
) {

    val context = LocalContext.current

    val fireStore = remember { FirebaseFirestore.getInstance() }

    fun checkRoomCapacityAndSetup(
        onNavigateBack: () -> Unit,
        onProceed: () -> Unit,
    ) {
        val roomRefID = fireStore.collection("Rooms").document(roomId)

        roomRefID.get()
            .addOnSuccessListener { document ->
                Timber.e("Fire store OK")

                if (document != null && document.exists()) {
                    val participantCount = (document["participantCount"] as? Long)?.toInt() ?: 0
                    if (participantCount >= 2) {
                        Toast.makeText(context, "Room is full", Toast.LENGTH_SHORT).show()
                        onNavigateBack()
                    } else {
                        roomRefID.update("participantCount", participantCount + 1)
                    }


                } else {
                    roomRefID.set(mapOf("participantCount" to 1))
                }

            }
            .addOnFailureListener {
                Timber.e("Fire store error")
                Toast.makeText(context, "Firebase failed", Toast.LENGTH_SHORT).show()
                onNavigateBack()
            }
    }


    LaunchedEffect(true) {
        checkRoomCapacityAndSetup(
            onNavigateBack = {
                navController.popBackStack()
            },
            onProceed = {}
        )
    }


    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Text(
                        text = "Video Call",
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        },
        content = { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AndroidView(
                    factory = { context ->
                        SurfaceViewRenderer(context).apply {

                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )

                AndroidView(
                    factory = { context ->
                        SurfaceViewRenderer(context).apply {

                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
        }
    )

}