package edu.fgcu.scaryturret.network.webrtc

import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

/**
 * This class only exists to soak up some of the required method implementations of the
 * SdpObserver interface, so that we don't have to implement every one of them.
 *
 * Now, we only have to override the methods we are actually going to use.
 *
 * Theoretically, we could add some helpful base class behaviors here as well, such as logging.
 */
open class CustomSdpObserver : SdpObserver {
    override fun onCreateSuccess(sessionDescription: SessionDescription?) {}
    override fun onSetFailure(sessionDescription: String?) {}
    override fun onSetSuccess() {}
    override fun onCreateFailure(sessionDescription: String?) {}
}
