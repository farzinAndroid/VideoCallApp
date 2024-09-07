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
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnection.IceServer
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
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
    val eglBase = remember { EglBase.create() }
    var peerConnectionFactory: PeerConnectionFactory? = remember { null }
    var peerConnector: PeerConnection? = remember { null }

    fun initializeWebRtc() {
        Timber.e("initializeWebRtc")

        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions
                .builder(context)
                .setEnableInternalTracer(true)
                .createInitializationOptions()
        )

        val videoEncoderFactory = DefaultVideoEncoderFactory(
            eglBase.eglBaseContext,
            true,
            false
        )

        val videoDecoderFactory = DefaultVideoDecoderFactory(eglBase.eglBaseContext)

        peerConnectionFactory = PeerConnectionFactory
            .builder()
            .setVideoDecoderFactory(videoDecoderFactory)
            .setVideoEncoderFactory(videoEncoderFactory)
            .createPeerConnectionFactory()
    }

    fun createPeerConnection(){
        Timber.e("createPeerConnection")


        val iceServers = IceServer
            .builder(listOf(
                "stun:stun1.l.google.com:19302",
                "stun:stun2.l.google.com:19302",
            ))

        val rtcConfig = PeerConnection.RTCConfiguration(listOf(iceServers.createIceServer()))

        peerConnector = peerConnectionFactory?.createPeerConnection(
            rtcConfig,
            object : PeerConnection.Observer {
                override fun onSignalingChange(p0: PeerConnection.SignalingState?) {

                }

                override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
                    super.onConnectionChange(newState)
                    Timber.e("onConnectionChange $newState")
                }


                override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {

                }

                override fun onIceConnectionReceivingChange(p0: Boolean) {

                }

                override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {

                }

                override fun onIceCandidate(p0: IceCandidate?) {

                }

                override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {

                }

                override fun onAddStream(p0: MediaStream?) {

                }

                override fun onRemoveStream(p0: MediaStream?) {

                }

                override fun onDataChannel(p0: DataChannel?) {

                }

                override fun onRenegotiationNeeded() {

                }

                override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {

                }

            }
        )
    }

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
            onProceed = {
                initializeWebRtc()
            }
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