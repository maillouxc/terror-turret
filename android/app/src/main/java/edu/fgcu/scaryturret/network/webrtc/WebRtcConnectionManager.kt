package edu.fgcu.scaryturret.network.webrtc

import android.Manifest
import android.content.Context
import android.util.Log
import com.gun0912.tedpermission.PermissionListener
import edu.fgcu.scaryturret.LoggerTags
import org.json.JSONException
import org.json.JSONObject
import org.webrtc.*
import android.widget.Toast
import com.gun0912.tedpermission.TedPermission


class WebRtcConnectionManager(
        private val appContext: Context,
        private val webRtcStreamReceiver: WebRtcStreamReceiver
): Signaller.WebRtcSignalHandler {

    interface WebRtcStreamReceiver {
        fun onStreamReady(mediaStream: MediaStream)
    }

    /**
     * Used to handle permission request responses
     */
    private var permissionsListener: PermissionListener = object : PermissionListener {
        override fun onPermissionGranted() {
            Toast.makeText(appContext, "Permission Granted", Toast.LENGTH_SHORT).show()
        }

        override fun onPermissionDenied(deniedPermissions: List<String>) {
            Toast.makeText(appContext, "Microphone permission rejected", Toast.LENGTH_SHORT).show()
        }
    }

    init {
        TedPermission.with(appContext)
                .setPermissionListener(permissionsListener)
                .setDeniedMessage("We need microphone permissions to make things work!")
                .setPermissions(Manifest.permission.RECORD_AUDIO)
                .check()
    }

    private lateinit var localPeer: PeerConnection
    private lateinit var signaller: Signaller

    private var sdpConstraints = MediaConstraints()
    private var peerIceServers: MutableList<PeerConnection.IceServer> = ArrayList()

    val rootEglBase = RootEglBaseBuilder().rootEglBase!!

    private val peerConnectionFactory: PeerConnectionFactory by lazy {
        // Initialize PeerConnectionFactory global options
        val pcfInitOptions = PeerConnectionFactory.InitializationOptions.builder(appContext)
                .setEnableVideoHwAcceleration(true)
                .createInitializationOptions()
        PeerConnectionFactory.initialize(pcfInitOptions)
        val videoEncoderFactory = DefaultVideoEncoderFactory(rootEglBase.eglBaseContext, true, true)
        val videoDecoderFactory = DefaultVideoDecoderFactory(rootEglBase.eglBaseContext)
        val options = PeerConnectionFactory.Options() // Currently does nothing but is required
        PeerConnectionFactory(options, videoEncoderFactory, videoDecoderFactory)
    }

    private var sdpObserver = object: CustomSdpObserver() {}

    private var peerConnectionObserver = object: CustomPeerConnectionObserver() {
        override fun onIceCandidate(iceCandidate: IceCandidate?) {
            onIceCandidateReceived(iceCandidate)
        }
        override fun onAddStream(mediaStream: MediaStream?) {
            gotRemoteStream(mediaStream)
        }
    }

    private var answerObserver = object: CustomSdpObserver() {
        override fun onCreateSuccess(sessionDescription: SessionDescription?) {
            super.onCreateSuccess(sessionDescription)
            localPeer.setLocalDescription(CustomSdpObserver(), sessionDescription)
            signaller.sendAnswer(sessionDescription!!)
        }
    }

    fun connect(ip: String) {
        signaller = Signaller(signallingIp = ip, signallingPort = 9002, signalHandler = this)
        createPeerConnection()
        signaller.sendCallRequest()
    }

    private fun createPeerConnection() {
        val webRtcConfig = PeerConnection.RTCConfiguration(peerIceServers)
        with (webRtcConfig) {
            tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED
            bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
            rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            keyType = PeerConnection.KeyType.ECDSA // Use ECDSA encryption
        }

        // Send local audio to turret
        val audioConstraints = MediaConstraints()
        val localAudioSource = peerConnectionFactory.createAudioSource(audioConstraints)
        val localAudioTrack = peerConnectionFactory.createAudioTrack("101", localAudioSource)
        val outboundStream = peerConnectionFactory.createLocalMediaStream("102")
        outboundStream.addTrack(localAudioTrack)

        localPeer = peerConnectionFactory.createPeerConnection(
                webRtcConfig, MediaConstraints(), peerConnectionObserver
        )!!


        localPeer.addStream(outboundStream)
    }

    override fun onOfferReceived(offer: String) {
        try {
            val offerSdp = JSONObject(offer).getString("sdp")
            localPeer.setRemoteDescription(
                    sdpObserver,
                    SessionDescription(SessionDescription.Type.OFFER, offerSdp)
            )
            localPeer.createAnswer(answerObserver, sdpConstraints)
        } catch (ex: JSONException) {
            Log.e(LoggerTags.LOG_WEBRTC, ex.toString())
        }
    }

    override fun onIceCandidateReceived(iceCandidate: IceCandidate?) {
        if (iceCandidate != null) {
            localPeer.addIceCandidate(iceCandidate)
        }
    }

    private fun gotRemoteStream(mediaStream: MediaStream?) {
        Log.i(LoggerTags.LOG_WEBRTC, "Got remote stream")
        if (mediaStream != null) {
            webRtcStreamReceiver.onStreamReady(mediaStream)
        }
    }

    fun cleanup() {
        localPeer.close()
        signaller.cleanup()
    }

}
