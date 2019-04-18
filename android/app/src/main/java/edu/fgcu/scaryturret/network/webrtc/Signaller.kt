package edu.fgcu.scaryturret.network.webrtc

import android.util.Log
import com.google.gson.Gson
import edu.fgcu.scaryturret.LoggerTags.LOG_WEBRTC
import edu.fgcu.scaryturret.network.webrtc.dtos.CallRequestOptionsDto
import edu.fgcu.scaryturret.network.webrtc.dtos.SignallingMessageDto
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONException
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import java.util.concurrent.TimeUnit

/**
 * Implements the WebRTC signalling protocol - but only the bits we need.
 *
 * The protocol is simple on the surface but can get annoyingly complex, and documentation is
 * poor at best.
 *
 * See the documentation in the individual methods of this class to gain more insight into how
 * this all works.
 */
class Signaller(
        signallingProtocol: String = "ws",
        signallingIp: String,
        signallingPort: Int,
        private var signalHandler: WebRtcSignalHandler
) {

    interface WebRtcSignalHandler {
        fun onOfferReceived(offer: String)
        fun onIceCandidateReceived(iceCandidate: IceCandidate?)
        fun onConnectionFailure(msg: String)
    }

    private var signallingWebSocket: WebSocket

    private val webSocketListener = object : WebSocketListener() {

        override fun onOpen(webSocket: WebSocket?, response: Response?) {
            Log.i(LOG_WEBRTC, "Signaller connection opened.")
        }

        override fun onMessage(webSocket: WebSocket?, text: String?) {
            Log.i(LOG_WEBRTC, "Signaller message received: $text")
            if (text != null) { onSignallingMessageReceived(text) }
        }

        override fun onClosed(webSocket: WebSocket?, code: Int, reason: String?) {
            Log.i(LOG_WEBRTC, "Signaller connection closed - reason: $reason")
        }

        override fun onFailure(webSocket: WebSocket?, t: Throwable?, response: Response?) {
            Log.e(LOG_WEBRTC, "Signaller connection failure: $t")
            signalHandler.onConnectionFailure("$t")
        }

    }

    init {
        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY

        val okHttpClient = OkHttpClient.Builder()
                .readTimeout(TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor)
                .build()

        // Connect to the UV4L signalling websocket
        val webSocketUrl = "$signallingProtocol://$signallingIp:$signallingPort/stream/webrtc"
        Log.i(LOG_WEBRTC, "Attempting to connect to signalling server: $webSocketUrl")
        val webSocketRequest = Request.Builder().url(webSocketUrl).build()
        signallingWebSocket = okHttpClient.newWebSocket(webSocketRequest, webSocketListener)
    }

    /**
     * Sends the request to begin a call to UV4L.
     *
     * Due to the way UV4L works, this is a little different than how WebRTC signalling
     * normally works. Essentially, we send this message to ask UV4L to call us, since we can't
     * directly call UV4L. UV4L should next respond with an "offer".
     */
    fun sendCallRequest() {
        val callRequest = SignallingMessageDto(
                what = "call",
                data = Gson().toJson(
                        CallRequestOptionsDto(
                                forceHwVcodec = true,
                                trickleIce = true
                        )
                )
        )
        signallingWebSocket.send(Gson().toJson(callRequest))
    }

    /**
     * When UV4L sends a call offer, we need to respond with an answer containing our local
     * session description.
     */
    fun sendAnswer(sessionDescription: SessionDescription) {
        try {
            val obj = JSONObject()
            obj.put("type", "answer")
            obj.put("sdp", sessionDescription.description)

            val wrapper = JSONObject()
            wrapper.put("what", "answer")
            wrapper.put("data", obj.toString())

            Log.d(LOG_WEBRTC, "Sending answer: $wrapper")
            signallingWebSocket.send(wrapper.toString())
        } catch (ex: JSONException) {
            Log.e(LOG_WEBRTC, ex.toString())
        }
    }

    private fun onSignallingMessageReceived(message: String) {
        val messageObject = Gson().fromJson(message, SignallingMessageDto::class.java)
        when (messageObject.what) {
            "offer" -> { signalHandler.onOfferReceived(messageObject.data) }
            "message" -> { Log.i(LOG_WEBRTC, "Message received: ${messageObject.data}") }
            "iceCandidate" -> {
                try {
                    onIceCandidateReceived(JSONObject(messageObject.data))
                } catch (ex: JSONException) {
                    // This signifies that the last (empty) ice candidate was received
                    onIceCandidateReceived(null)
                }
            }
        }
    }

    private fun onIceCandidateReceived(data: JSONObject?) {
        if (data == null) {
            onDoneReceivingIceCandidates()
        }
        else {
            val iceCandidate = IceCandidate(
                    data.getString("sdpMid"),
                    data.getInt("sdpMLineIndex"),
                    data.getString("candidate")
            )
            signalHandler.onIceCandidateReceived(iceCandidate)
        }
    }

    private fun onDoneReceivingIceCandidates() {
        Log.i(LOG_WEBRTC, "Done receiving trickle ICE candidates!")
    }

    fun cleanup() {
        signallingWebSocket.close(WEBSOCKET_CLOSE_CODE_NORMAL, "App closed")
    }

    companion object {
        const val TIMEOUT_SECONDS = 5
        const val WEBSOCKET_CLOSE_CODE_NORMAL = 1000
    }

}
