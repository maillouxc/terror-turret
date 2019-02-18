package edu.fgcu.terrorturret.network.webrtc

import android.util.Log
import com.google.gson.Gson
import edu.fgcu.terrorturret.LoggerTags
import edu.fgcu.terrorturret.network.webrtc.dtos.CallRequestOptionsDto
import edu.fgcu.terrorturret.network.webrtc.dtos.SignallingMessageDto
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONException
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import java.util.concurrent.TimeUnit

class Signaller(
        signalingProtocol: String = "wss",
        signallingIp: String,
        signallingPort: Int = 9000,
        private var signalHandler: WebRtcSignalHandler
) {

    interface WebRtcSignalHandler {
        fun onOfferReceived(offer: String)
        fun onIceCandidateReceived(iceCandidate: IceCandidate?)
    }

    private var signallingWebSocket: WebSocket

    private val webSocketListener = object : WebSocketListener() {

        override fun onOpen(webSocket: WebSocket?, response: Response?) {
            Log.d(LoggerTags.LOG_WEBRTC, "Signaller connection opened.")
        }

        override fun onMessage(webSocket: WebSocket?, text: String?) {
            Log.i(LoggerTags.LOG_WEBRTC, "Signaller message received: $text")
            if (text != null) { onSignallingMessageReceived(text) }
        }

        override fun onClosed(webSocket: WebSocket?, code: Int, reason: String?) {
            Log.w(LoggerTags.LOG_WEBRTC, "Signaller connection closed - reason: $reason")
        }

        override fun onFailure(webSocket: WebSocket?, t: Throwable?, response: Response?) {
            Log.e(LoggerTags.LOG_WEBRTC, "Signaller connection failure: $t")
        }

    }

    init {
        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY

        val okHttpClient = OkHttpClient.Builder()
                .readTimeout(TIMEOUT_SECONDS.toLong(), TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor)
                .build()

        val webSocketUrl = val webSocketUrl = "$signalingProtocol://$signallingIp:$signallingPort/stream/webrtc"
        Log.i(LoggerTags.LOG_WEBRTC, "ARGS--> protocol: $signalingProtocol, IP: $signallingIp, port: $signallingPort")
        val webSocketRequest = Request.Builder()
                .url(webSocketUrl)
                .build()

        signallingWebSocket = okHttpClient.newWebSocket(webSocketRequest, webSocketListener)
    }

    fun sendCallRequest() {
        val callRequest = SignallingMessageDto(
                what = "call",
                data = Gson().toJson(
                        CallRequestOptionsDto(
                                forceHwVcodec = false,
                                trickleIce = true
                        )
                )
        )
        signallingWebSocket.send(Gson().toJson(callRequest))
    }

    fun sendAnswer(sessionDescription: SessionDescription) {
        try {
            val obj = JSONObject()
            obj.put("type", "answer")
            obj.put("sdp", sessionDescription.description)

            val wrapper = JSONObject()
            wrapper.put("what", "answer")
            wrapper.put("data", obj.toString())

            Log.d(LoggerTags.LOG_WEBRTC, "Sending answer: $wrapper")

            signallingWebSocket.send(wrapper.toString())
        } catch (ex: JSONException) {
            Log.e(LoggerTags.LOG_WEBRTC, ex.toString())
        }
    }

    private fun onSignallingMessageReceived(message: String) {
        val messageObject = Gson().fromJson(message, SignallingMessageDto::class.java)
        when (messageObject.what) {
            "offer" -> { signalHandler.onOfferReceived(messageObject.data) }
            "message" -> { log("Message received: ${messageObject.data}") }
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
        log("Done receiving trickle ICE candidates!")
    }

    fun cleanup() {
        signallingWebSocket.close(WEBSOCKET_CLOSE_CODE_NORMAL, "App closed")
    }

    private fun log(msg: String) {
        Log.i(LoggerTags.LOG_WEBRTC, msg)
    }

    companion object {
        const val TIMEOUT_SECONDS = 10
        const val WEBSOCKET_CLOSE_CODE_NORMAL = 1000
    }

}
