package laynekm.bytesize_history

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.Toast
import java.util.*

// Handles notifications and the shared preferences associated with them
// TODO: Should notifications abide by the user's preferences? What if they have all eras turned off?
class NotificationManager(val context: Context) {

    private val preferencesKey = context.getString(R.string.notification_pref_key)
    private val notificationEnabledKey = context.getString(R.string.notification_enabled_pref_key)
    private val notificationTimeKey = context.getString(R.string.notification_time_pref_key)
    private val notificationTimeDefault = stringToTime(context.getString(R.string.notification_time_default))
    private val sharedPref = context.getSharedPreferences(preferencesKey, Context.MODE_PRIVATE)
    private var notificationTime = stringToTime(sharedPref.getString(notificationTimeKey, timeToString(notificationTimeDefault))!!)

    // Initializes notification preferences to default values if they do not yet exist and notifications are enabled
    init {
        val preferencesEnabled = sharedPref.getBoolean(notificationEnabledKey, true)
        val missingPreferences = !sharedPref.contains(notificationEnabledKey) || !sharedPref.contains(notificationTimeKey)
        if (preferencesEnabled && missingPreferences) {
            setNotificationPreferences(true, notificationTimeDefault)
            setNotification(notificationTimeDefault)
        }

        // TODO: Remove these toasts, just here for testing purposes
        else if (!preferencesEnabled) {
            Toast.makeText(context, "Notifications are disabled.", Toast.LENGTH_LONG).show()
        } else if (!missingPreferences) {
            Toast.makeText(context, "Notification already set for ${timeTo12HourString(notificationTime)}.", Toast.LENGTH_LONG).show()
        }
    }

    // Sets alarm (ie. notification) that will repeat every 24 hours
    private fun setNotification(time: Time) {
        notificationTime = time
        val calendar: Calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, time.hour)
            set(Calendar.MINUTE, time.minute)
            set(Calendar.SECOND, 0)
        }

        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmIntent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        manager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, AlarmManager.INTERVAL_HOUR, pendingIntent)
        // manager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, AlarmManager.INTERVAL_DAY, pendingIntent)

        Toast.makeText(context, "Daily notification set for ${timeTo12HourString(time)}", Toast.LENGTH_LONG).show()
    }

    // Same as setNotification but uses the existing notificationTime instead of a new value
    fun setNotification() {
        setNotification(notificationTime)
    }

    // Cancels notification
    private fun cancelNotification() {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmIntent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        manager.cancel(pendingIntent)
    }

    // Cancels notification and disables notifications in user preferences
    fun disableNotification() {
        cancelNotification()
        setNotificationPreferences(false, notificationTime)
        Toast.makeText(context, "Daily notification disabled", Toast.LENGTH_LONG).show()
    }

    // Updates notification time and user preferences (must cancel first)
    fun updateNotification(time: Time) {
        cancelNotification()
        setNotificationPreferences(true, time)
        setNotification(time)
    }

    private fun setNotificationPreferences(enabled: Boolean, time: Time) {
        with (sharedPref.edit()) {
            putString(notificationTimeKey, timeToString(time))
            apply()
        }

        with (sharedPref.edit()) {
            putBoolean(notificationEnabledKey, enabled)
            apply()
        }
    }
}