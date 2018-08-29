package edu.fgcu.terrorturret.applogic

import android.util.Log
import edu.fgcu.terrorturret.LoggerTags.LOG_TURRET_CONTROL
import kotlin.math.absoluteValue

object RobotController {

    private const val NUM_SPEED_SETTINGS = 10

    var isSafetyOn = true

    fun fireZeMissiles() {
        if (!isSafetyOn) {
            Log.i(LOG_TURRET_CONTROL, "Firing ze missiles!")
            // TODO fire ze missiles
        } else {
            Log.i(LOG_TURRET_CONTROL, "Cannot fire, safety is on!")
        }
    }

    fun rotateTurretAtSpeed(speedLevel: Int) {
        validateSpeedSetting(speedLevel)
        // TODO rotate turret
    }

    fun pitchTurretAtSpeed(speedLevel: Int) {
        validateSpeedSetting(speedLevel)
        // TODO pitch turret
    }

    /**
     * Ensures that the speed level passed to one of the servo-commanding functions to pitch or
     * rotate the turret falls within the valid range of speed settings (positive and negative),
     * else throws an IllegalArgumentException.
     */
    private fun validateSpeedSetting(speedLevel: Int) {
        if (speedLevel.absoluteValue > NUM_SPEED_SETTINGS) {
            throw IllegalArgumentException("Speed must be no between + and - $NUM_SPEED_SETTINGS")
        }
    }

}
