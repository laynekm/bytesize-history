package laynekm.bytesize_history

import android.content.Context
import android.os.Bundle
import java.util.Calendar

// TODO: Set new date if app is loaded on a new day without being closed the day before
class MainPresenter(val context: Context, val view: View) {

    private val contentManager: ContentManager = ContentManager()
    private val themeManager: ThemeManager = ThemeManager(context)
    private var filterManager: FilterManager = FilterManager(context)
    private var notificationManager: NotificationManager = NotificationManager(context)

    private var currentDate: Date = getToday()
    private var currentType: Type? = HistoryItems.filterOptions.types[0]
    private var fetching: Boolean = false
    private var fetchError: Boolean = false

    private val dateKey: String = "selectedDate"
    private val typeKey: String = "selectedType"

    init {
        themeManager.applyTheme()
        notificationManager.checkNotification()

        // Load filters from preferences or, if they don't exist, set default filters as preferences
        if (filterManager.hasPreferences()) HistoryItems.filterOptions = filterManager.getPreferences()
        else filterManager.setPreferences(HistoryItems.filterOptions)
    }

    fun onViewCreated(savedInstanceState: Bundle?) {
        if (savedInstanceState !== null) {
            currentDate = stringToDate(savedInstanceState.getString(dateKey))
            currentType = stringToType(savedInstanceState.getString(typeKey))
        }

        view.onTypeChanged(currentType)
        view.onDateChanged(currentDate)
        view.onFiltersChanged(HistoryItems.filterOptions)

        if (HistoryItems.isEmpty()) fetchHistoryItems()
        else view.onContentChanged(HistoryItems.filteredHistoryItems)
    }

    fun setCurrentType(type: Type?) {
        if (type === currentType) return
        currentType = type
        view.onTypeChanged(currentType)
    }

    fun setCurrentDate(date: Date) {
        if (datesEqual(date, currentDate)) return
        currentDate = date
        view.onDateChanged(currentDate)
        fetchHistoryItems()
    }

    fun fetchHistoryItems() {
        fetching = true
        fetchError = false
        view.onContentChanged(getEmptyTypeMap())
        view.onFetchStarted()
        contentManager.fetchHistoryItems(currentDate, ::fetchHistoryItemsCallback)
    }

    private fun fetchHistoryItemsCallback(success: Boolean) {
        fetching = false
        view.onFetchFinished()
        fetchError = !success
        if (success) view.onContentChanged(HistoryItems.filteredHistoryItems)
        checkForErrors()
    }

    fun toggleFilterDropdown(dropdownView: android.view.View) {
        if (dropdownView.visibility == android.view.View.GONE) {
            filterManager.setViewContent(dropdownView, HistoryItems.filterOptions)
            view.onDropdownOpened()
            return
        }

        view.onDropdownClosed()
        val newFilters = filterManager.setFilterOptions(dropdownView)
        if (!newFilters.equals(HistoryItems.filterOptions)) {
            HistoryItems.filterOptions = newFilters.copy()
            filterManager.setPreferences(HistoryItems.filterOptions)
            contentManager.filterHistoryItems()
            view.onContentChanged(HistoryItems.filteredHistoryItems)
            view.onFiltersChanged(HistoryItems.filterOptions)

            // If currentType is no longer in filterOptions, set currentType to first type or null
            if (!HistoryItems.filterOptions.types.contains(currentType)) {
                if (HistoryItems.filterOptions.types.size == 0) setCurrentType(null)
                else setCurrentType(HistoryItems.filterOptions.types[0])
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
        val year = Calendar.getInstance().get(Calendar.YEAR)
        val month = currentDate.month
        val day = currentDate.day
        view.showDatePickerDialog(year, month, day)
    }

    fun getTheme(): String = themeManager.getTheme()

    fun toggleTheme() {
        themeManager.toggleTheme()
        view.recreate()
    }

    fun onSaveInstanceState(outState: Bundle) {
        outState.putString(dateKey, dateToString(currentDate))
        outState.putString(typeKey, "$currentType")
    }

    interface View {
        fun onTypeChanged(type: Type?)
        fun onDateChanged(date: Date)
        fun onContentChanged(items: HashMap<Type, MutableList<HistoryItem>>)
        fun onFiltersChanged(filters: FilterOptions)
        fun onFetchStarted()
        fun onFetchFinished()
        fun onError(error: Error?, type: Type? = Type.EVENT)
        fun onDropdownOpened()
        fun onDropdownClosed()
        fun showDatePickerDialog(year: Int, month: Int, day: Int)
        fun recreate()
    }
}