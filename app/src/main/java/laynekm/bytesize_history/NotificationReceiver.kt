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

    private val contentProvider: ContentProvider = ContentProvider()

    // TODO: Fix notifications not working over data connection
    override fun onReceive(context: Context, intent: Intent) {
        this.createNotificationChannel(context)
        this.contentProvider.fetchDailyHistoryFact(context, ::pushNotification)
    }

    private fun pushNotification(context: Context, historyItem: HistoryItem, date: Date) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(ic_launcher)
            .setContentTitle(context.getString(R.string.notification_title, buildDateForNotification(date), historyItem.year))
            .setContentText(historyItem.desc)
            .setStyle(NotificationCompat.BigTextStyle())
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, builder.build())
    }

    // Create the NotificationChannel, but only on API 26+ because
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