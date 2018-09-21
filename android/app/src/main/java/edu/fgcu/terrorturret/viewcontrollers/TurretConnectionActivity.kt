package edu.fgcu.terrorturret.viewcontrollers

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Vibrator
import edu.fgcu.terrorturret.R
import edu.fgcu.terrorturret.utils.shake
import kotlinx.android.synthetic.main.activity_turret_connection.*

class TurretConnectionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_turret_connection)
        connect_button.setOnClickListener { onClickConnect() }
    }

    private fun onClickConnect() {
        if (validateConnectionInfo()) {
            onConnected()
        }
    }

    /**
     * Returns true if the form fields are valid and we can proceed, else returns false.
     *
     * This method also handles setting an error message based on the validation problem that was
     * encountered, and vibrating the device if the form is invalid.
     *
     * While it would be nice to have a robust form validation library, it would really be overkill
     * for a simple app like this.
     */
    private fun validateConnectionInfo(): Boolean {
        // Clear any existing errors
        field_turret_ip.error = null
        field_turret_port.error = null
        field_turret_password.error = null

        var valid = true

        // Ensure all required fields are filled
        if (field_turret_ip.text.isNullOrEmpty()) {
            field_turret_ip.shake()
            field_turret_ip.error = getString(R.string.validation_error_required_field)
            valid = false
        }
        if (field_turret_port.text.isNullOrEmpty()) {
            field_turret_port.shake()
            field_turret_port.error = getString(R.string.validation_error_required_field)
            valid = false
        }
        if (field_turret_password.text.isNullOrEmpty()) {
            field_turret_password.shake()
            field_turret_password.error = getString(R.string.validation_error_required_field)
            valid = false
        }

        if (!valid) {
            (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(200)
        }

        return valid
    }

    private fun onConnected() {
        goToTurretControlActivity()
    }

    private fun goToTurretControlActivity() {
        val turretControlIntent = Intent(this, TurretControlActivity::class.java)
        startActivity(turretControlIntent)
    }

}
