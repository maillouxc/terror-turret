package edu.fgcu.terrorturret.viewcontrollers

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import edu.fgcu.terrorturret.R
import edu.fgcu.terrorturret.applogic.TurretController
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        registerClickListeners()
    }

    private fun registerClickListeners() {
        connect_button.setOnClickListener { onClickConnect() }
    }

    private fun onClickConnect() {
        // TODO implement real connect method
        // For now, just show the turret control layout

        // TODO remove the temp testing code below
        onConnected()
    }

    private fun onConnected() {
        // TODO implement

        // TODO remove test code
        val turretControlIntent = Intent(this, TurretControlActivity::class.java)
        startActivity(turretControlIntent)
    }

    private fun onConnectionEnd() {
        // TODO
    }

    private fun onClickFireButton() {
        TurretController.fireZeMissiles()
    }

    private fun onAnalogStickMoved(xPos: Int, yPos: Int) {
        // TODO
    }

}
