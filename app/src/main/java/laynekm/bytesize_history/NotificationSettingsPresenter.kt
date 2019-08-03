package laynekm.bytesize_history

import android.content.Context
import android.os.Bundle

/*
    Functions as the presenter for NotificationSettingsActivity
    - Retrieves data from NotificationManager
    - Formats data and sends to View
    - Determines behaviour when user interacts with View
 */

class NotificationSettingsPresenter(val context: Context, val view: View) {

    private val notificationManager = NotificationManager(context)
    private var timePickerVisible: Boolean = false
    private val timePickerVisibleKey: String = "timePickerVisible"

    init {
        ThemeManager(context).applyTheme()
    }

    fun onViewCreated(savedInstanceState: Bundle?) {
        if (savedInstanceState !== null) {
            timePickerVisible = savedInstanceState.getBoolean(timePickerVisibleKey)
        }

        if (timePickerVisible) showTimePickerDialog()
        view.updateUI(notificationManager.getEnabled(), notificationManager.getTime())
    }

    fun setTime(time: Time) {
        onCloseTimePickerDialog()
        notificationManager.setNotification(time)
        view.updateUI(notificationManager.getEnabled(), notificationManager.getTime())
    }

    fun setNotification() {
        if (notificationManager.getEnabled()) notificationManager.disableNotification()
        else notificationManager.setNotification()
        view.updateUI(notificationManager.getEnabled(), notificationManager.getTime())
    }

    fun showTimePickerDialog() {
        timePickerVisible = true
        val hour = notificationManager.getTime().hour
        val minute = notificationManager.getTime().minute
        view.showTimePickerDialog(hour, minute)
    }

    fun onCloseTimePickerDialog() {
        timePickerVisible = false
    }

    fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(timePickerVisibleKey, timePickerVisible)
    }

    interface View {
        fun updateUI(enabled: Boolean, time: Time)
        fun showTimePickerDialog(hour: Int, minute: Int)
    }
}