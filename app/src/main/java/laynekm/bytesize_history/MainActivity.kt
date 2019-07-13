package laynekm.bytesize_history

import android.app.DatePickerDialog
import android.graphics.Typeface
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.WebView
import android.support.v7.widget.Toolbar
import android.view.animation.AnimationUtils.loadAnimation
import java.util.Calendar
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import android.widget.*


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
    private lateinit var sharedPref: SharedPreferences

    private lateinit var filterManager: FilterManager
    private lateinit var notificationManager: NotificationManager
    private var filterOptions: FilterOptions = FilterOptions()
    private val contentProvider: ContentProvider = ContentProvider()

    private var selectedDate: Date = getToday()
    private var selectedType: Type? = filterOptions.types[0]
    private var fetching: Boolean = false

    private val dateKey = "selectedDate"

    // TODO: Set new date if app is loaded on a new day without being closed the day before
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState !== null) {
            selectedDate = stringToDate(savedInstanceState.getString(dateKey))
        }

        toolbar = findViewById(R.id.toolbar)
        dateLabel = findViewById(R.id.dateLabel)
        errorTextView = findViewById(R.id.errorTextView)
        retryBtn = findViewById(R.id.retryBtn)
        dropdownView = findViewById(R.id.dropdownView)
        progressBar = findViewById(R.id.progressBar)
        dropdownFilter = findViewById(R.id.dropdownFilter)
        webView = findViewById(R.id.webView)
        sharedPref = this.getSharedPreferences(getString(R.string.notification_pref_key), Context.MODE_PRIVATE)

        setSupportActionBar(toolbar)
        retryBtn.setOnClickListener { fetchHistoryItems() }
        dropdownFilter.setOnClickListener { dropdownFilterOnClick() }
        toolbar.setNavigationOnClickListener { onBackPressed() }

        filterManager = FilterManager(this)
        if (filterManager.hasPreferences()) filterOptions = filterManager.getPreferences()
        else filterManager.setPreferences(filterOptions)

        notificationManager = NotificationManager(this)
        notificationManager.initializePreferences()

        initializeRecyclerViews()
        initializeFilters()

        setSelectedType(selectedType)
        dateLabel.text = buildDateForLabel(selectedDate)

        fetchHistoryItems()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.changeNotification -> {
                val notificationSettingsIntent = Intent(this@MainActivity, NotificationSettingsActivity::class.java)
                startActivity(notificationSettingsIntent)
                return true
            }
            R.id.changeTheme -> {
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Get RecyclerViews and create their adapters, initialized as empty
    private fun initializeRecyclerViews() {
        historyViews = HistoryViews(mutableMapOf(
            Type.EVENT to findViewById(R.id.eventItems),
            Type.BIRTH to findViewById(R.id.birthItems),
            Type.DEATH to findViewById(R.id.deathItems),
            Type.OBSERVANCE to findViewById((R.id.observanceItems))
        ))

        historyAdapters = HistoryAdapters(mutableMapOf(
            Type.EVENT to HistoryItemAdapter(this, mutableListOf()),
            Type.BIRTH to HistoryItemAdapter(this, mutableListOf()),
            Type.DEATH to HistoryItemAdapter(this, mutableListOf()),
            Type.OBSERVANCE to HistoryItemAdapter(this, mutableListOf())
        ))

        for ((type, adapter) in historyViews.views) {
            adapter.adapter = historyAdapters.adapters[type]
            adapter.layoutManager = LinearLayoutManager(this)
        }
    }

    // Get filter TextViews and assign onClick handlers
    private fun initializeFilters() {
        textViewFilters = TextViewFilters(mutableMapOf(
            Type.EVENT to findViewById(R.id.eventBtn) as TextView,
            Type.BIRTH to findViewById(R.id.birthBtn) as TextView,
            Type.DEATH to findViewById(R.id.deathBtn) as TextView,
            Type.OBSERVANCE to findViewById(R.id.observanceBtn) as TextView
        ))

        for ((type, textView) in textViewFilters.filters) {
            textView.setOnClickListener { setSelectedType(type) }
        }

        updateTypeSelectors()
    }

    // Fetch history items from content provider
    private fun fetchHistoryItems() {
        fetching = true
        retryBtn.visibility = View.GONE
        errorTextView.visibility = View.GONE
        progressBar.visibility = View.VISIBLE
        contentProvider.fetchHistoryItems(selectedDate, filterOptions, ::updateRecyclerView, ::onFetchError)
    }

    // Filter history items without having to refetch
    private fun filterHistoryItems() {
        contentProvider.filterHistoryItems(filterOptions, ::updateRecyclerView)
    }

    // Callback function to update RecyclerViews and other UI elements
    private fun updateRecyclerView(items: MutableMap<Type, MutableList<HistoryItem>>) {
        fetching = false
        progressBar.visibility = View.GONE

        for ((type, adapter) in historyAdapters.adapters) {
            adapter.setItems(items[type]!!)
        }

        checkFilterResults(selectedType)
    }

    // Update date using value selected from calendar, clear and refetch history items if it changed
    private fun updateDate(date: Date) {
        if (!datesEqual(date, selectedDate)) {
            selectedDate = date
            dateLabel.text = buildDateForLabel(selectedDate)
            updateRecyclerView(getEmptyTypeMap())
            fetchHistoryItems()
        }
    }

    // Toggle dropdown filter visibility and handle updating filters
    private fun dropdownFilterOnClick() {
        if (dropdownView.visibility == View.GONE) {
            filterManager.setViewContent(dropdownView, filterOptions)
            dropdownFilter.rotation = 180.toFloat()
            dropdownView.visibility = View.VISIBLE
            dropdownView.startAnimation(loadAnimation(this, R.anim.slide_down))
        } else {
            dropdownFilter.rotation = 0.toFloat()
            dropdownView.startAnimation(loadAnimation(this, R.anim.slide_up))
            dropdownView.visibility = View.GONE

            val newFilters = filterManager.setFilterOptions(dropdownView)
            if (!newFilters.equals(filterOptions)) {
                filterOptions = newFilters
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
                textViewFilters.filters[type]!!.setBackgroundResource(R.drawable.border_bottom)
            } else {
                view.visibility = View.INVISIBLE
                textViewFilters.filters[type]!!.setTypeface(null, Typeface.NORMAL)
                textViewFilters.filters[type]!!.setBackgroundResource(0)
            }
        }

        // Case where selectedType is no longer in filterOptions, or no types are in filterOptions
        if (!filterOptions.types.contains(selectedType)) {
            if (filterOptions.types.size == 0) setSelectedType(null)
            else setSelectedType(filterOptions.types[0])
        }

        checkFilterResults(selectedType)
    }

    // Displays DatePicker and handles calls updateDate with the new selected date
    // TODO: Remove onClick from XML
    fun showDatePickerDialog(view: View) {
        val date: Calendar = Calendar.getInstance()
        val selectedYear = date.get(Calendar.YEAR)
        val selectedMonth = selectedDate.month
        val selectedDay = selectedDate.day

        DatePickerDialog(this, DatePickerDialog.OnDateSetListener { _, _, month, day ->
            updateDate(Date(month, day))
        }, selectedYear, selectedMonth, selectedDay).show()
    }

    // If  WebView is open, back button should close the WebView; otherwise, it should function normally
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
            errorTextView.text = getString(R.string.type_error)
        } else if (historyAdapters.adapters[type]!!.itemCount == 0) {
            errorTextView.visibility = View.VISIBLE
            errorTextView.text = getString(R.string.filter_error, mapTypeToLabel(type))
        } else {
            errorTextView.visibility = View.GONE
        }
    }

    // Ensures date is consistent when activity is destroyed/recreated
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState);
        outState.putString(dateKey, dateToString(selectedDate))
    }
}
