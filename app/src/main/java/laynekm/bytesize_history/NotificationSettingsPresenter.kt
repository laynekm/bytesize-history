package laynekm.bytesize_history

import android.content.Context

class NotificationSettingsPresenter(val context: Context, val view: View) {

    private val notificationManager = NotificationManager(context)

    init {
        ThemeManager(context).applyTheme()
    }

    fun onViewCreated() {
        view.updateUI(notificationManager.getEnabled(), notificationManager.getTime())
    }

    fun setTime(time: Time) {
        notificationManager.setNotification(time)
        view.updateUI(notificationManager.getEnabled(), notificationManager.getTime())
    }

    fun setNotification() {
        if (notificationManager.getEnabled()) notificationManager.disableNotification()
        else notificationManager.setNotification()
        view.updateUI(notificationManager.getEnabled(), notificationManager.getTime())
    }

    fun showTimePickerDialog() {
        val hour = notificationManager.getTime().hour
        val minute = notificationManager.getTime().minute
        view.showTimePickerDialog(hour, minute)
    }

    interface View {
        fun updateUI(enabled: Boolean, time: Time)
        fun showTimePickerDialog(hour: Int, minute: Int)
    }
}