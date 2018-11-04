package edu.fgcu.terrorturret.network.webrtc

import android.content.Context
import org.webrtc.*

class WebRtcConnectionManager(private val context: Context): Signaller.WebRtcSignalHandler {

    private lateinit var localPeer: PeerConnection
    private lateinit var signaller: Signaller
    private var sdpConstraints = MediaConstraints()

    private var peerIceServers: MutableList<PeerConnection.IceServer> = ArrayList()

    private val peerConnectionFactory: PeerConnectionFactory by lazy {
        PeerConnectionFactory.initializeAndroidGlobals(context, true)
        val options = PeerConnectionFactory.Options()
        PeerConnectionFactory(options)
    }

    private var sdpObserver = object: CustomSdpObserver() {
        // TODO
    }

    private var peerConnectionObserver = object: CustomPeerConnectionObserver() {

        override fun onIceCandidate(iceCandidate: IceCandidate?) {
            onIceCandidateReceived(iceCandidate)
        }

        override fun onAddStream(mediaStream: MediaStream?) {
            gotRemoteStream(mediaStream)
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
        localPeer.setRemoteDescription(
                sdpObserver, SessionDescription(SessionDescription.Type.OFFER, offer)
        )
    }

    private fun onIceCandidateReceived(iceCandidate: IceCandidate?) {
        localPeer.addIceCandidate(iceCandidate)
    }

    private fun gotRemoteStream(mediaStream: MediaStream?) {
        // TODO
    }

    fun cleanup() {
        localPeer.close()
        signaller.cleanup()
    }

}
