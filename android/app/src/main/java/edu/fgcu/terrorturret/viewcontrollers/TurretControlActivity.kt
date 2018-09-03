package edu.fgcu.terrorturret.viewcontrollers

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import edu.fgcu.terrorturret.R
import edu.fgcu.terrorturret.applogic.RobotController
import kotlinx.android.synthetic.main.activity_turret_control.*

class TurretControlActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_turret_control)
        registerClickListeners()

        // Update the UI for the state of "Safety engaged"
        onSwitchSafety(false)
    }

    private fun registerClickListeners() {
        fire_button.setOnClickListener { onClickFire() }
        safety.setOnCheckedChangeListener { _, checked -> onSwitchSafety(checked) }
    }

    private fun onClickFire() {
        val weaponIsHot = !safety.isChecked
        if (weaponIsHot) {
            RobotController.fireZeMissiles()
        }
    }

    private fun onSwitchSafety(checked: Boolean) {
        if (checked) {
            // Weapon is now hot
            fire_button.alpha = 1.0f
            crosshair.alpha = 1.0f
            RobotController.engageSafety(false)

            // TODO send command to gun to disengage safety
        } else {
            // Weapon is now safe
            fire_button.alpha = 0.2f
            crosshair.alpha = 0.1f
            RobotController.engageSafety(true)

            // TODO send command to gun to engage safety
        }
    }

}
