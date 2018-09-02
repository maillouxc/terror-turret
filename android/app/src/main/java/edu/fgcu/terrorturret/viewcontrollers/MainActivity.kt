package edu.fgcu.terrorturret.viewcontrollers

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import edu.fgcu.terrorturret.R
import edu.fgcu.terrorturret.applogic.RobotController

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        registerClickListeners()
    }

    private fun registerClickListeners() {
        // TODO
    }

    private fun onClickFireButton() {
        RobotController.fireZeMissiles()
    }

    private fun onAnalogStickMoved(xPos: Int, yPos: Int) {
        // TODO
    }

}
