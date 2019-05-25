package laynekm.bytesize_history

import android.app.DatePickerDialog
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.WebView
import android.widget.ProgressBar
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*



class MainActivity : AppCompatActivity()  {

    private lateinit var historyItemAdapter: HistoryItemAdapter
    private lateinit var progressBar: ProgressBar
    private val contentProvider: ContentProvider = ContentProvider()
    private var selectedDate: Date = getToday()
    private var currentItems: MutableList<HistoryItem> = ArrayList()

    private val dateString = "selectedDate"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        progressBar = findViewById(R.id.progressBar)

        if (savedInstanceState !== null) {
            selectedDate = stringToDate(savedInstanceState.getString(dateString))
        }

        initializeRecyclerView(currentItems)
        var dateLabel: TextView = findViewById(R.id.dateLabel)
        dateLabel.text = buildDateLabel(selectedDate)
        getInitialHistoryItems()
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
    private fun initializeRecyclerView(items: MutableList<HistoryItem>) {
        val historyItemView: RecyclerView = findViewById(R.id.historyItems)
        historyItemAdapter = HistoryItemAdapter(this, items)
        historyItemView.adapter = historyItemAdapter
        historyItemView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (!recyclerView.canScrollVertically(1)) {
                    getNextHistoryItems()
                }
            }
        })
        historyItemView.layoutManager = LinearLayoutManager(this)
    }

    // Fetch data and show progress bar
    private fun getInitialHistoryItems() {
        progressBar.visibility = View.VISIBLE
        contentProvider.fetchHistoryItems(buildDateURL(selectedDate), ::setRecyclerViewItems)
    }

    // Populate recycler view with fetched data nad hide progress bar
    private fun setRecyclerViewItems(items: MutableList<HistoryItem>) {
        progressBar.visibility = View.GONE
        currentItems = items
        Log.wtf("setRecyclerViewItems", "${currentItems.size}")
        historyItemAdapter.setItems(currentItems)
        historyItemAdapter.notifyDataSetChanged()
    }

    // Functions for adding
    private fun getNextHistoryItems() {
        contentProvider.getNextHistoryItems(::addRecyclerViewItems)
    }

    private fun addRecyclerViewItems(items: MutableList<HistoryItem>) {
        currentItems.addAll(items)
        Log.wtf("addRecyclerViewItems", "${currentItems.size}")
        historyItemAdapter.setItems(currentItems)
        historyItemAdapter.notifyDataSetChanged()
    }

    private fun updateDate(date: Date) {
        if (!datesEqual(date, selectedDate)) {
            selectedDate = date
            var dateLabel: TextView = findViewById(R.id.dateLabel)
            dateLabel.text = buildDateLabel(selectedDate)
            getInitialHistoryItems()
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

    override fun onBackPressed() {
        val webView: WebView = findViewById(R.id.webView)
        if (webView.visibility === View.VISIBLE) {
            webView.visibility = View.GONE
        } else {
            super.onBackPressed()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState);
        outState.putString(dateString, dateToString(selectedDate))
    }
}
