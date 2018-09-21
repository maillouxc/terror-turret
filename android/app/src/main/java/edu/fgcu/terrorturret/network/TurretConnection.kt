package edu.fgcu.terrorturret.network

import android.util.Log
import com.github.niqdev.mjpeg.Mjpeg
import com.github.niqdev.mjpeg.MjpegInputStream
import rx.Observable

object TurretConnection {

    var timeout = 5
    var analogUpdateFreqHz = 60
    var turretIp: String = ""
    var turretPort: Int  = 8080
    var turretPassword: String = ""

    fun init(turretIp: String, turretPort: Int, turretPassword: String) {
        this.turretIp = turretIp
        this.turretPort = turretPort
        this.turretPassword = turretPassword
    }

    fun getVideoStream(): Observable<MjpegInputStream> {
        val connectionString = "http://$turretIp:$turretPort/stream/video.mjpeg"
        Log.d("TURRET", "Connecting on: $connectionString")
        return Mjpeg.newInstance()
                .open(connectionString, timeout)
    }

}
