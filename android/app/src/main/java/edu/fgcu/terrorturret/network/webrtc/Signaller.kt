package edu.fgcu.terrorturret.network.webrtc

import android.util.Log
import com.google.gson.Gson
import edu.fgcu.terrorturret.LoggerTags
import edu.fgcu.terrorturret.network.webrtc.dtos.CallRequestOptionsDto
import edu.fgcu.terrorturret.network.webrtc.dtos.SignallingMessageDto
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

class Signaller(
        signallingIp: String,
        port: Int,
        private var signalHandler: WebRtcSignalHandler
) {

    interface WebRtcSignalHandler {
        fun onOfferReceived(offer: String)
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

        val webSocketUrl = "ws://$signallingIp:$port/stream/webrtc"
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

    fun onIceCandidateReceived() {
        // TODO
    }

    private fun onSignallingMessageReceived(message: String) {
        val messageObject = Gson().fromJson(message, SignallingMessageDto::class.java)
        when (messageObject.what) {
            "offer" -> { signalHandler.onOfferReceived(messageObject.data) }
            "message" -> { log("Message received: ${messageObject.data}") }
            "iceCandidate" -> { } // TODO
        }
    }

    fun cleanup() {
        // TODO
    }

    private fun log(msg: String) {
        Log.i(LoggerTags.LOG_WEBRTC, msg)
    }

    companion object {
        const val TIMEOUT_SECONDS = 5
        const val STUN_SERVER_URL = "stun:stun.1.google.com:19302"
    }

}
