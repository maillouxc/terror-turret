package edu.fgcu.scaryturret.network.webrtc

import org.webrtc.*

/**
 * This class only exists to soak up some of the required method implementations of the
 * PeerConnection.Observer interface, so that we don't have to implement every one of them.
 *
 * Now, we only have to override the methods we are actually going to use.
 *
 * Theoretically, we could add some helpful base class behaviors here as well, such as logging.
 */
open class CustomPeerConnectionObserver : PeerConnection.Observer {
    override fun onIceCandidate(iceCandidate: IceCandidate?) {}
    override fun onAddStream(mediaStream: MediaStream?) {}
    override fun onDataChannel(p0: DataChannel?) {}
    override fun onIceConnectionReceivingChange(p0: Boolean) {}
    override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {}
    override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {}
    override fun onSignalingChange(p0: PeerConnection.SignalingState?) {}
    override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {}
    override fun onRemoveStream(p0: MediaStream?) {}
    override fun onRenegotiationNeeded() {}
    override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {}
}
