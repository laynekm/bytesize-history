package laynekm.bytesize_history

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.os.Build
import android.support.v4.app.NotificationCompat

import laynekm.bytesize_history.R.mipmap.ic_launcher
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread

// Receives notification broadcast and pushes notification
// Should be called once per day if user has notifications enabled
class NotificationReceiver : BroadcastReceiver() {

    private val CHANNEL_ID = "NotificationReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        this.createNotificationChannel(context)
        doAsync {
            val historyItem = ContentManager().fetchDailyHistoryFact()
            uiThread { pushNotification(context, historyItem) }
        }
    }

    private fun pushNotification(context: Context, historyItem: HistoryItem) {

        val preferencesKey = context.getString(R.string.preferences_key)
        val notificationTimeKey = context.getString(R.string.notification_time_pref_key)
        val sharedPref = context.getSharedPreferences(preferencesKey, Context.MODE_PRIVATE)
        val notificationTime = stringTo12HourString(sharedPref.getString(notificationTimeKey, "default time")!!)
        val year = historyItem.formattedYear
        val date = historyItem.date

        // Set up intent to launch app when notification is tapped
        val notifyIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val notifyPendingIntent = PendingIntent.getActivity(
            context, 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(ic_launcher)
            .setContentTitle(context.getString(R.string.notification_title, buildDateForNotification(date), year))
            .setContentText(historyItem.desc)
            // .setContentText("${historyItem.desc}\n\nScheduled to send at $notificationTime.")
            .setStyle(NotificationCompat.BigTextStyle())
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(notifyPendingIntent)

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