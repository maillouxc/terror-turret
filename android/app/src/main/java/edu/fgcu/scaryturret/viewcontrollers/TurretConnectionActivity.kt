package edu.fgcu.scaryturret.viewcontrollers

import android.Manifest
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Vibrator
import android.util.Log
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import edu.fgcu.scaryturret.LoggerTags.LOG_PI_CONNECTION
import edu.fgcu.scaryturret.R
import edu.fgcu.scaryturret.network.TurretConnection
import edu.fgcu.scaryturret.utils.shake
import edu.fgcu.scaryturret.utils.toast
import kotlinx.android.synthetic.main.activity_turret_connection.*

class TurretConnectionActivity : AppCompatActivity(),
        TurretConnection.TurretConnectionStatusListener {

    /**
     * Used to handle permission request responses.
     */
    private var permissionsListener: PermissionListener = object : PermissionListener {
        override fun onPermissionGranted() {
            // We're now good to connect - we have the needed permissions
            connect()
        }

        override fun onPermissionDenied(deniedPermissions: List<String>) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_turret_connection)
        connect_button.setOnClickListener { onClickConnect() }
        prefillFieldsWithPastConnectionInfo()
    }

    /**
     * Called when the connection to the turret control websocket fails.
     */
    override fun onConnectionFailed(msg: String) {
        runOnUiThread {
            toast(R.string.toast_error_turret_connection_failed)
            Handler().postDelayed({finish()}, 1000)
        }
    }

    /**
     * A UI quality-of-life enhancement which prefills the connection form with the info used
     * last time, if there is any. If none was available, the fields stay blank.
     */
    private fun prefillFieldsWithPastConnectionInfo() {
        val sharedPreferences = getPreferences(Context.MODE_PRIVATE) ?: return
        with (sharedPreferences) {
            val lastIpUsed = getString(PREF_LAST_IP_USED, "")
            val lastTurretPortUsed = getString(PREF_LAST_TURRET_PORT_USED, "")
            val lastVideoPortUsed = getString(PREF_LAST_VIDEO_PORT_USED, "")
            val lastPasswordUsed = getString(PREF_LAST_PASSWORD_USED, "")
            val lastSSLUsed = getBoolean(PREF_LAST_SSL_USED, false)

            field_turret_ip.text.append(lastIpUsed)
            field_turret_port.text.append(lastTurretPortUsed)
            field_video_port.text.append(lastVideoPortUsed)
            field_turret_password.text.append(lastPasswordUsed)
            field_ssl.isChecked = lastSSLUsed
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
            val turretPort =  field_turret_port.text.toString()
            val videoPort = field_video_port.text.toString()
            val password = field_turret_password.text.toString()
            val ssl = field_ssl.isChecked

            putString(PREF_LAST_IP_USED, ip)
            putString(PREF_LAST_TURRET_PORT_USED, turretPort)
            putString(PREF_LAST_VIDEO_PORT_USED, videoPort)
            putString(PREF_LAST_PASSWORD_USED, password)
            putBoolean(PREF_LAST_SSL_USED, ssl)
            apply()
        }
    }

    private fun onClickConnect() {
        // We request the needed permissions - if they are successful, then we connect
        TedPermission.with(applicationContext)
                .setPermissionListener(permissionsListener)
                .setDeniedMessage(getString(R.string.permission_reason_microphone))
                .setPermissions(Manifest.permission.RECORD_AUDIO)
                .check()
    }

    private fun connect() {
        if (validateConnectionInfo()) {
            try {
                TurretConnection.init(
                        turretIp = field_turret_ip.text.toString(),
                        turretPort = field_turret_port.text.toString().toInt(),
                        videoPort = field_video_port.text.toString().toInt(),
                        turretPassword = field_turret_password.text.toString(),
                        protocol = if (field_ssl.isChecked) "wss" else "ws",
                        turretConnectionStatusListener = this
                )
                onConnected()
            } catch (ex: Exception) {
                toast(R.string.toast_error_connection_failed)
                Log.e(LOG_PI_CONNECTION, ex.toString())
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
        field_video_port.error = null
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
        if (field_video_port.text.isNullOrEmpty()) {
            field_video_port.shake()
            field_video_port.error = getString(R.string.validation_error_required_field)
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
        private const val PREF_LAST_TURRET_PORT_USED = "PREF_LAST_TURRET_PORT_USED"
        private const val PREF_LAST_VIDEO_PORT_USED = "PREF_LAST_VIDEO_PORT_USED"
        private const val PREF_LAST_PASSWORD_USED = "PREF_LAST_PASSWORD_USED"
        private const val PREF_LAST_SSL_USED = "PREF_LAST_SSL_USED"
    }

}
