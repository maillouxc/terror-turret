package edu.fgcu.scaryturret.network.webrtc

import android.content.Context
import android.util.Log
import org.json.JSONException
import org.json.JSONObject
import org.webrtc.*
import edu.fgcu.scaryturret.LoggerTags.LOG_WEBRTC

/**
 * This class is responsible for handling the WebRTC connection to the turret.
 */
class WebRtcConnectionManager(
        private val appContext: Context,
        private val webRtcStreamReceiver: WebRtcStreamReceiver
): Signaller.WebRtcSignalHandler {

    interface WebRtcStreamReceiver {
        fun onStreamReady(mediaStream: MediaStream)
        fun onSignallingConnectionFailed(msg: String)
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

    /**
     * Sends a message to UV4L requesting that it calls us - just a weird quirk of how UV4L works.
     */
    fun connect(signallingProtocol: String, signallingIp: String, signallingPort: Int) {
        signaller = Signaller(signallingProtocol, signallingIp, signallingPort, signalHandler = this)
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

        @Suppress("DEPRECATION")
        localPeer = peerConnectionFactory.createPeerConnection(
                webRtcConfig, MediaConstraints(), peerConnectionObserver
        )!!

        localPeer.addStream(outboundStream)
    }

    /**
     * Called when the remote peer's call offer is received - responds by answering the offer,
     * and sets the remote peer description.
     */
    override fun onOfferReceived(offer: String) {
        try {
            val offerSdp = JSONObject(offer).getString("sdp")
            localPeer.setRemoteDescription(
                    sdpObserver,
                    SessionDescription(SessionDescription.Type.OFFER, offerSdp)
            )
            localPeer.createAnswer(answerObserver, sdpConstraints)
        } catch (ex: JSONException) {
            Log.e(LOG_WEBRTC, ex.toString())
        }
    }

    /**
     * Called when an ICE candidate is received by the remote peer.
     * Adds the new candidate to the list of available ICE candidates.
     */
    override fun onIceCandidateReceived(iceCandidate: IceCandidate?) {
        if (iceCandidate != null) {
            localPeer.addIceCandidate(iceCandidate)
        }
    }

    /**
     * Called when the signaller WebSocket connection fails.
     */
    override fun onConnectionFailure(msg: String) {
        // Just pass the message through to the calling class
        webRtcStreamReceiver.onSignallingConnectionFailed(msg)
    }

    private fun gotRemoteStream(mediaStream: MediaStream?) {
        Log.i(LOG_WEBRTC, "Got remote stream")
        if (mediaStream != null) {
            webRtcStreamReceiver.onStreamReady(mediaStream)
        } else {
            Log.w(LOG_WEBRTC, "Remote stream was null!")
        }
    }

    fun cleanup() {
        localPeer.close()
        signaller.cleanup()
    }

}
