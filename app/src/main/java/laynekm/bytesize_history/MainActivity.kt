package laynekm.bytesize_history

import android.app.DatePickerDialog
import android.graphics.Typeface
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
import android.support.v7.widget.Toolbar
import java.util.*
import android.widget.Button


class HistoryViews(var views: MutableMap<Type, RecyclerView>)
class HistoryAdapters(var adapters: MutableMap<Type, HistoryItemAdapter>)
class TextViewFilters(var filters: MutableMap<Type, TextView>)

class MainActivity : AppCompatActivity()  {

    private lateinit var toolbar: Toolbar
    private lateinit var historyViews: HistoryViews
    private lateinit var historyAdapters: HistoryAdapters
    private lateinit var textViewFilters: TextViewFilters
    private lateinit var dateLabel: TextView
    private lateinit var errorTextView: TextView
    private lateinit var retryBtn: Button
    private lateinit var dropdownFilter: ImageView
    private lateinit var dropdownView: View
    private lateinit var progressBar: ProgressBar
    private lateinit var webView: WebView

    private val defaultOrder: Order = Order.ASCENDING
    private val defaultTypes: MutableList<Type> = mutableListOf(Type.EVENT, Type.BIRTH, Type.DEATH)
    private val defaultEras: MutableList<Era> = mutableListOf(Era.ANCIENT, Era.MEDIEVAL, Era.EARLYMODERN, Era.EIGHTEENS, Era.NINETEENS, Era.TWOTHOUSANDS)
    private var filterOptions: FilterOptions = FilterOptions(defaultOrder, defaultTypes, defaultEras)

    private val contentProvider: ContentProvider = ContentProvider()
    private var selectedDate: Date = getToday()
    private var selectedType: Type? = filterOptions.types[0]
    private var fetching: Boolean = false

    private val dateKey = "selectedDate"

    // TODO: Set new date if app is loaded on a new day without being closed the day before
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        toolbar =  findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.navigationIcon = null

        initializeRecyclerViews()

        dateLabel = findViewById(R.id.dateLabel)
        errorTextView = findViewById(R.id.errorTextView)
        retryBtn = findViewById(R.id.retryBtn)
        dropdownView = findViewById(R.id.dropdownView)
        progressBar = findViewById(R.id.progressBar)
        dropdownFilter = findViewById(R.id.dropdownFilter)
        webView = findViewById(R.id.webView)
        retryBtn.setOnClickListener { fetchHistoryItems() }
        dropdownFilter.setOnClickListener { dropdownFilterOnClick() }
        toolbar.setNavigationOnClickListener { onBackPressed() }

        if (savedInstanceState !== null) {
            selectedDate = stringToDate(savedInstanceState.getString(dateKey))
        }

        dateLabel.text = buildDateLabel(selectedDate)
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
        retryBtn.visibility = View.GONE
        errorTextView.visibility = View.GONE
        progressBar.visibility = View.VISIBLE
        contentProvider.fetchHistoryItems(selectedDate, filterOptions, selectedType, ::updateRecyclerView, ::onFetchError)
    }

    // Filters history items without having to refetch
    private fun filterHistoryItems() {
        contentProvider.filterHistoryItems(filterOptions, ::updateRecyclerView)
    }

    // Callback function passed into fetchHistoryItems, updates views and other UI elements
    private fun updateRecyclerView(items: MutableMap<Type, MutableList<HistoryItem>>) {
        if (progressBar.visibility == View.VISIBLE) progressBar.visibility = View.GONE

        for ((type, adapter) in historyAdapters.adapters) {
            adapter.setItems(items[type]!!)
        }

        fetching = false
        checkFilterResults(selectedType)
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

        for ((type, view) in historyViews.views) {
            if (type === selectedType) {
                view.visibility = View.VISIBLE
                textViewFilters.filters[type]!!.setTypeface(null, Typeface.BOLD)
            } else {
                textViewFilters.filters[type]!!.setTypeface(null, Typeface.NORMAL)
                view.visibility = View.GONE
            }
        }

        checkFilterResults(selectedType)
    }

    // Displays DatePicker and handles calls updateDate with the new selected date
    fun showDatePickerDialog(view: View) {
        val date: Calendar = Calendar.getInstance()
        val selectedYear = date.get(Calendar.YEAR)
        val selectedMonth = selectedDate.month
        val selectedDay = selectedDate.day

        DatePickerDialog(this, DatePickerDialog.OnDateSetListener { _, _, month, day ->
            updateDate(Date(month, day))
        }, selectedYear, selectedMonth, selectedDay).show()
    }

    // If the WebView is open, back button should close the WebView; otherwise, it should function normally
    override fun onBackPressed() {
        if (webView.visibility == View.VISIBLE) {
            webView.visibility = View.GONE
            webView.loadUrl("about:blank")
            toolbar.navigationIcon = null
        } else {
            super.onBackPressed()
        }
    }

    private fun onFetchError() {
        progressBar.visibility = View.GONE
        retryBtn.visibility = View.VISIBLE
        errorTextView.visibility = View.VISIBLE
        errorTextView.setText(R.string.fetch_error)
    }

    private fun checkFilterResults(type: Type?) {
        if (type === null) {
            errorTextView.visibility = View.VISIBLE
            errorTextView.text = resources.getString(R.string.type_error)
        } else if (historyAdapters.adapters[type]!!.itemCount == 0) {
            errorTextView.visibility = View.VISIBLE
            errorTextView.text = resources.getString(R.string.filter_error, formatType(type))
        } else {
            errorTextView.visibility = View.GONE
        }
    }

    private fun formatType(type: Type): String {
        return type.toString().toLowerCase() + "s"
    }

    // Ensures date is consistent when activity is destroyed/recreated
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState);
        outState.putString(dateKey, dateToString(selectedDate))
    }
}
