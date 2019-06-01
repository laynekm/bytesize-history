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
    private lateinit var secondaryProgressBar: ProgressBar
    private val contentProvider: ContentProvider = ContentProvider()
    private var filterOptions: FilterOptions = FilterOptions()
    private var selectedDate: Date = getToday()
    private var updating: Boolean = false
    private var endOfList: Boolean = false

    private val dateString = "selectedDate"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        progressBar = findViewById(R.id.progressBar)
        secondaryProgressBar = findViewById(R.id.secondaryProgressBar)

        if (savedInstanceState !== null) {
            selectedDate = stringToDate(savedInstanceState.getString(dateString))
        }

        initializeRecyclerView()
        var dateLabel: TextView = findViewById(R.id.dateLabel)
        dateLabel.text = buildDateLabel(selectedDate)
        progressBar.visibility = View.VISIBLE
        secondaryProgressBar.visibility = View.GONE
        getHistoryItems()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.changeNotification -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Populate recycler view with initial empty data
    private fun initializeRecyclerView() {
        val historyItemView: RecyclerView = findViewById(R.id.historyItems)
        historyItemAdapter = HistoryItemAdapter(this, ArrayList())
        historyItemView.adapter = historyItemAdapter
        historyItemView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (!recyclerView.canScrollVertically(1) && !updating && !endOfList) {
                    secondaryProgressBar.visibility = View.VISIBLE
                    getHistoryItems()
                }
            }
        })
        historyItemView.layoutManager = LinearLayoutManager(this)
    }

    // Functions for adding
    private fun getHistoryItems() {
        updating = true
        contentProvider.fetchHistoryItems(selectedDate, ::updateRecyclerView, filterOptions)
    }

    // Populate recycler view with fetched data and hide progress bar
    private fun updateRecyclerView(items: MutableList<HistoryItem>, lastItem: Boolean) {
        if (progressBar.visibility === View.VISIBLE) progressBar.visibility = View.GONE
        if (secondaryProgressBar.visibility === View.VISIBLE) secondaryProgressBar.visibility = View.GONE
        val historyItemView: RecyclerView = findViewById(R.id.historyItems)
        if (historyItemAdapter.itemCount === 0) historyItemView.smoothScrollBy(0, 250)
        historyItemAdapter.setItems(items)
        historyItemAdapter.notifyDataSetChanged()
        updating = false
        endOfList = lastItem

    }

    private fun updateDate(date: Date) {
        if (!datesEqual(date, selectedDate)) {
            selectedDate = date
            var dateLabel: TextView = findViewById(R.id.dateLabel)
            dateLabel.text = buildDateLabel(selectedDate)
            progressBar.visibility = View.VISIBLE
            getHistoryItems()
        }
    }

    fun showDatePickerDialog(view: View) {
        var date: Calendar = Calendar.getInstance()
        var selectedYear = date.get(Calendar.YEAR)
        var selectedMonth = selectedDate.month
        var selectedDay = selectedDate.day

        // TODO: Hide year label
        DatePickerDialog(this, DatePickerDialog.OnDateSetListener { _, _, month, day ->
            updateDate(Date(month, day))
        }, selectedYear, selectedMonth, selectedDay).show()
    }

    override fun onBackPressed() {
        val webView: WebView = findViewById(R.id.webView)
        if (webView.visibility === View.VISIBLE) {
            webView.visibility = View.GONE
            webView.loadUrl("about:blank")
        } else {
            super.onBackPressed()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState);
        outState.putString(dateString, dateToString(selectedDate))
    }
}
