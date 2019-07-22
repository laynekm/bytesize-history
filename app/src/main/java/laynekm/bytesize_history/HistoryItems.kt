package laynekm.bytesize_history

// Store history items in a singleton object so they are shared between activities and preserved onPause/onDestroy
object HistoryItems {
    var allHistoryItems = getEmptyTypeMap()
    var filteredHistoryItems = getEmptyTypeMap()
}