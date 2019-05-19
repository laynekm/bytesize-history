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
import java.util.*

class MainActivity : AppCompatActivity()  {

    private val contentProvider: ContentProvider = ContentProvider()
    private lateinit var historyItemAdapter: HistoryItemAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        updateDate(getToday())

        // Populate recycler view with empty list to initialize
        val item = HistoryItem(Type.EVENT, "1900", "Test", "", mutableListOf(Link("test")))
        val items = mutableListOf(item)
        populateRecyclerView(items)

        // Fetch history data from API and update recycler view
        contentProvider.getHistoryData(buildDateURL(getToday()), ::updateRecyclerView)
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
        var dateLabel: TextView = findViewById(R.id.dateLabel)
        dateLabel.text = buildDateLabel(date)
    }

    fun showDatePickerDialog(view: View) {

        var date: Calendar = Calendar.getInstance()
        var thisAYear = date.get(Calendar.YEAR).toInt()
        var thisAMonth = date.get(Calendar.MONTH).toInt()
        var thisADay = date.get(Calendar.DAY_OF_MONTH).toInt()

        val dpd = DatePickerDialog(this, DatePickerDialog.OnDateSetListener { view2, thisYear, thisMonth, thisDay ->
            // Display Selected date in textbox
            thisAMonth = thisMonth
            thisADay = thisDay
            thisAYear = thisYear

            val newDate: Calendar = Calendar.getInstance()
            newDate.set(thisYear, thisMonth, thisDay)

            var dateLabel: TextView = findViewById(R.id.dateLabel)
            dateLabel.text = buildDateLabel(Date(thisAMonth, thisADay))


        }, thisAYear, thisAMonth, thisADay)

        dpd.show()
    }
}
