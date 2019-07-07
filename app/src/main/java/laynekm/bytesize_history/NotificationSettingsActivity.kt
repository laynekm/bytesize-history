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

    lateinit var sharedPref: SharedPreferences
    lateinit var notificationSwitch: Switch
    lateinit var notificationTimeView: TextView
    lateinit var notificationTimeButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_settings)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        notificationManager = NotificationManager(this)

        preferencesKey = getString(R.string.preferences_key)
        notificationEnabledKey = getString(R.string.notification_enabled_pref_key)
        notificationTimeKey = getString(R.string.notification_time_pref_key)
        notificationTimeDefault = getString(R.string.notification_time_default)

        notificationSwitch = findViewById(R.id.notification_switch)
        notificationTimeView = findViewById(R.id.notification_time)
        notificationTimeButton = findViewById(R.id.notification_time_button)

        notificationSwitch.setOnCheckedChangeListener { _, isChecked ->  toggleNotification(isChecked) }
        notificationTimeButton.setOnClickListener { showTimePickerDialog() }

        sharedPref = this.getSharedPreferences(preferencesKey, Context.MODE_PRIVATE)
        notificationEnabled = sharedPref.getBoolean(notificationEnabledKey,  true)
        notificationTime = sharedPref.getString(notificationTimeKey, notificationTimeDefault)!!

        notificationSwitch.setChecked(notificationEnabled)
        notificationTimeView.text = notificationTime
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
        notificationManager.updateNotification(time)
        notificationTimeView.text = timeToString(time)
    }

    private fun toggleNotification(checked: Boolean) {
        if (!checked) notificationManager.cancelNotification()
    }

}
