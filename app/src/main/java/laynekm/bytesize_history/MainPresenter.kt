package laynekm.bytesize_history

import android.content.Context
import android.os.Bundle
import android.webkit.WebView
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.util.Calendar

/*
    Functions as the presenter for MainActivity
    - Retrieves data from various models/managers (Content, Theme, Filter, Notification)
    - Formats data and sends to View
    - Determines behaviour when user interacts with View
 */

class MainPresenter(val context: Context, val view: View) {

    private val contentManager: ContentManager = ContentManager()
    private val themeManager: ThemeManager = ThemeManager(context)
    private var filterManager: FilterManager = FilterManager(context)
    private var notificationManager: NotificationManager = NotificationManager(context)

    private var today: Date = getToday()
    private var currentDate: Date = getToday()
    private var currentType: Type? = null

    private var fetchError: Boolean = false
    private var filterDropdownVisible: Boolean = false
    private var datePickerVisible: Boolean = false
    private var webViewVisible: Boolean = false
    private val dateKey: String = "selectedDate"
    private val typeKey: String = "selectedType"
    private val filterDropdownVisibleKey: String = "filtersVisible"
    private val datePickerVisibleKey: String = "datePickerVisible"
    private val webViewVisibleKey: String = "webViewVisible"

    // Initializes managers, this will be called before MainActivity has content so can't update View here
    init {
        themeManager.applyTheme()
        notificationManager.checkNotification()

        // Load filters from preferences or, if they don't exist, set default filters as preferences
        if (filterManager.hasPreferences()) HistoryItems.filterOptions = filterManager.getPreferences()
        else filterManager.setPreferences(HistoryItems.filterOptions)
    }

    // Retrieve saved state and initialize values set the View's initial state
    fun onViewCreated(savedInstanceState: Bundle?) {
        if (savedInstanceState !== null) {
            currentDate = stringToDate(savedInstanceState.getString(dateKey))
            currentType = stringToType(savedInstanceState.getString(typeKey))
            filterDropdownVisible = savedInstanceState.getBoolean(filterDropdownVisibleKey)
            datePickerVisible = savedInstanceState.getBoolean(datePickerVisibleKey)
            webViewVisible = savedInstanceState.getBoolean(webViewVisibleKey)
        }

        if (currentType == null && HistoryItems.filterOptions.types.size > 0) {
            currentType = HistoryItems.filterOptions.types[0]
        }

        if (filterDropdownVisible) view.onDropdownOpened()
        if (datePickerVisible) showDatePickerDialog()
        if (webViewVisible) showWebView()

        view.onTypeChanged(currentType)
        view.onDateChanged(currentDate)
        view.onFiltersChanged(HistoryItems.filterOptions)

        if (HistoryItems.isEmpty()) fetchHistoryItems()
        else view.onContentChanged(HistoryItems.filteredHistoryItems)
    }

    // Update date if app is resumed but the date has changed
    fun onViewResumed() {
        val updatedToday = getToday()
        if (!datesEqual(today, updatedToday)) {
            today = updatedToday
            setCurrentDate(updatedToday, false)
        }
    }

    // Updates currentType, fetching images for that type if necessary
    fun setCurrentType(type: Type?) {
        if (type === currentType) return
        currentType = type
        view.onTypeChanged(currentType)
        if (type != null && !HistoryItems.allImagesFetched(type)) {
            fetchImages(type)
        }
        checkForErrors()
    }

    // Updates currentDate, will always trigger a fetch for history items on that date
    fun setCurrentDate(date: Date, updateDatePickerVisibility: Boolean = true) {
        if (updateDatePickerVisibility) onCloseDatePickerDialog()
        if (datesEqual(date, currentDate)) return
        currentDate = date
        view.onDateChanged(currentDate)
        fetchHistoryItems()
    }

    // Clears existing content and fetches history items for currentDate
    // uiThread is updated when the async call is finished
    fun fetchHistoryItems() {
        fetchError = false
        view.onContentChanged(getEmptyTypeMap())
        view.onFetchStarted()

        doAsync {
            val fetchedItems = contentManager.fetchHistoryItems(currentDate)
            uiThread { onFetchHistoryItemsFinished(fetchedItems != null) }
        }
    }

    private fun onFetchHistoryItemsFinished(success: Boolean) {
        fetchError = !success
        if (success && currentType != null) {
            fetchImages(currentType)
        } else {
            view.onFetchFinished()
        }

        checkForErrors()
    }

    // Fetches all images for given type, uiThread will not be updated until all images are fetched
    private fun fetchImages(type: Type?) {
        if (type == null) return

        view.onFetchStarted()
        view.hideRecyclerView(type)
        HistoryItems.filteredHistoryItems[type]!!.forEach {
            doAsync {
                val item = contentManager.fetchImage(it)
                uiThread { onFetchImagesFinished(item) }
            }
        }
    }

    private fun onFetchImagesFinished(item: HistoryItem) {
        if (HistoryItems.filteredHistoryItems[item.type]!!.all { it.hasFetchedImage }) {
            view.onContentChanged(HistoryItems.filteredHistoryItems, item.type)
            view.showRecyclerView(currentType!!)
            view.onFetchFinished()
        }
    }

    // Handles logic around opening/closing the filter dropdown menu
    // If dropdown is closed, may need to filter history items and fetch new images
    fun toggleFilterDropdown(dropdownView: android.view.View) {
        if (dropdownView.visibility == android.view.View.GONE) {
            filterManager.setViewContent(dropdownView, HistoryItems.filterOptions)
            filterDropdownVisible = true
            view.onDropdownOpened()
            return
        }

        filterDropdownVisible = false
        view.onDropdownClosed()
        val newFilters = filterManager.setFilterOptions(dropdownView)
        if (!newFilters.equals(HistoryItems.filterOptions)) {
            HistoryItems.filterOptions = newFilters.copy()
            filterManager.setPreferences(HistoryItems.filterOptions)
            contentManager.filterHistoryItems()
            view.onFiltersChanged(HistoryItems.filterOptions)

            // If currentType is no longer in filterOptions, set currentType to first type or null
            if (!HistoryItems.filterOptions.types.contains(currentType)) {
                if (HistoryItems.filterOptions.types.size == 0) setCurrentType(null)
                else setCurrentType(HistoryItems.filterOptions.types[0])
            } else {
                // Need to call fetchImages because the new items being displayed may not have had images fetched
                if (!HistoryItems.allImagesFetched(currentType)) fetchImages(currentType)
                else view.onContentChanged(HistoryItems.filteredHistoryItems, currentType)
            }

            // Update content for all other types, their images will be fetched once they become the current type
            for (type in HistoryItems.filterOptions.types) {
                view.onContentChanged(HistoryItems.filteredHistoryItems, type)
            }

            checkForErrors()
        }
    }

    private fun checkForErrors() {
        val currentTypeNull = currentType === null
        val currentTypeEmpty = currentType !== null && HistoryItems.filteredHistoryItems[currentType!!]!!.size == 0
        when {
            fetchError -> view.onError(Error.FETCH_ERROR)
            currentTypeNull -> view.onError(Error.TYPE_ERROR)
            currentTypeEmpty -> view.onError(Error.FILTER_ERROR, currentType)
            else -> view.onError(null)
        }
    }

    fun showDatePickerDialog() {
        datePickerVisible = true
        val year = Calendar.getInstance().get(Calendar.YEAR)
        val month = currentDate.month
        val day = currentDate.day
        view.showDatePickerDialog(year, month, day)
    }

    fun onCloseDatePickerDialog() {
        datePickerVisible = false
    }

    fun showWebView(url: String? = null) {
        webViewVisible = true
        view.showWebView()
        if (url != null) {
            view.loadUrlToWebView(url)
        }
    }

    // Call webView's canGoBack method if the previous url is not "about:blank"
    // Otherwise, hide the webView and load "about:blank"
    fun onBackPressed(webView: WebView): Boolean {
        val webViewList = webView.copyBackForwardList()
        val lastIndex = webViewList.getItemAtIndex(webViewList.currentIndex - 1)
        val lastUrl = when (lastIndex) {
            null -> ""
            else -> lastIndex.url
        }
        val canGoBack = lastIndex != null && lastUrl != "about:blank"

        if (webViewVisible && canGoBack) {
            view.goBackWebView()
            return true
        } else if (webViewVisible && !canGoBack) {
            webViewVisible = false
            view.hideWebView()
            view.loadUrlToWebView("about:blank")
            return true
        }

        return false
    }

    fun getTheme(): Theme = themeManager.getTheme()

    fun toggleTheme() {
        themeManager.toggleTheme()
        view.recreate()
    }

    fun onSaveInstanceState(outState: Bundle) {
        outState.putString(dateKey, dateToString(currentDate))
        outState.putString(typeKey, "$currentType")
        outState.putBoolean(filterDropdownVisibleKey, filterDropdownVisible)
        outState.putBoolean(datePickerVisibleKey, datePickerVisible)
        outState.putBoolean(webViewVisibleKey, webViewVisible)
    }

    interface View {
        fun onTypeChanged(type: Type?)
        fun onDateChanged(date: Date)
        fun onContentChanged(items: HashMap<Type, MutableList<HistoryItem>>, type: Type?)
        fun onContentChanged(items: HashMap<Type, MutableList<HistoryItem>>)
        fun onFiltersChanged(filters: FilterOptions)
        fun onFetchStarted()
        fun onFetchFinished()
        fun onError(error: Error?, type: Type? = Type.EVENT)
        fun onDropdownOpened()
        fun onDropdownClosed()
        fun showDatePickerDialog(year: Int, month: Int, day: Int)
        fun showWebView()
        fun hideWebView()
        fun showRecyclerView(type: Type)
        fun hideRecyclerView(type: Type)
        fun loadUrlToWebView(url: String)
        fun goBackWebView()
        fun recreate()
    }
}