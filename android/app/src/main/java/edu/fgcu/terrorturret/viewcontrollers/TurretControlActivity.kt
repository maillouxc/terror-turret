package edu.fgcu.terrorturret.viewcontrollers

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.WindowManager
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import edu.fgcu.terrorturret.LoggerTags
import edu.fgcu.terrorturret.R
import edu.fgcu.terrorturret.applogic.TurretController
import edu.fgcu.terrorturret.network.TurretConnection
import edu.fgcu.terrorturret.network.webrtc.WebRtcConnectionManager
import kotlinx.android.synthetic.main.activity_turret_control.*
import org.webrtc.*
import java.util.ArrayList
import org.webrtc.VideoCapturer

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

        val permissionListener = object: PermissionListener {
            override fun onPermissionGranted() {
                tryLocalVideoRendering()
            }

            override fun onPermissionDenied(deniedPermissions: ArrayList<String>?) {
                // TODO
            }
        }
        TedPermission.with(this)
                .setPermissionListener(permissionListener)
                .setPermissions(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
                .check()

        //beginStreamingVideo()
    }

    private fun tryLocalVideoRendering() {
        fun createCameraCapturer(enumerator: CameraEnumerator): VideoCapturer? {
            val deviceNames = enumerator.deviceNames

            Log.d(LoggerTags.LOG_WEBRTC, "Trying to find front-facing camera")
            for (deviceName in deviceNames) {
                if (enumerator.isFrontFacing(deviceName)) {
                    val videoCapturer = enumerator.createCapturer(deviceName, null)
                    if (videoCapturer != null) {
                        return videoCapturer
                    }
                }
            }

            Log.d(LoggerTags.LOG_WEBRTC, "Looking for other cameras.")
            for (deviceName in deviceNames) {
                if (!enumerator.isFrontFacing(deviceName)) {
                    Log.d(LoggerTags.LOG_WEBRTC, "Creating other camera capturer.")
                    val videoCapturer = enumerator.createCapturer(deviceName, null)
                    if (videoCapturer != null) {
                        return videoCapturer
                    }
                }
            }

            return null
        }

        val videoCapturer = createCameraCapturer(Camera1Enumerator(false))
        val pcf = webRtcConnectionManager.peerConnectionFactory
        val videoConstraints = MediaConstraints()
        val audioConstraints = MediaConstraints()
        val videoSource = pcf.createVideoSource(videoCapturer)
        val localVideoTrack = pcf.createVideoTrack("100", videoSource)
        val audioSource = pcf.createAudioSource(audioConstraints)
        val localAudioTrack = pcf.createAudioTrack("101", audioSource)
        val targetVideoFramerate = 30
        val videoWidth = 1024
        val videoHeight = 720

        videoCapturer!!.startCapture(videoWidth, videoHeight, targetVideoFramerate)
        localVideoTrack.addSink(video_view)
        localAudioTrack.setEnabled(true)


        video_view.setMirror(true)

        video_view.init(webRtcConnectionManager.rootEglBase.eglBaseContext, null)
        val localVideoRenderer = VideoRenderer(video_view)
        localVideoTrack.addRenderer(localVideoRenderer)

    }

    override fun onDestroy() {
        super.onDestroy()
        webRtcConnectionManager.cleanup()
    }

    private fun beginStreamingVideo() {
        video_view.init(webRtcConnectionManager.rootEglBase.eglBaseContext, null)

        val webRtcIp = TurretConnection.turretIp
        val webRtcPort = TurretConnection.turretPort
        //video_view.setZOrderMediaOverlay(true)

        try {
            webRtcConnectionManager.connect(webRtcIp, webRtcPort)
        } catch (ex: Exception) {
            // TODO catch more specific exception
            // TODO attempt to recover from exception
            Log.e(LoggerTags.LOG_WEBRTC, ex.toString())
        }
    }

    override fun onStreamReady(mediaStream: MediaStream) {
        val videoTrack = mediaStream.videoTracks[0]
        runOnUiThread {
            try {
                val remoteRenderer = VideoRenderer(video_view)
                videoTrack.addRenderer(remoteRenderer)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
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
