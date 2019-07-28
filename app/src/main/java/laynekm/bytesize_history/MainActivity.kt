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
import android.content.Intent
import android.graphics.Bitmap
import android.webkit.WebViewClient
import android.widget.*

class HistoryViews(var views: HashMap<Type, RecyclerView>)
class HistoryAdapters(var adapters: HashMap<Type, HistoryItemAdapter>)
class TextViewFilters(var filters: HashMap<Type, TextView>)

class MainActivity : AppCompatActivity(), MainPresenter.View  {

    private lateinit var presenter: MainPresenter
    private lateinit var toolbar: Toolbar
    private lateinit var historyViews: HistoryViews
    private lateinit var historyAdapters: HistoryAdapters
    private lateinit var textViewFilters: TextViewFilters
    private lateinit var dateLabel: TextView
    private lateinit var errorTextView: TextView
    private lateinit var retryBtn: Button
    private lateinit var dropdownFilter: ImageView
    private lateinit var datePickerButton: ImageView
    private lateinit var dropdownView: View
    private lateinit var progressBar: ProgressBar
    private lateinit var webView: WebView
    private lateinit var datePickerDialog: DatePickerDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        presenter = MainPresenter(this, this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        toolbar = findViewById(R.id.toolbar)
        dateLabel = findViewById(R.id.dateLabel)
        errorTextView = findViewById(R.id.errorTextView)
        retryBtn = findViewById(R.id.retryBtn)
        dropdownView = findViewById(R.id.dropdownView)
        progressBar = findViewById(R.id.progressBar)
        dropdownFilter = findViewById(R.id.dropdownFilter)
        datePickerButton = findViewById(R.id.calendarView)

        webView = findViewById(R.id.webView)
        if (savedInstanceState !== null) webView.restoreState(savedInstanceState)
        webView.webViewClient = object: WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.visibility = View.GONE
            }
        }

        setSupportActionBar(toolbar)
        retryBtn.setOnClickListener { presenter.fetchHistoryItems() }
        dropdownFilter.setOnClickListener { presenter.toggleFilterDropdown(dropdownView) }
        datePickerButton.setOnClickListener { presenter.showDatePickerDialog() }
        toolbar.setNavigationOnClickListener { onBackPressed() }

        initializeRecyclerViews()
        initializeFilterViews()

        presenter.onViewCreated(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        presenter.onViewResumed()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        val themeMenuItem = menu.findItem(R.id.changeTheme)
        when (presenter.getTheme()) {
            Theme.LIGHT -> themeMenuItem.title = getString(R.string.theme_settings_light)
            Theme.DARK -> themeMenuItem.title = getString(R.string.theme_settings_dark)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.changeNotification -> {
                val notificationSettingsIntent = Intent(this@MainActivity, NotificationSettingsActivity::class.java)
                startActivity(notificationSettingsIntent)
                return true
            } R.id.changeTheme -> {
                presenter.toggleTheme()
                return true
            } else -> super.onOptionsItemSelected(item)
        }
    }

    // Get RecyclerViews and create their adapters, initialized as empty
    private fun initializeRecyclerViews() {
        historyViews = HistoryViews(hashMapOf(
            Type.EVENT to findViewById(R.id.eventItems),
            Type.BIRTH to findViewById(R.id.birthItems),
            Type.DEATH to findViewById(R.id.deathItems),
            Type.OBSERVANCE to findViewById((R.id.observanceItems))
        ))

        historyAdapters = HistoryAdapters(hashMapOf(
            Type.EVENT to HistoryItemAdapter(this, presenter, mutableListOf()),
            Type.BIRTH to HistoryItemAdapter(this, presenter, mutableListOf()),
            Type.DEATH to HistoryItemAdapter(this, presenter, mutableListOf()),
            Type.OBSERVANCE to HistoryItemAdapter(this, presenter, mutableListOf())
        ))

        for ((type, adapter) in historyViews.views) {
            adapter.adapter = historyAdapters.adapters[type]
            adapter.layoutManager = LinearLayoutManager(this)
        }
    }

    // Get filter TextViews and assign onClick handlers
    private fun initializeFilterViews() {
        textViewFilters = TextViewFilters(hashMapOf(
            Type.EVENT to findViewById(R.id.eventBtn) as TextView,
            Type.BIRTH to findViewById(R.id.birthBtn) as TextView,
            Type.DEATH to findViewById(R.id.deathBtn) as TextView,
            Type.OBSERVANCE to findViewById(R.id.observanceBtn) as TextView
        ))

        for ((type, textView) in textViewFilters.filters) {
            textView.setOnClickListener { presenter.setCurrentType(type) }
        }
    }

    override fun onFetchStarted() {
        progressBar.visibility = View.VISIBLE
        retryBtn.visibility = View.GONE
        errorTextView.visibility = View.GONE
    }

    override fun onFetchFinished() {
        progressBar.visibility = View.GONE
    }

    // If type is not null, update adapter of specified type; otherwise, update all adapters
    override fun onContentChanged(items: HashMap<Type, MutableList<HistoryItem>>, type: Type?) {
        if (type != null) {
            historyAdapters.adapters[type]!!.setItems(items[type]!!)
        } else {
            for ((actualType, adapter) in historyAdapters.adapters) {
                adapter.setItems(items[actualType]!!)
            }
        }
    }

    override fun onError(error: Error?, type: Type?) {
        errorTextView.visibility = View.VISIBLE

        when (error) {
            Error.FETCH_ERROR -> {
                retryBtn.visibility = View.VISIBLE
                errorTextView.text = getString(R.string.fetch_error)
            } Error.TYPE_ERROR -> {
                errorTextView.text = getString(R.string.type_error)
            } Error.FILTER_ERROR -> {
                errorTextView.text = getString(R.string.filter_error, typeToString(type!!))
            } else -> {
                errorTextView.visibility = View.GONE
            }
        }
    }

    override fun onDateChanged(date: Date) {
        dateLabel.text = buildDateForLabel(date)
    }

    override fun onFiltersChanged(filters: FilterOptions) {
        for ((type, textView) in textViewFilters.filters) {
            if (filters.types.contains(type)) textView.visibility = View.VISIBLE
            else textView.visibility = View.GONE
        }
    }

    override fun onDropdownOpened() {
        dropdownFilter.rotation = 180.toFloat()
        dropdownView.visibility = View.VISIBLE
        dropdownView.startAnimation(loadAnimation(this, R.anim.slide_down))
    }

    override fun onDropdownClosed() {
        dropdownFilter.rotation = 0.toFloat()
        dropdownView.startAnimation(loadAnimation(this, R.anim.slide_up))
        dropdownView.visibility = View.GONE
    }

    override fun onTypeChanged(newType: Type?) {
        for ((type, view) in historyViews.views) {
            if (type === newType) {
                view.visibility = View.VISIBLE
                textViewFilters.filters[type]!!.setTypeface(null, Typeface.BOLD)
                textViewFilters.filters[type]!!.setBackgroundResource(R.drawable.border_bottom)
            } else {
                view.visibility = View.INVISIBLE
                textViewFilters.filters[type]!!.setTypeface(null, Typeface.NORMAL)
                textViewFilters.filters[type]!!.setBackgroundResource(0)
            }
        }
    }

    override fun showRecyclerView(type: Type) {
        historyViews.views[type]!!.visibility = View.VISIBLE
    }

    override fun hideRecyclerView(type: Type) {
        historyViews.views[type]!!.visibility = View.GONE
    }

    override fun showDatePickerDialog(year: Int, month: Int, day: Int) {
        datePickerDialog = DatePickerDialog(this, DatePickerDialog.OnDateSetListener { _, _, m, d ->
            presenter.setCurrentDate(Date(m, d))
        }, year, month, day)
        datePickerDialog.setOnDismissListener { presenter.onCloseDatePickerDialog() }
        datePickerDialog.show()
    }

    override fun showWebView() {
        progressBar.visibility = View.VISIBLE
        webView.visibility = View.VISIBLE
        toolbar.setNavigationIcon(R.drawable.back_arrow)
    }

    override fun hideWebView() {
        webView.visibility = View.GONE
        webView.clearHistory()
        toolbar.navigationIcon = null
    }

    override fun goBackWebView() {
        webView.goBack()
    }

    override fun loadUrlToWebView(url: String) {
        webView.loadUrl(url)
    }

    override fun onBackPressed() {
        if (!presenter.onBackPressed(webView)) {
            super.onBackPressed()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        presenter.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun recreate() {
        super.recreate()
    }

    override fun onPause() {
        super.onPause()
        if (::datePickerDialog.isInitialized) datePickerDialog.dismiss()
    }
}
