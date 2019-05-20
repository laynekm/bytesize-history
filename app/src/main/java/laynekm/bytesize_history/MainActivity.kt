package laynekm.bytesize_history

import android.app.DatePickerDialog
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity()  {

    private val contentProvider: ContentProvider = ContentProvider()
    private lateinit var historyItemAdapter: HistoryItemAdapter
    private var selectedDate: Date = getToday()
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        updateDate(selectedDate)
        progressBar = findViewById(R.id.progressBar)

        // Populate recycler view with empty list to initialize
        populateRecyclerView(ArrayList())

        // Initialize text view with current date and fetch history data
        var dateLabel: TextView = findViewById(R.id.dateLabel)
        dateLabel.text = buildDateLabel(selectedDate)
        getHistoryData()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Populate recycler view with initial empty data
    private fun populateRecyclerView(items: MutableList<HistoryItem>) {
        val historyItemView: RecyclerView = findViewById(R.id.historyItems)
        historyItemAdapter = HistoryItemAdapter(this, items)
        historyItemView.adapter = historyItemAdapter
        historyItemView.layoutManager = LinearLayoutManager(this)
    }

    // Fetch data and show progress bar
    private fun getHistoryData() {
        progressBar.visibility = View.VISIBLE
        contentProvider.getHistoryData(buildDateURL(selectedDate), ::updateRecyclerView)
    }

    // Populate recycler view with fetched data nad hide progress bar
    private fun updateRecyclerView(items: MutableList<HistoryItem>) {
        progressBar.visibility = View.GONE
        historyItemAdapter.setItems(items)
        historyItemAdapter.notifyDataSetChanged()
    }

    private fun updateDate(date: Date) {
        if (!datesEqual(date, selectedDate)) {
            selectedDate = date
            var dateLabel: TextView = findViewById(R.id.dateLabel)
            dateLabel.text = buildDateLabel(selectedDate)
            getHistoryData()
        }
    }

    fun showDatePickerDialog(view: View) {
        var date: Calendar = Calendar.getInstance()
        var selectedYear = date.get(Calendar.YEAR)
        var selectedMonth = selectedDate.month
        var selectedDay = selectedDate.day

        // TODO: Hide year label
        val datePicker = DatePickerDialog(this, DatePickerDialog.OnDateSetListener { _, _, month, day ->
            updateDate(Date(month, day))
        }, selectedYear, selectedMonth, selectedDay)

        datePicker.show()
    }
}
