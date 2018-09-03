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
    }

    private fun registerClickListeners() {
        fire_button.setOnClickListener { onClickFire() }
    }

    private fun onClickFire() {
        val weaponIsHot = !safety.isChecked
        if (weaponIsHot) {
            RobotController.fireZeMissiles()
        }
    }

}
