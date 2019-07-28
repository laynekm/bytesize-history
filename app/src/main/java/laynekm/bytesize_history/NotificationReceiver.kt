package laynekm.bytesize_history

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.os.Build
import android.support.v4.app.NotificationCompat

import laynekm.bytesize_history.R.mipmap.ic_launcher

class NotificationReceiver : BroadcastReceiver() {

    private val CHANNEL_ID = "NotificationReceiver"

    private val contentManager: ContentManager = ContentManager()

    override fun onReceive(context: Context, intent: Intent) {
        this.createNotificationChannel(context)
        this.contentManager.fetchDailyHistoryFact(context, ::pushNotification)
    }

    // REMINDER: Remove "Scheduled to send..." text
    private fun pushNotification(context: Context, historyItem: HistoryItem, date: Date) {

        val preferencesKey = context.getString(R.string.preferences_key)
        val notificationTimeKey = context.getString(R.string.notification_time_pref_key)
        val sharedPref = context.getSharedPreferences(preferencesKey, Context.MODE_PRIVATE)
        val notificationTime = stringTo12HourString(sharedPref.getString(notificationTimeKey, "default time")!!)

        val year = when {
            historyItem.year === null -> "history"
            historyItem.year < 0 -> context.resources.getString(R.string.BC_text, historyItem.year * -1)
            else -> "${historyItem.year}"
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(ic_launcher)
            .setContentTitle(context.getString(R.string.notification_title, buildDateForNotification(date), year))
            .setContentText("${historyItem.desc}\n\nScheduled to send at $notificationTime.")
            .setStyle(NotificationCompat.BigTextStyle())
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, builder.build())
    }

    // Create the NotificationChannel (only necessary for API 26+)
    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Bytesize History Notifications"
            val descriptionText = "Pushes a daily notification at a time of your choosing"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}