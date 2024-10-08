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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.DataChannel
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnection.IceServer
import org.webrtc.PeerConnectionFactory
import org.webrtc.RendererCommon
import org.webrtc.RtpReceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.SurfaceViewRenderer
import timber.log.Timber
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoCallScreen(
    navController: NavController,
    roomId: String,
) {

    val context = LocalContext.current

    val executor = remember { Executors.newSingleThreadExecutor() }
    val fireStore = remember { FirebaseFirestore.getInstance() }
    val eglBase = remember { EglBase.create() }
    var peerConnectionFactory: PeerConnectionFactory? = remember { null }
    var peerConnection: PeerConnection? = remember { null }
    var localCandidatesToShare = remember { arrayListOf<Map<String, Any?>>() }
    var queuedRemoteCandidates = remember { arrayListOf<IceCandidate>() }
    val sdpMediaConstraints = remember {
        MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", true.toString()))
        }
    }

    var isOfferer by remember { mutableStateOf(false) }
    var remoteDescriptionSet by remember { mutableStateOf(false) }


    // renderers
    var localRenderer by remember { mutableStateOf<SurfaceViewRenderer?>(null) }
    var remoteRenderer by remember { mutableStateOf<SurfaceViewRenderer?>(null) }

    fun sendSignallingMessage(message: Map<String, Any?>) {
        Timber.e("sendSignallingMessage $message")

        val signallingRef = fireStore.collection("rooms").document(roomId)

        signallingRef.set(message, SetOptions.merge())
    }

    var localSDP: SessionDescription? = remember { null }

    val sdpObserver = object : SdpObserver {
        override fun onCreateSuccess(sessionDescription: SessionDescription) {
            Timber.d("onCreateSuccess")

            if (localSDP != null) {
                Timber.e("localSDP not null session created")
                return
            }

            localSDP = sessionDescription

            executor.execute {
                peerConnection?.setLocalDescription(this, sessionDescription)
            }
        }

        override fun onSetSuccess() {

            if (localSDP == null) return


            executor.execute {
                if (isOfferer) {
                    if (peerConnection?.remoteDescription == null) {
                        // answer no yet received
                        sendSignallingMessage(
                            mapOf(
                                "type" to "offer",
                                "sdpOffer" to localSDP?.description
                            )
                        )
                    } else {
                        remoteDescriptionSet = true
                        addQueuedCandidates()
                    }
                } else {
                    if (peerConnection?.localDescription != null) {
                        sendSignallingMessage(
                            mapOf(
                                "type" to "answer",
                                "sdpAnswer" to localSDP?.description
                            )
                        )
                        remoteDescriptionSet = true
                        addQueuedCandidates()
                    }
                }
            }

        }

        private fun addQueuedCandidates() {
            queuedRemoteCandidates.forEach {
                peerConnection?.addIceCandidate(it)
            }

            queuedRemoteCandidates.clear()
        }

        override fun onCreateFailure(p0: String?) {
        }

        override fun onSetFailure(p0: String?) {
        }

    }

    fun createAnswer() {
        peerConnection?.createAnswer(sdpObserver, sdpMediaConstraints)
    }

    fun handleSignallingMessages(data: Map<String, Any>) {
        //candidates

        if (isOfferer && data["iceAnswer"] != null) {

            executor.execute {
                val cmDataList = data["iceAnswer"] as List<*>
                cmDataList.forEach {
                    val cData = it as Map<*, *>

                    val candidate = IceCandidate(
                        /* sdpMid = */ cData["sdpMid"] as String,
                        /* sdpMLineIndex = */ (cData["sdpMLineIndex"] as Long).toInt(),
                        /* sdp = */ cData["candidate"] as String
                    )

                    //remote description
                    if (remoteDescriptionSet) {
                        peerConnection?.addIceCandidate(candidate)
                    } else {
                        queuedRemoteCandidates.add(candidate)
                    }
                }

                // clear data
                sendSignallingMessage(
                    mapOf(
                        "iceAnswer" to null
                    )
                )
            }


        }

        if (!isOfferer && data["iceOffer"] != null) {

            executor.execute {
                val cmDataList = data["iceOffer"] as List<*>
                cmDataList.forEach {
                    val cData = it as Map<*, *>

                    val candidate = IceCandidate(
                        /* sdpMid = */ cData["sdpMid"] as String,
                        /* sdpMLineIndex = */ (cData["sdpMLineIndex"] as Long).toInt(),
                        /* sdp = */ cData["candidate"] as String
                    )

                    //remote description
                    if (remoteDescriptionSet) {
                        peerConnection?.addIceCandidate(candidate)
                    } else {
                        queuedRemoteCandidates.add(candidate)
                    }
                }

                // clear data
                sendSignallingMessage(
                    mapOf(
                        "iceOffer" to null
                    )
                )
            }


        }

        if (!isOfferer && data["sdpOffer"] != null) {
            executor.execute {
                val offerSDP = data["sdpOffer"] as String
                val offer = SessionDescription(SessionDescription.Type.OFFER, offerSDP)
                sendSignallingMessage(mapOf("sdpOffer" to null))

                peerConnection?.setRemoteDescription(sdpObserver, offer)
                createAnswer()
            }
        }

        if (isOfferer && data["sdpAnswer"] != null) {
            executor.execute {
                val answerSdp = data["sdpAnswer"] as String
                val answer = SessionDescription(SessionDescription.Type.ANSWER, answerSdp)
                sendSignallingMessage(mapOf("sdpAnswer" to null))
                peerConnection?.setRemoteDescription(sdpObserver, answer)
            }
        }
    }

    fun setupFirebaseListeners() {
        Timber.e("setupFirebaseListeners")

        val signallingRef = fireStore.collection("rooms").document(roomId)

        signallingRef.addSnapshotListener { value, error ->
            if (error != null) {
                error.printStackTrace()
                return@addSnapshotListener
            }

            value?.data?.let {
                handleSignallingMessages(it)
            }
        }

    }

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

    fun createPeerConnection() {
        Timber.e("createPeerConnection")


        val iceServers = IceServer
            .builder(
                listOf(
                    "stun:stun1.l.google.com:19302",
                    "stun:stun2.l.google.com:19302",
                )
            )

        val rtcConfig = PeerConnection.RTCConfiguration(listOf(iceServers.createIceServer()))

        peerConnection = peerConnectionFactory?.createPeerConnection(
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

                override fun onIceCandidate(candidate: IceCandidate?) {

                    val key = if (isOfferer) "iceOffer" else "iceAnswer"

                    candidate?.let {
                        localCandidatesToShare.add(
                            mapOf(
                                "candidate" to it.sdp,
                                "sdpMid" to it.sdpMid,
                                "sdpMLineIndex" to it.sdpMLineIndex
                            )
                        )
                    }

                    // send to signalling server (firebase)
                    sendSignallingMessage(
                        mapOf(key to localCandidatesToShare)
                    )

                }

                override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {

                }

                override fun onAddStream(streams: MediaStream?) {
                    Timber.e("onAddStream $streams")
                    streams?.videoTracks?.firstOrNull()?.addSink(remoteRenderer)
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
        val roomRefID = fireStore.collection("rooms").document(roomId)

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
                        onProceed()
                    }


                } else {
                    roomRefID.set(mapOf("participantCount" to 1))
                    isOfferer = true
                    onProceed()
                }

            }
            .addOnFailureListener {
                Timber.e("Fire store error ${it.message}")
                Toast.makeText(context, "Firebase failed", Toast.LENGTH_SHORT).show()
                onNavigateBack()
            }
    }


    fun createCameraCapturer(): CameraVideoCapturer? {
        val cameraEnumerator = Camera2Enumerator(context)
        val deviceNames = cameraEnumerator.deviceNames

        for (deviceName in deviceNames) {
            if (cameraEnumerator.isFrontFacing(deviceName)) {
                val videoCapturer = cameraEnumerator.createCapturer(deviceName, null)
                if (videoCapturer != null) {
                    return videoCapturer
                }
            }
        }

        for (deviceName in deviceNames) {
            if (!cameraEnumerator.isFrontFacing(deviceName)) {
                val videoCapturer = cameraEnumerator.createCapturer(deviceName, null)
                if (videoCapturer != null) {
                    return videoCapturer
                }
            }
        }

        return null
    }


    // local rendering
    fun initializeLocalMediaStream() {
        Timber.e("initializeLocalMediaStream")

        val videoSource = peerConnectionFactory?.createVideoSource(false)
        videoSource ?: run {
            Timber.e("videoSource is null")
        }

        val videoCapturer = createCameraCapturer()

        videoCapturer?.let { vc ->
            vc.initialize(
                /* p0 = */ SurfaceTextureHelper.create(
                    /* threadName = */ "CaptureThread",/* sharedContext = */eglBase.eglBaseContext
                ),
                /* p1 = */ context,
                /* p2 = */ videoSource?.capturerObserver
            )

            vc.startCapture(1280, 720, 30)
        }

        val videoTrack = peerConnectionFactory?.createVideoTrack("1001", videoSource)
        val mediaStream = peerConnectionFactory?.createLocalMediaStream("mediaStream")
        mediaStream?.addTrack(videoTrack)
        peerConnection?.addStream(mediaStream)
        videoTrack?.addSink(localRenderer)
    }

    fun createOffer() {
        Timber.e("create offer")
        peerConnection?.createOffer(sdpObserver, sdpMediaConstraints)

    }

    fun deleteFirestoreDoc(){
        val signallingRef = fireStore.collection("rooms").document(roomId)
        signallingRef.delete()
    }


    LaunchedEffect(true) {
        checkRoomCapacityAndSetup(
            onNavigateBack = {
                navController.popBackStack()
            },
            onProceed = {
                executor.execute {
                    initializeWebRtc()
                    createPeerConnection()
                    initializeLocalMediaStream()
                    setupFirebaseListeners()
                    if (isOfferer) {
                        createOffer()
                    }
                }

            }
        )
    }


    DisposableEffect(Unit) {
        onDispose {
            executor.execute {
                localRenderer?.release()?.also {
                    localRenderer = null
                }

                remoteRenderer?.release()?.also {
                    remoteRenderer = null
                }

                peerConnection?.dispose().also {
                    peerConnection = null
                }
                peerConnectionFactory?.dispose().also {
                    peerConnectionFactory = null
                }

                eglBase.release()

                PeerConnectionFactory.stopInternalTracingCapture()
                PeerConnectionFactory.shutdownInternalTracer()

                deleteFirestoreDoc()

            }
            executor.shutdown()
        }
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
                            init(
                                /* sharedContext = */ eglBase.eglBaseContext,
                                /* rendererEvents = */ null
                            )
                            setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
                            localRenderer = this
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )

                AndroidView(
                    factory = { context ->
                        SurfaceViewRenderer(context).apply {
                            init(
                                /* sharedContext = */ eglBase.eglBaseContext,
                                /* rendererEvents = */ null
                            )
                            setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT)
                            remoteRenderer = this
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