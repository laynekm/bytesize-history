package laynekm.bytesize_history

import android.app.DatePickerDialog
import android.media.Image
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.WebView
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class HistoryViews(var views: MutableMap<Type, RecyclerView>)
class HistoryAdapters(var adapters: MutableMap<Type, HistoryItemAdapter>)

class MainActivity : AppCompatActivity()  {

    private lateinit var historyViews: HistoryViews
    private lateinit var historyAdapters: HistoryAdapters
    private lateinit var dropdownFilter: ImageView
    private lateinit var dropdownView: View
    private lateinit var eventFilter: TextView
    private lateinit var birthFilter: TextView
    private lateinit var deathFilter: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var secondaryProgressBar: ProgressBar

    private val defaultOrder: Order = Order.ASCENDING
    private val defaultTypes: MutableList<Type> = mutableListOf(Type.EVENT, Type.BIRTH, Type.DEATH)
    private val defaultEras: MutableList<Era> = mutableListOf(Era.ANCIENT, Era.MEDIEVAL, Era.EARLYMODERN, Era.EIGHTEENS, Era.NINETEENS, Era.TWOTHOUSANDS)

    private val contentProvider: ContentProvider = ContentProvider()
    private var filterOptions: FilterOptions = FilterOptions(defaultOrder, defaultTypes, defaultEras)
    private var selectedDate: Date = getToday()
    private var selectedType: Type = filterOptions.types[0]
    private var updating: Boolean = false
    private var shouldScroll: Boolean = false
    private var endOfList: Boolean = false

    private val dateString = "selectedDate"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        dropdownFilter = findViewById(R.id.dropdownFilter)
        dropdownView = findViewById(R.id.dropdownView)
        eventFilter = findViewById(R.id.eventBtn)
        birthFilter = findViewById(R.id.birthBtn)
        deathFilter = findViewById(R.id.deathBtn)
        progressBar = findViewById(R.id.progressBar)
        secondaryProgressBar = findViewById(R.id.secondaryProgressBar)

        dropdownFilter.setOnClickListener {
            if (dropdownView.visibility === View.GONE) {
                dropdownView.visibility = View.VISIBLE
                filterOptions.setViewContent(dropdownView)
            }
            else {
                dropdownView.visibility = View.GONE
                val filtersChanged = filterOptions.setFilterOptions(dropdownView)
                if (filtersChanged) {
                    progressBar.visibility = View.VISIBLE
                    if (!filterOptions.types.contains(selectedType)) setSelectedType(filterOptions.types[0])
                    updateTypeSelectors()
                    getHistoryItems()
                }
            }
        }

        eventFilter.setOnClickListener { setSelectedType(Type.EVENT) }
        birthFilter.setOnClickListener { setSelectedType(Type.BIRTH) }
        deathFilter.setOnClickListener { setSelectedType(Type.DEATH) }

        if (savedInstanceState !== null) {
            selectedDate = stringToDate(savedInstanceState.getString(dateString))
        }

        initializeRecyclerViews()
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
    private fun initializeRecyclerViews() {
        historyViews = HistoryViews(mutableMapOf(
            Type.EVENT to findViewById(R.id.eventItems),
            Type.BIRTH to findViewById(R.id.birthItems),
            Type.DEATH to findViewById(R.id.deathItems)
        ))

        historyAdapters = HistoryAdapters(mutableMapOf(
            Type.EVENT to HistoryItemAdapter(this, mutableListOf()),
            Type.BIRTH to HistoryItemAdapter(this, mutableListOf()),
            Type.DEATH to HistoryItemAdapter(this, mutableListOf())
        ))

        for ((type, adapter) in historyViews.views) {
            adapter.adapter = historyAdapters.adapters.get(type)
            adapter.layoutManager = LinearLayoutManager(this)
            adapter.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    if (!recyclerView.canScrollVertically(1) && !updating && !endOfList) {
                        secondaryProgressBar.visibility = View.VISIBLE
                        shouldScroll = true
                        getHistoryItems()
                    }
                }
            })
        }
    }

    // Functions for adding
    private fun getHistoryItems() {
        updating = true
        contentProvider.fetchHistoryItems(selectedDate, ::updateRecyclerView, filterOptions, selectedType)
    }

    // Populate recycler view with fetched data and hide progress bar
    private fun updateRecyclerView(items: MutableMap<Type, MutableList<HistoryItem>>, lastItem: Boolean) {
        if (progressBar.visibility === View.VISIBLE) progressBar.visibility = View.GONE
        if (secondaryProgressBar.visibility === View.VISIBLE) secondaryProgressBar.visibility = View.GONE

        for ((type, adapter) in historyAdapters.adapters) {
            if (shouldScroll) historyViews.views.get(type)!!.smoothScrollBy(0, 250)
            adapter.setItems(items.get(type)!!)
            adapter.notifyDataSetChanged()
            shouldScroll = false
        }

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

    private fun updateTypeSelectors() {
        if (filterOptions.types.contains(Type.EVENT)) eventFilter.visibility = View.VISIBLE
        else { eventFilter.visibility = View.GONE }
        if (filterOptions.types.contains(Type.BIRTH)) birthFilter.visibility = View.VISIBLE
        else { birthFilter.visibility = View.GONE }
        if (filterOptions.types.contains(Type.DEATH)) deathFilter.visibility = View.VISIBLE
        else { deathFilter.visibility = View.GONE }
    }

    private fun setSelectedType(type: Type) {
        selectedType = type
        getHistoryItems()
        when (selectedType) {
            Type.EVENT -> {
                findViewById<RecyclerView>(R.id.eventItems).visibility = View.VISIBLE
                findViewById<RecyclerView>(R.id.birthItems).visibility = View.GONE
                findViewById<RecyclerView>(R.id.deathItems).visibility = View.GONE
            }
            Type.BIRTH -> {
                findViewById<RecyclerView>(R.id.eventItems).visibility = View.GONE
                findViewById<RecyclerView>(R.id.birthItems).visibility = View.VISIBLE
                findViewById<RecyclerView>(R.id.deathItems).visibility = View.GONE
            }
            Type.DEATH -> {
                findViewById<RecyclerView>(R.id.eventItems).visibility = View.GONE
                findViewById<RecyclerView>(R.id.birthItems).visibility = View.GONE
                findViewById<RecyclerView>(R.id.deathItems).visibility = View.VISIBLE
            }
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
