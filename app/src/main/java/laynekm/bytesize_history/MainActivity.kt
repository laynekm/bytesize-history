package laynekm.bytesize_history

import android.app.DatePickerDialog
import android.graphics.Typeface
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
import kotlinx.android.synthetic.main.filter_dropdown.*
import java.util.*

class HistoryViews(var views: MutableMap<Type, RecyclerView>)
class HistoryAdapters(var adapters: MutableMap<Type, HistoryItemAdapter>)
class TextViewFilters(var filters: MutableMap<Type, TextView>)

class MainActivity : AppCompatActivity()  {

    private lateinit var historyViews: HistoryViews
    private lateinit var historyAdapters: HistoryAdapters
    private lateinit var textViewFilters: TextViewFilters
    private lateinit var dateLabel: TextView
    private lateinit var dropdownFilter: ImageView
    private lateinit var dropdownView: View
    private lateinit var progressBar: ProgressBar

    private val defaultOrder: Order = Order.ASCENDING
    private val defaultTypes: MutableList<Type> = mutableListOf(Type.EVENT, Type.BIRTH, Type.DEATH)
    private val defaultEras: MutableList<Era> = mutableListOf(Era.ANCIENT, Era.MEDIEVAL, Era.EARLYMODERN, Era.EIGHTEENS, Era.NINETEENS, Era.TWOTHOUSANDS)
    private var filterOptions: FilterOptions = FilterOptions(defaultOrder, defaultTypes, defaultEras)

    private val contentProvider: ContentProvider = ContentProvider()
    private var selectedDate: Date = getToday()
    private var selectedType: Type? = filterOptions.types[0]
    private var fetching: Boolean = false

    private val dateString = "selectedDate"

    // TODO: Set new date if app is loaded on a new day without being closed the day before
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        dateLabel = findViewById(R.id.dateLabel)
        dropdownFilter = findViewById(R.id.dropdownFilter)
        dropdownView = findViewById(R.id.dropdownView)
        progressBar = findViewById(R.id.progressBar)
        dropdownFilter.setOnClickListener { dropdownFilterOnClick() }

        if (savedInstanceState !== null) {
            selectedDate = stringToDate(savedInstanceState.getString(dateString))
        }

        dateLabel.text = buildDateLabel(selectedDate)
        progressBar.visibility = View.VISIBLE

        initializeRecyclerViews()
        fetchHistoryItems()
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

    // Create RecyclerViews and their adapters, initialized as empty
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

        textViewFilters = TextViewFilters(mutableMapOf(
            Type.EVENT to findViewById(R.id.eventBtn) as TextView,
            Type.BIRTH to findViewById(R.id.birthBtn) as TextView,
            Type.DEATH to findViewById(R.id.deathBtn) as TextView
        ))

        textViewFilters.filters[selectedType]!!.setTypeface(null, Typeface.BOLD)
        for ((type, textView) in textViewFilters.filters) {
            textView.setOnClickListener { setSelectedType(type) }
        }

        for ((type, adapter) in historyViews.views) {
            adapter.adapter = historyAdapters.adapters[type]
            adapter.layoutManager = LinearLayoutManager(this)
        }
    }

    // Fetches history items from content provider
    private fun fetchHistoryItems() {
        fetching = true
        progressBar.visibility = View.VISIBLE
        contentProvider.fetchHistoryItems(selectedDate, filterOptions, selectedType, ::updateRecyclerView)
    }

    // Filters history items without having to refetch
    private fun filterHistoryItems() {
        contentProvider.filterHistoryItems(filterOptions, ::updateRecyclerView)
    }

    // Callback function passed into fetchHistoryItems, updates views and other UI elements
    private fun updateRecyclerView(items: MutableMap<Type, MutableList<HistoryItem>>) {
        Log.wtf("updateRecyclerView", "$items")
        if (progressBar.visibility == View.VISIBLE) progressBar.visibility = View.GONE

        for ((type, adapter) in historyAdapters.adapters) {
            adapter.setItems(items[type]!!)
            adapter.notifyDataSetChanged()
        }

        fetching = false
    }

    // Updates date using value selected in calendar, refetches history items if date changed
    private fun updateDate(date: Date) {
        if (!datesEqual(date, selectedDate)) {
            selectedDate = date
            dateLabel.text = buildDateLabel(selectedDate)
            fetchHistoryItems()
        }
    }

    // Toggles dropdown filter, refetches history items when menu is closed if filters changed
    private fun dropdownFilterOnClick() {
        if (dropdownView.visibility == View.GONE) {
            dropdownView.visibility = View.VISIBLE
            filterOptions.setViewContent(dropdownView)
        }
        else {
            dropdownView.visibility = View.GONE
            val filtersChanged = filterOptions.setFilterOptions(dropdownView)
            if (filtersChanged) {
                if (!filterOptions.types.contains(selectedType)) {
                    if (filterOptions.types.size == 0) setSelectedType(null)
                    else setSelectedType(filterOptions.types[0])
                }
                updateTypeSelectors()
                filterHistoryItems()
            }
        }
    }

    // Shows and hides type selectors according to filter options
    private fun updateTypeSelectors() {
        for ((type, textView) in textViewFilters.filters) {
            if (filterOptions.types.contains(type)) textView.visibility = View.VISIBLE
            else textView.visibility = View.GONE
        }
    }

    // Sets current history item type and hides other views
    private fun setSelectedType(newType: Type?) {
        selectedType = newType
        if(historyAdapters.adapters[selectedType] != null && historyAdapters.adapters[selectedType]!!.itemCount == 0) {
            fetchHistoryItems()
        }

        for ((type, view) in historyViews.views) {
            if (type === selectedType) {
                view.visibility = View.VISIBLE
                textViewFilters.filters[type]!!.setTypeface(null, Typeface.BOLD)
            }
            else {
                textViewFilters.filters[type]!!.setTypeface(null, Typeface.NORMAL)
                view.visibility = View.GONE
            }
        }
    }

    // Displays DatePicker and handles calls updateDate with the new selected date
    fun showDatePickerDialog(view: View) {
        val date: Calendar = Calendar.getInstance()
        val selectedYear = date.get(Calendar.YEAR)
        val selectedMonth = selectedDate.month
        val selectedDay = selectedDate.day

        // TODO: Hide year label
        DatePickerDialog(this, DatePickerDialog.OnDateSetListener { _, _, month, day ->
            updateDate(Date(month, day))
        }, selectedYear, selectedMonth, selectedDay).show()
    }

    // If the WebView is open, back button should close the WebView; otherwise, it should function normally
    override fun onBackPressed() {
        val webView: WebView = findViewById(R.id.webView)
        if (webView.visibility == View.VISIBLE) {
            webView.visibility = View.GONE
            webView.loadUrl("about:blank")
        } else {
            super.onBackPressed()
        }
    }

    // Ensures state is consistent when activity is destroyed/recreated
    // TODO: Need to save a lot more than just the date
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState);
        outState.putString(dateString, dateToString(selectedDate))
    }
}
