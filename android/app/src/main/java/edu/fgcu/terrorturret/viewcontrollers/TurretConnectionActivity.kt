package edu.fgcu.terrorturret.viewcontrollers

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Vibrator
import android.util.Log
import edu.fgcu.terrorturret.LoggerTags
import edu.fgcu.terrorturret.R
import edu.fgcu.terrorturret.network.TurretConnection
import edu.fgcu.terrorturret.utils.shake
import edu.fgcu.terrorturret.utils.toast
import kotlinx.android.synthetic.main.activity_turret_connection.*

class TurretConnectionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_turret_connection)
        connect_button.setOnClickListener { onClickConnect() }
        prefillFieldsWithPastConnectionInfo()
    }

    /**
     * A UI quality-of-life enhancement which prefills the connection form with the info used
     * last time, if there is any. If none was available, the fields stay blank.
     */
    private fun prefillFieldsWithPastConnectionInfo() {
        val sharedPreferences = getPreferences(Context.MODE_PRIVATE) ?: return
        with (sharedPreferences) {
            val lastIpUsed = getString(PREF_LAST_IP_USED, "")
            val lastPortUsed = getString(PREF_LAST_PORT_USED, "")
            val lastPasswordUsed = getString(PREF_LAST_PASSWORD_USED, "")

            field_turret_ip.text.append(lastIpUsed)
            field_turret_port.text.append(lastPortUsed)
            field_turret_password.text.append(lastPasswordUsed)
        }
    }

    /**
     * A UI quality-of-life enhancement which saves the connection info in the activity's shared
     * preferences so that it can be used to prefill the form later.
     */
    private fun saveConnectionInfo() {
        val sharedPreferences = getPreferences(Context.MODE_PRIVATE)
        with (sharedPreferences.edit()) {
            val ip = field_turret_ip.text.toString()
            val port =  field_turret_port.text.toString()
            val password = field_turret_password.text.toString()

            putString(PREF_LAST_IP_USED, ip)
            putString(PREF_LAST_PORT_USED, port)
            putString(PREF_LAST_PASSWORD_USED, password)
            apply()
        }
    }

    private fun onClickConnect() {
        if (validateConnectionInfo()) {
            try {
                TurretConnection.init(
                        field_turret_ip.text.toString(),
                        field_turret_port.text.toString().toInt(),
                        field_turret_password.text.toString()
                )
                onConnected()
            } catch (ex: Exception) {
                toast(R.string.toast_error_connection_failed)
                Log.e(LoggerTags.LOG_PI_CONNECTION, ex.toString())
                return
            }
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
        saveConnectionInfo()
        goToTurretControlActivity()
    }

    private fun goToTurretControlActivity() {
        val turretControlIntent = Intent(this, TurretControlActivity::class.java)
        startActivity(turretControlIntent)
    }

    companion object {
        private const val PREF_LAST_IP_USED = "PREF_LAST_IP_USED"
        private const val PREF_LAST_PORT_USED = "PREF_LAST_PORT_USED"
        private const val PREF_LAST_PASSWORD_USED = "PREF_LAST_PASSWORD_USED"
    }

}
