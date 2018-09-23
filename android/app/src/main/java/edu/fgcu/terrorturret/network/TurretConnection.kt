package edu.fgcu.terrorturret.network

import android.util.Log
import com.github.niqdev.mjpeg.Mjpeg
import com.github.niqdev.mjpeg.MjpegInputStream
import edu.fgcu.terrorturret.LoggerTags
import okhttp3.*
import rx.Observable
import java.util.concurrent.TimeUnit

object TurretConnection {

    private var timeout = 5
    private var turretIp: String = ""
    private var turretPort: Int  = 8080
    private var turretPassword: String = ""

    private lateinit var webSocket: WebSocket

    fun init(turretIp: String, turretPort: Int, turretPassword: String) {
        this.turretIp = turretIp
        this.turretPort = turretPort
        this.turretPassword = turretPassword

        val okHttpClient = OkHttpClient.Builder()
                .readTimeout(timeout.toLong(), TimeUnit.SECONDS)
                .build()

        val webSocketRequest = Request.Builder()
                .url("ws://$turretIp:$turretPort/")
                .build()

        webSocket = okHttpClient.newWebSocket(webSocketRequest, webSocketListener)
    }

    fun getVideoStream(): Observable<MjpegInputStream> {
        val connectionString = "http://$turretIp:$turretPort/stream/video.mjpeg"
        return Mjpeg.newInstance().open(connectionString, timeout)
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
            Log.d(LoggerTags.LOG_PI_CONNECTION, "Message Received: $text")
        }

        override fun onClosed(webSocket: WebSocket?, code: Int, reason: String?) {
            Log.d(LoggerTags.LOG_PI_CONNECTION, "Connection closed - reason: $reason")
        }

        override fun onFailure(webSocket: WebSocket?, t: Throwable?, response: Response?) {
            Log.d(LoggerTags.LOG_PI_CONNECTION, "Connection failure: $t")
        }

    }

}
