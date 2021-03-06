package laynekm.bytesizehistory

import android.app.TimePickerDialog
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.widget.Button
import android.widget.TextView

/*
    Functions as the view for the NotificationSettingsPresenter class
    Displays user preferences surrounding notifications (ie. enabled/disabled and time)
 */

class NotificationSettingsActivity : AppCompatActivity(), NotificationSettingsPresenter.View {

    private lateinit var presenter: NotificationSettingsPresenter
    private lateinit var toolbar: Toolbar
    private lateinit var notificationSummaryTextView: TextView
    private lateinit var notificationToggleButton: Button
    private lateinit var notificationTimeButton: Button
    private lateinit var timePickerDialog: TimePickerDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        presenter = NotificationSettingsPresenter(this, this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_settings)

        toolbar = findViewById(R.id.toolbar)
        notificationSummaryTextView = findViewById(R.id.notification_summary_text)
        notificationToggleButton = findViewById(R.id.notification_toggle_btn)
        notificationTimeButton = findViewById(R.id.notification_time_btn)

        toolbar.setNavigationOnClickListener { onBackPressed() }
        notificationToggleButton.setOnClickListener { presenter.setNotification() }
        notificationTimeButton.setOnClickListener { presenter.showTimePickerDialog() }
        presenter.onViewCreated(savedInstanceState)
    }

    override fun updateUI(enabled: Boolean, time: Time) {
        if (enabled) {
            notificationSummaryTextView.text = getString(R.string.notification_summary_enabled, timeTo12HourString(time))
            notificationToggleButton.text = getString(R.string.notification_disable)
            notificationTimeButton.isEnabled = true
            notificationTimeButton.alpha = 1f
        } else {
            notificationSummaryTextView.text = getString(R.string.notification_summary_disabled)
            notificationToggleButton.text = getString(R.string.notification_enable)
            notificationTimeButton.isEnabled = false
            notificationTimeButton.alpha = .5f
        }
    }

    override fun showTimePickerDialog(hour: Int, minute: Int) {
        timePickerDialog = TimePickerDialog(this, TimePickerDialog.OnTimeSetListener { _, h, m ->
            presenter.setTime(Time(h, m))
        }, hour, minute, false)
        timePickerDialog.setOnDismissListener { presenter.onCloseTimePickerDialog() }
        timePickerDialog.show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        presenter.onSaveInstanceState(outState)
    }

    override fun onPause() {
        super.onPause()
        if (::timePickerDialog.isInitialized) timePickerDialog.dismiss()
    }
}
