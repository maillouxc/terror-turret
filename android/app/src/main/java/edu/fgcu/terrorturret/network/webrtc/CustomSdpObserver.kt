package edu.fgcu.terrorturret.network.webrtc

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
    override fun onCreateSuccess(p0: SessionDescription?) {}
    override fun onSetFailure(p0: String?) {}
    override fun onSetSuccess() {}
    override fun onCreateFailure(p0: String?) {}
}
