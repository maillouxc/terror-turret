package edu.fgcu.terrorturret.viewcontrollers

import android.content.Context
import android.media.AudioManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.WindowManager
import edu.fgcu.terrorturret.LoggerTags
import edu.fgcu.terrorturret.R
import edu.fgcu.terrorturret.applogic.TurretController
import edu.fgcu.terrorturret.network.TurretConnection
import edu.fgcu.terrorturret.network.webrtc.WebRtcConnectionManager
import kotlinx.android.synthetic.main.activity_turret_control.*
import org.webrtc.*

class TurretControlActivity : AppCompatActivity(),
        WebRtcConnectionManager.WebRtcStreamReceiver {

    private lateinit var webRtcConnectionManager: WebRtcConnectionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_turret_control)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        registerClickListeners()
        registerJoystickMovementListener()
        onClickArmSwitch(false)

        webRtcConnectionManager = WebRtcConnectionManager(this, this)

        beginStreamingVideo()
    }

    override fun onDestroy() {
        super.onDestroy()
        webRtcConnectionManager.cleanup()
    }

    private fun beginStreamingVideo() {
        video_view.init(webRtcConnectionManager.rootEglBase.eglBaseContext, null)

        val webRtcIp = TurretConnection.turretIp
        val webRtcPort = TurretConnection.turretPort

        try {
            webRtcConnectionManager.connect(webRtcIp, webRtcPort)
        } catch (ex: Exception) {
            // TODO catch more specific exception
            // TODO attempt to recover from exception
            Log.e(LoggerTags.LOG_WEBRTC, ex.toString())
        }

        enableSpeakerphone()
    }

    override fun onStreamReady(mediaStream: MediaStream) {
        val videoTrack = mediaStream.videoTracks[0]
        val audioTrack = mediaStream.audioTracks[0]
        audioTrack.setEnabled(true)
        runOnUiThread {
            try {
                videoTrack.addSink(video_view)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    /**
     * This function is needed because WebRTC streams are treated as a call by Android,
     * so we have to enable speakerphone if we want the audio to play through the main device
     * speaker and not just the tiny speaker used for phone calls.
     */
    private fun enableSpeakerphone() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.mode = AudioManager.MODE_IN_CALL
        audioManager.isSpeakerphoneOn = true
    }

    private fun registerJoystickMovementListener() {
        analog_stick.setOnMoveListener({ angle, strength ->
            // Strength is a percentage value [0, 100]
            // Angle is degrees measured from the right, counterclockwise.

            // We need to convert this to a normalized value [0, 1] in (x,y) coordinates
            // I don't have time to rewrite the library to allow this, so for now this is how we
            // do it, but ideally this functionality should be baked in to the library

            val angleInRadians = Math.toRadians(angle.toDouble())
            val normalizedX = (strength / 100.0) * Math.cos(angleInRadians)
            val normalizedY = (strength / 100.0) * Math.sin(angleInRadians)

            TurretController.updateAnalogPosition(normalizedX, normalizedY)
        },
        JOYSTICK_UPDATE_FREQ_HZ
        )
    }

    private fun registerClickListeners() {
        fire_button.setOnTouchListener { _, motionEvent -> onClickFire(motionEvent) }
        arm_switch.setOnCheckedChangeListener { _, checked -> onClickArmSwitch(checked) }
    }

    private fun onClickFire(motionEvent: MotionEvent): Boolean {
        when (motionEvent.action) {
            MotionEvent.ACTION_DOWN -> {
                // Button was pressed
                if (arm_switch.isChecked) {
                    TurretController.fireZeMissiles()
                }
            }
            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                // Button was released
                TurretController.ceaseFire()
            }
        }
        return true // We handled the event
    }

    private fun onClickArmSwitch(checked: Boolean) {
        if (checked) {
            fire_button.alpha = 1.0f
            crosshair.alpha = 1.0f
            TurretController.engageSafety(false)
        } else {
            fire_button.alpha = 0.2f
            crosshair.alpha = 0.1f
            TurretController.engageSafety(true)
        }
    }

    companion object {
        const val JOYSTICK_UPDATE_FREQ_HZ = 10
    }

}
