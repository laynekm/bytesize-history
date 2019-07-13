package laynekm.bytesize_history

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.SharedPreferences
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.view.View
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

class NotificationSettingsActivity : AppCompatActivity() {

    private lateinit var notificationManager: NotificationManager
    private var preferencesKey = ""
    private var notificationEnabledKey = ""
    private var notificationTimeKey = ""
    private var notificationTimeDefault = ""
    private var notificationEnabled: Boolean = false
    private var notificationTime: String = notificationTimeDefault

    private lateinit var sharedPref: SharedPreferences
    private lateinit var notificationSummaryTextView: TextView
    private lateinit var notificationToggleButton: Button
    private lateinit var notificationTimeButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_settings)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        notificationManager = NotificationManager(this)

        preferencesKey = getString(R.string.notification_pref_key)
        notificationEnabledKey = getString(R.string.notification_enabled_pref_key)
        notificationTimeKey = getString(R.string.notification_time_pref_key)
        notificationTimeDefault = getString(R.string.notification_time_default)

        notificationSummaryTextView = findViewById(R.id.notification_summary_text)
        notificationToggleButton = findViewById(R.id.notification_toggle_btn)
        notificationTimeButton = findViewById(R.id.notification_time_btn)

        sharedPref = this.getSharedPreferences(preferencesKey, Context.MODE_PRIVATE)
        notificationEnabled = sharedPref.getBoolean(notificationEnabledKey,  true)
        notificationTime = sharedPref.getString(notificationTimeKey, notificationTimeDefault)!!

        if (notificationEnabled) {
            notificationSummaryTextView.text = getString(R.string.notification_summary_enabled, notificationTime)
            notificationToggleButton.text = getString(R.string.notification_disable)
        } else {
            notificationSummaryTextView.text = getString(R.string.notification_summary_disabled)
            notificationToggleButton.text = getString(R.string.notification_enable)
            notificationTimeButton.isEnabled = false
        }

        notificationToggleButton.setOnClickListener { toggleNotification() }
        notificationTimeButton.setOnClickListener { showTimePickerDialog() }
    }

    private fun showTimePickerDialog() {
        val selectedTime = stringToTime(notificationTime)
        val selectedHour = selectedTime.hour
        val selectedMinute = selectedTime.minute

        TimePickerDialog(this, TimePickerDialog.OnTimeSetListener { _, hour, minute ->
            updateTime(Time(hour, minute))
        }, selectedHour, selectedMinute, false).show()
    }

    private fun updateTime(time: Time) {
        if (timesEqual(stringToTime(notificationTime), time)) return

        notificationManager.updateNotification(time)
        notificationTime = timeToString(time)
        notificationSummaryTextView.text = getString(R.string.notification_summary_enabled, notificationTime)
    }

    private fun toggleNotification() {
        notificationEnabled = !notificationEnabled

        if (notificationEnabled) {
            notificationManager.updateNotification(stringToTime(notificationTime))
            notificationSummaryTextView.text = getString(R.string.notification_summary_enabled, notificationTime)
            notificationToggleButton.text = getString(R.string.notification_disable)
            notificationTimeButton.isEnabled = true
        } else {
            notificationManager.disableNotification()
            notificationSummaryTextView.text = getString(R.string.notification_summary_disabled)
            notificationToggleButton.text = getString(R.string.notification_enable)
            notificationTimeButton.isEnabled = false
        }
    }

}
