package edu.fgcu.scaryturret.network

import android.util.Log
import edu.fgcu.scaryturret.LoggerTags
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

object TurretConnection {

    var turretIp: String = ""
    var turretPort: Int  = 9000
    var protocol: String = "ws"

    private var timeout = 5
    private var turretPassword: String = ""

    private lateinit var webSocket: WebSocket

    fun init(turretIp: String, turretPort: Int, turretPassword: String, protocol: String) {
        this.turretIp = turretIp
        this.turretPort = turretPort
        this.turretPassword = turretPassword
        this.protocol = protocol

        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY

        val okHttpClient = OkHttpClient.Builder()
                .readTimeout(timeout.toLong(), TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor)
                .build()

        // We need to indicate this to the user somewhere that this is what's happening
        val webSocketPort = turretPort + 1

        val webSocketRequest = Request.Builder()
                .url("ws://$turretIp:$webSocketPort/")
                .build()

        webSocket = okHttpClient.newWebSocket(webSocketRequest, webSocketListener)
    }

    fun sendTurretCommand(command: String) {
        Log.d(LoggerTags.LOG_PI_CONNECTION, "Sending command: $command")
        webSocket.send(command)
    }

    private val webSocketListener = object : WebSocketListener() {

        override fun onOpen(webSocket: WebSocket?, response: Response?) {
            Log.d(LoggerTags.LOG_PI_CONNECTION, "Connection opened.")
        }

        override fun onMessage(webSocket: WebSocket?, text: String?) {
            Log.i(LoggerTags.LOG_PI_CONNECTION, "Message Received: $text")
        }

        override fun onClosed(webSocket: WebSocket?, code: Int, reason: String?) {
            Log.w(LoggerTags.LOG_PI_CONNECTION, "Connection closed - reason: $reason")
        }

        override fun onFailure(webSocket: WebSocket?, t: Throwable?, response: Response?) {
            Log.e(LoggerTags.LOG_PI_CONNECTION, "Connection failure: $t")
        }

    }

}
