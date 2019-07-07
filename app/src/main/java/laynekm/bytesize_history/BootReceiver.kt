package laynekm.bytesize_history

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

// Receives ACTION_BOOT_COMPLETED broadcast and sets alarm from user preferences
// This is necessary because all alarms are cancelled when a device shuts down
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.intent.action.BOOT_COMPLETED") {
            val notificationManager = NotificationManager(context)
            notificationManager.setNotification()
        }
    }
}