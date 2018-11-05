package edu.fgcu.terrorturret.network.webrtc

import android.content.Context
import android.util.Log
import edu.fgcu.terrorturret.LoggerTags
import org.json.JSONException
import org.json.JSONObject
import org.webrtc.*

class WebRtcConnectionManager(
        private val context: Context,
        private val webRtcStreamReceiver: WebRtcStreamReceiver
): Signaller.WebRtcSignalHandler {

    interface WebRtcStreamReceiver {
        fun onStreamReady(mediaStream: MediaStream)
    }

    private lateinit var localPeer: PeerConnection
    private lateinit var signaller: Signaller
    private var sdpConstraints = MediaConstraints()

    private var peerIceServers: MutableList<PeerConnection.IceServer> = ArrayList()

    private val peerConnectionFactory: PeerConnectionFactory by lazy {
        PeerConnectionFactory.initializeAndroidGlobals(context, true)
        val options = PeerConnectionFactory.Options()
        PeerConnectionFactory(options)
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
            signaller.sendAnswer()
        }

    }

    fun connect(ip: String, port: Int) {
        signaller = Signaller(ip, port, this)
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
            // Use ECDSA encryption
            keyType = PeerConnection.KeyType.ECDSA
        }

        sdpConstraints = MediaConstraints()

        localPeer = peerConnectionFactory.createPeerConnection(
                webRtcConfig, sdpConstraints, peerConnectionObserver
        )
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
            // TODO determine how to handle
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
