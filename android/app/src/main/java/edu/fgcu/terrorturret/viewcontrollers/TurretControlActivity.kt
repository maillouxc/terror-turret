package edu.fgcu.terrorturret.viewcontrollers

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import com.github.niqdev.mjpeg.DisplayMode
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
        beginStreamingVideo()
        onSwitchSafety(false)
    }

    private fun beginStreamingVideo() {
        TurretConnection.getVideoStream().subscribe { videoStream ->
            video_view.setSource(videoStream)
            video_view.setDisplayMode(DisplayMode.FULLSCREEN)
            video_view.showFps(true)
        }
    }

    private fun registerClickListeners() {
        fire_button.setOnClickListener { onClickFire() }
        safety.setOnCheckedChangeListener { _, checked -> onSwitchSafety(checked) }
    }

    private fun onClickFire() {
        val weaponIsHot = !safety.isChecked
        if (weaponIsHot) {
            TurretController.fireZeMissiles()
        }
    }

    private fun onSwitchSafety(checked: Boolean) {
        if (checked) {
            // Weapon is now hot
            fire_button.alpha = 1.0f
            crosshair.alpha = 1.0f
            TurretController.engageSafety(false)

            // TODO send command to gun to disengage safety
        } else {
            // Weapon is now safe
            fire_button.alpha = 0.2f
            crosshair.alpha = 0.1f
            TurretController.engageSafety(true)

            // TODO send command to gun to engage safety
        }
    }

}
