package laynekm.bytesize_history

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.Toast
import java.util.*

// Handles notifications and the shared preferences associated with them
class NotificationManager(val context: Context) {

    private val preferencesKey = context.getString(R.string.preferences_key)
    private val notificationEnabledKey = context.getString(R.string.notification_enabled_pref_key)
    private val notificationTimeKey = context.getString(R.string.notification_time_pref_key)
    private val notificationTimeDefault = context.getString(R.string.notification_time_default)
    private val sharedPref = context.getSharedPreferences(preferencesKey, Context.MODE_PRIVATE)

    fun initializePreferences() {
        // If notifications are not enabled, no need to do anything
        if (!sharedPref.getBoolean(notificationEnabledKey, true)) return

        // Set sharedPref values to defaults if they do not exist
        if (!sharedPref.contains(notificationEnabledKey)) {
            with (sharedPref.edit()) {
                putBoolean(notificationEnabledKey, true)
                apply()
            }
        }

        if (!sharedPref.contains(notificationTimeKey)) {
            with (sharedPref.edit()) {
                putString(notificationTimeKey, notificationTimeDefault)
                apply()
            }
        }

        // TODO: Remove test time (replace defaultTime with notificationTimeDefault)
        // TODO: Set alarm only if it isn't already set (how to check?)
        val defaultTime = "${Calendar.getInstance().get(Calendar.HOUR_OF_DAY)}:${Calendar.getInstance().get(Calendar.MINUTE) + 1}"
//        val notificationTime: String = sharedPref.getString(notificationTimeKey, defaultTime)!!
        val notificationTime = defaultTime
        setNotification(stringToTime(notificationTime))
    }

    private fun setNotification(time: Time) {
        // Set shared preferences
        with (sharedPref.edit()) {
            putString(notificationTimeKey, timeToString(time))
            apply()
        }

        // Set NotificationReceiver
        val calendar: Calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, time.hour)
            set(Calendar.MINUTE, time.minute)
            set(Calendar.SECOND, 0)
        }

        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmIntent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        manager.cancel(pendingIntent)
        manager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, 60000, pendingIntent)
//    manager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, AlarmManager.INTERVAL_DAY, pendingIntent)

        Toast.makeText(context, "Daily notification set for ${timeToString(time)}", Toast.LENGTH_LONG).show()
    }

    fun updateNotification(time: Time) {
        cancelNotification()
        setNotification(time)
    }

    fun cancelNotification() {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmIntent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        manager.cancel(pendingIntent)
        Toast.makeText(context, "Daily notification disabled", Toast.LENGTH_LONG).show()
    }
}