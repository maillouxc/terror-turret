package edu.fgcu.scaryturret.network

import android.util.Log
import edu.fgcu.scaryturret.LoggerTags.LOG_PI_CONNECTION
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

/**
 * This singleton object is responsible for handling the websocket connection to the turret, used
 * to send commands to move and control the turret.
 */
object TurretConnection {

    interface TurretConnectionStatusListener {
        fun onConnectionFailed(msg: String)
    }

    private lateinit var turretConnectionStatusListener: TurretConnectionStatusListener

    var turretIp: String = ""
    var turretPort: Int = 0
    var videoPort: Int = 0
    var protocol: String = ""

    private var timeout = 5
    private var turretPassword: String = ""

    private lateinit var webSocket: WebSocket

    /**
     * Initializes the connection to the turret.
     */
    fun init(turretIp: String,
             turretPort: Int,
             videoPort: Int,
             turretPassword: String,
             protocol: String,
             turretConnectionStatusListener: TurretConnectionStatusListener) {

        this.turretIp = turretIp
        this.turretPort = turretPort
        this.videoPort = videoPort
        this.turretPassword = turretPassword
        this.protocol = protocol
        this.turretConnectionStatusListener = turretConnectionStatusListener

        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY

        val okHttpClient = OkHttpClient.Builder()
                .readTimeout(timeout.toLong(), TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor)
                .build()

        val webSocketRequest = Request.Builder()
                .url("$protocol://$turretIp:$turretPort/")
                .build()

        webSocket = okHttpClient.newWebSocket(webSocketRequest, webSocketListener)
    }

    /**
     * Sends the provided command to the turret over the websocket connection.
     */
    fun sendTurretCommand(command: String) {
        Log.d(LOG_PI_CONNECTION, "Sending command: $command")
        webSocket.send(command)
    }

    private val webSocketListener = object : WebSocketListener() {

        override fun onOpen(webSocket: WebSocket?, response: Response?) {
            Log.i(LOG_PI_CONNECTION, "Connection opened.")
        }

        override fun onMessage(webSocket: WebSocket?, text: String?) {
            Log.i(LOG_PI_CONNECTION, "Message Received: $text")
        }

        override fun onClosed(webSocket: WebSocket?, code: Int, reason: String?) {
            Log.i(LOG_PI_CONNECTION, "Connection closed - reason: $reason")
        }

        override fun onFailure(webSocket: WebSocket?, t: Throwable?, response: Response?) {
            Log.e(LOG_PI_CONNECTION, "Connection failure: $t")
            turretConnectionStatusListener.onConnectionFailed("$t")
        }

    }

}
