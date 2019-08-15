package laynekm.bytesizehistory

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
    private val notificationTimeDefault = stringToTime(context.getString(R.string.notification_time_default))
    private val sharedPref = context.getSharedPreferences(preferencesKey, Context.MODE_PRIVATE)

    private var notificationEnabled = sharedPref.getBoolean(notificationEnabledKey, true)
    private var notificationTime = stringToTime(sharedPref.getString(notificationTimeKey, timeToString(notificationTimeDefault))!!)

    // Sets notification if it is enabled in user preferences but is not active for whatever reason
    fun checkNotification() {
        val alarmActive = this.alarmActive()
        if (notificationEnabled && !alarmActive) setNotification()
        // else if (alarmActive) Toast.makeText(context, "Alarm already set for ${timeTo12HourString(notificationTime)}", Toast.LENGTH_LONG).show()
        // else Toast.makeText(context, "Notifications disabled.", Toast.LENGTH_LONG).show()
    }

    // Sets alarm (ie. notification) that will repeat every 24 hours
    fun setNotification(time: Time = notificationTime) {
        notificationEnabled = true
        notificationTime = time
        setNotificationPreferences(notificationEnabled, notificationTime)
        cancelNotification()

        val calendar: Calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, notificationTime.hour)
            set(Calendar.MINUTE, notificationTime.minute)
            set(Calendar.SECOND, 0)
        }

        // If time is in past, set it for next day otherwise it will be triggered immediately
        if (calendar.timeInMillis < System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmIntent = Intent(context, NotificationReceiver::class.java).let { intent ->
            PendingIntent.getBroadcast(context, 0, intent, 0)
        }

        manager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, 1000 * 60 * 60 * 24, alarmIntent)
        Toast.makeText(context, "Daily notification set for ${timeTo12HourString(notificationTime)}.", Toast.LENGTH_LONG).show()
    }

    // Cancels notification and disables notifications in user preferences
    fun disableNotification() {
        notificationEnabled = false
        setNotificationPreferences(notificationEnabled, notificationTime)
        cancelNotification()
        Toast.makeText(context, "Daily notification has been disabled.", Toast.LENGTH_LONG).show()
    }

    private fun cancelNotification() {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmIntent = Intent(context, NotificationReceiver::class.java).let { intent ->
            PendingIntent.getBroadcast(context, 0, intent, 0)
        }
        manager.cancel(alarmIntent)
    }

    private fun alarmActive(): Boolean {
        val alarmIntent = Intent(context, NotificationReceiver::class.java)
        return PendingIntent.getBroadcast(context, 0, alarmIntent, PendingIntent.FLAG_NO_CREATE) !== null
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

    fun getEnabled(): Boolean = notificationEnabled
    fun getTime(): Time = notificationTime
}