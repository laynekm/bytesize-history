package laynekm.dailyhistory

import android.app.DatePickerDialog
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_main.*
import java.net.URL
import java.util.*

class MainActivity : AppCompatActivity()  {

    private val contentProvider: ContentProvider = ContentProvider()
    private lateinit var historyItemAdapter: HistoryItemAdapter
    private var selectedDate: Date = getToday()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        updateDate(selectedDate)

        // Populate recycler view with empty list to initialize
        populateRecyclerView(ArrayList())

        // Initialize text view and fetch history content
        var dateLabel: TextView = findViewById(R.id.dateLabel)
        dateLabel.text = buildDateLabel(selectedDate)
        contentProvider.getHistoryData(buildDateURL(selectedDate), ::updateRecyclerView)
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

    private fun populateRecyclerView(items: MutableList<HistoryItem>) {
        var historyItemView: RecyclerView = findViewById(R.id.historyItems)
        historyItemAdapter = HistoryItemAdapter(this, items)
        historyItemView.adapter = historyItemAdapter
        historyItemView.layoutManager = LinearLayoutManager(this)
    }

    private fun updateRecyclerView(items: MutableList<HistoryItem>) {
        items.forEach{
            Log.wtf("Got the items", it.toString())
        }

        historyItemAdapter.setItems(items)
        historyItemAdapter.notifyDataSetChanged()
    }

    fun updateDate(date: Date) {
        if (!datesEqual(date, selectedDate)) {
            selectedDate = date
            var dateLabel: TextView = findViewById(R.id.dateLabel)
            dateLabel.text = buildDateLabel(selectedDate)
            contentProvider.getHistoryData(buildDateURL(selectedDate), ::updateRecyclerView)
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
