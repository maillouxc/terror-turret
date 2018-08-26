package edu.fgcu.terrorturret

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle

import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch

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
        launch(UI) {
            finish()
            delay(SPLASH_DURATION_MS)
            startActivity(Intent(this@SplashActivity, MainActivity::class.java))
        }
    }

    companion object {
        private const val SPLASH_DURATION_MS = 1000
    }

}
