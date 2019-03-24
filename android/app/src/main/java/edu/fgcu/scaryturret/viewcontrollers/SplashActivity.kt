package edu.fgcu.scaryturret.viewcontrollers

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import edu.fgcu.scaryturret.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.android.Main
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The activity responsible for displaying the application's splash screen.
 *
 * This is the launcher activity for the app, meaning that it is the first activity opened.
 */
class SplashActivity : AppCompatActivity() {

    /**
     * This will be the first method called when the application is launched from the launcher.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        goToMainActivityAfterSlightDelay()
    }

    /**
     * Waits (without blocking) until a preset delay has elapsed, then launches the main activity.
     *
     * This is needed to make the splash screen flash for more than just a fraction of a second.
     * If our app ever reaches a point where it must do significant loading in this activity,
     * we can reduce or eliminate that delay.
     */
    private fun goToMainActivityAfterSlightDelay() {
        launch(Dispatchers.Main) {
            delay(SPLASH_DURATION_MS)
            finish()
            startActivity(Intent(this@SplashActivity, TurretConnectionActivity::class.java))
        }
    }

    companion object {
        private const val SPLASH_DURATION_MS = 1500L
    }

}
