package edu.fgcu.terrorturret.viewcontrollers

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.WindowManager
import com.github.niqdev.mjpeg.DisplayMode
import edu.fgcu.terrorturret.LoggerTags
import edu.fgcu.terrorturret.R
import edu.fgcu.terrorturret.applogic.TurretController
import edu.fgcu.terrorturret.network.TurretConnection
import kotlinx.android.synthetic.main.activity_turret_control.*

class TurretControlActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_turret_control)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        registerClickListeners()
        registerJoystickMovementListener()
        onClickArmSwitch(false)

        beginStreamingVideo()
    }
    private fun beginStreamingVideo() {
        Log.i(LoggerTags.LOG_PI_CONNECTION, "Beginning video stream.")
        TurretConnection.getVideoStream().subscribe { videoStream ->
            video_view.setSource(videoStream)
            video_view.setDisplayMode(DisplayMode.FULLSCREEN)
        }
    }

    private fun registerJoystickMovementListener() {
        analog_stick.setOnMoveListener { angle, strength ->
            // Strength is a percentage value [0, 100]
            // Angle is degrees measured clockwise from the vertical, so 0 degrees is straight up

            // We need to convert this to a normalized value [0, 1] in (x,y) coordinates
            // I don't have time to rewrite the library to allow this, so for now this is how we
            // do it, but ideally this functionality should be baked in to the library

            val angleInRadians = Math.toRadians(angle)
            val normalizedX = (strength / 100) * Math.cos(angleInRadians)
            val normalizedY = (strength / 100) * Math.sin(angleInRadians)

            TurretController.updateAnalogPosition(normalizedX, normalizedY)
        }
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

}
