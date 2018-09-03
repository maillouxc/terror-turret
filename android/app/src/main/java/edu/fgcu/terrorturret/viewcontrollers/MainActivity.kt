package edu.fgcu.terrorturret.viewcontrollers

import android.content.pm.ActivityInfo
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import edu.fgcu.terrorturret.R
import edu.fgcu.terrorturret.applogic.RobotController
import kotlinx.android.synthetic.main.layout_turret_connect.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_turret_connect)
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
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        setContentView(R.layout.layout_turret_control)

        // TODO remove the temp testing code below
        launch(UI) {
            delay(5000)
            onConnectionEnd()
        }
    }

    private fun onConnectionEnd() {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.layout_turret_connect)
    }

    private fun onClickFireButton() {
        RobotController.fireZeMissiles()
    }

    private fun onAnalogStickMoved(xPos: Int, yPos: Int) {
        // TODO
    }

}
