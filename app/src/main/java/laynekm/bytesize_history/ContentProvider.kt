package laynekm.bytesize_history

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.gson.JsonParser
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.net.URL
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection

// Used to return desc and links from parseDescriptionAndLinks
data class ParseResult(val desc: String, val links: MutableList<Link>)

class ContentProvider {

    private val TAG = "BHContentProvider"
    private val API_BASE_URL = "https://en.wikipedia.org/w/api.php"
    private val WEB_BASE_URL = "https://en.wikipedia.org/wiki"

    private var historyItems = getEmptyTypeMap()
    private var filteredHistoryItems = getEmptyTypeMap()
    private var selectedFilters = FilterOptions()

    private fun connectToURL(url: URL): String {
        var result = ""
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 10000
        val reader = BufferedReader(InputStreamReader(connection.inputStream))

        var str = reader.readLine()
        while (str != null) {
            result += str
            str = reader.readLine()
        }

        reader.close()
        return result
    }

    // Fetches history data, parses into lists, fetches their images, returns them to MainActivity
    // TODO: Fetch images for each type when tab is selected so initial load time isn't as long
    // TODO: Figure out why notifications often send 1-10 minutes late
    // TODO: Fix some items not appearing in recycler views? June 30, 1688 and 1864
    // TODO: July 11: Date appears as 911911
    // TODO: Fix July 9, 1730 abbr
    // TODO: Fix observances not appearing sometimes
    fun fetchHistoryItems(
        date: Date,
        filters: FilterOptions,
        updateRecyclerView: (MutableMap<Type, MutableList<HistoryItem>>) -> Unit,
        onFetchError: () -> Unit) {

        this.selectedFilters = filters.copy()
        val url = buildURL(buildDateForURL(date))
        var result = ""

        // Fetch items and put into their respective lists (events, births, deaths)
        doAsync {
            try {
                result = connectToURL(url)
            } catch (e: Exception) {
                Log.e("ContentProvider", "$e")
                uiThread { onFetchError() }
                return@doAsync
            }

            val allHistoryItems = parseContent(result)
            for ((type) in historyItems) {
                if (selectedFilters.types.contains(type)) {
                    historyItems[type] = filterType(allHistoryItems, type)
                    filteredHistoryItems[type] = filterErasAndSort(historyItems[type]!!)
                }
            }

            // Callback function that updates recycler views in main thread
            uiThread { updateRecyclerView(filteredHistoryItems) }
        }
    }

    // Returns a single history event for the user's daily notification
    fun fetchDailyHistoryFact(context: Context, pushNotification: (Context, HistoryItem, Date) -> Unit) {
        val date = getToday()
        val url = buildURL(buildDateForURL(date))
        var result = ""
        doAsync {
            result = connectToURL(url)
            val allHistoryItems = parseContent(result)
            val randomHistoryItem = getRandomHistoryItem(allHistoryItems, Type.EVENT)
            uiThread { pushNotification(context, randomHistoryItem, date) }
        }
    }

    // Returns random history item of the specified type
    private fun getRandomHistoryItem(items: MutableList<HistoryItem>, type: Type): HistoryItem {
        val filteredItems = filterType(items, type)
        val randomIndex = (0 until filteredItems.size - 1).random()
        return filteredItems[randomIndex]
    }

    fun filterHistoryItems(
        filters: FilterOptions,
        updateRecyclerView: (MutableMap<Type, MutableList<HistoryItem>>) -> Unit) {

        if (selectedFilters.equals(filters)) return
        selectedFilters = filters.copy()

        for ((type) in historyItems) {
            filteredHistoryItems[type] = filterErasAndSort(historyItems[type]!!)
        }

        updateRecyclerView(filteredHistoryItems)
    }

    private fun filterType(items: MutableList<HistoryItem>, type: Type): MutableList<HistoryItem> {
        val filteredItems: MutableList<HistoryItem> = mutableListOf()
        items.forEach { if (it.type === type) filteredItems.add(it) }
        return filteredItems
    }

    private fun filterErasAndSort(items: MutableList<HistoryItem>): MutableList<HistoryItem> {
        val filteredItems: MutableList<HistoryItem> = mutableListOf()
        items.forEach { if (selectedFilters.eras.contains(it.era)) filteredItems.add(it) }
        if (selectedFilters.order === Order.DESCENDING) filteredItems.reverse()
        return filteredItems
    }

    // Builds URL for the initial API call to Wikipedia
    private fun buildURL(searchParam: String): URL {
        val uri: Uri = Uri.parse(API_BASE_URL).buildUpon()
            .appendQueryParameter("action", "query")
            .appendQueryParameter("prop", "revisions")
            .appendQueryParameter("rvprop", "content")
            .appendQueryParameter("rvslots", "main")
            .appendQueryParameter("rvlimit", "1")
            .appendQueryParameter("format", "json")
            .appendQueryParameter("formatversion", "2")
            .appendQueryParameter("titles", searchParam)
            .build()

        Log.d(TAG, "buildURL: $uri")
        return URL(uri.toString())
    }

    // Builds URL to get images for each history item
    private fun buildImageURL(searchParam: String): URL {
        val uri: Uri = Uri.parse(API_BASE_URL).buildUpon()
            .appendQueryParameter("action", "query")
            .appendQueryParameter("prop", "pageimages")
            .appendQueryParameter("pithumbsize", "200")
            .appendQueryParameter("format", "json")
            .appendQueryParameter("formatversion", "2")
            .appendQueryParameter("titles", searchParam)
            .build()

        Log.d(TAG, "buildImageURL: $uri")
        return URL(uri.toString())
    }

    // Build URL for each Wikipedia link item, needs to be string since it's passed into WebView.loadUrl
    private fun buildWebURL(searchParam: String): String {
        val uri: Uri = Uri.parse(WEB_BASE_URL).buildUpon()
            .appendPath(searchParam)
            .build()

        Log.d(TAG, "buildWebURL: $uri")
        return uri.toString()
    }

    // Builds HistoryItem objects from json string input
    private fun parseContent(json: String): MutableList<HistoryItem> {

        // Extract content property from json
        val content = JsonParser().parse(json)
            .asJsonObject.get("query")
            .asJsonObject.getAsJsonArray("pages").get(0)
            .asJsonObject.getAsJsonArray("revisions").get(0)
            .asJsonObject.get("slots")
            .asJsonObject.get("main")
            .asJsonObject.get("content")
            .toString()

        // Content itself is not in json format but can be split into an array
        val lines = content.split("\\n").toTypedArray()

        lines.forEach {
            Log.wtf(TAG, it)
        }

        // Split array into events, births, and deaths; assumes this order is respected
        // Only care about strings starting with an asterisk, sublists indicated by multiple asterisks
        // TODO: Change to when statement
        val historyItems = mutableListOf<HistoryItem>()
        var type: Type? = null
        for (line in lines) {
            if (line.contains("==Events==")) type = Type.EVENT
            if (line.contains("==Births==")) type = Type.BIRTH
            if (line.contains("==Deaths==")) type = Type.DEATH
            if (line.contains("==Holidays and observances==")) type = Type.OBSERVANCE
            if (line.contains("==References==")) break
            if (type != null && line.contains("*")) {
                historyItems.add(buildHistoryItem(line, type))
            }
        }

        return historyItems
    }

    private fun buildHistoryItem(line: String, type: Type): HistoryItem {
        val year = parseYear(line, type)
        val depth = parseDepth(line)
        val (desc, links) = parseDescriptionAndLinks(line, type)
//        Log.wtf(TAG, "$desc")
        val historyItem = HistoryItem(type, year, desc, links, depth)
        Log.wtf(TAG, "$year - $desc")
        return historyItem
    }

    // Parse out unneeded chars and return integer representation of year (BC will be negative)
    // TODO: Properly assign year to sublists
    private fun parseYear(line: String, type: Type): Int? {
        if (type === Type.OBSERVANCE) return null

        var yearSection = ""
        if (line.contains("&ndash;")) yearSection = line.substringBefore("&ndash;")
        else if (line.contains(" – ")) yearSection = line.substringBefore(" – ")
        if (yearSection == "") return null

        if (yearSection.contains("(")) {
            val secondaryYear = yearSection.substringBetween("(", ")")
            yearSection = yearSection.replace(secondaryYear, "")
        }

        var yearInt = Regex("[^0-9]").replace(yearSection, "").toInt()
        if (yearSection.contains("BC")) yearInt *= -1
        return yearInt
    }

    // Depth of the item is determined by the number of asterisks (ie. one item might have its own list)
    // Subtract 1 because root depth should be 0
    private fun parseDepth(line: String): Int {
        var depth = 0
        line.forEach { if (it == '*') depth++ }
        return --depth
    }

    // Fetches image URL based on provided webpage links
    // Some pages might not have images, so keep fetching until one of them does
    fun fetchImage(links: MutableList<Link>): String {
        var image = ""
        links.forEach {
            image = parseImageURL(buildImageURL(it.title).readText())
            if (image !== "") return image
        }

        return image
    }

    // Parse line and return description and links
    private fun parseDescriptionAndLinks(line: String, type: Type): ParseResult {
        var desc = line
        if (type !== Type.OBSERVANCE) desc = line.substringAfter("&ndash; ")
        val links = mutableListOf<Link>()

        // Loop until all square brackets are removed
        // Link text is on the left side, text to display is on the right
        while (desc.contains("[[")) {
            val innerText = desc.substringBetween("[[", "]]")
            if (innerText.contains("|")) {
                val leftText = innerText.substringBetween("[[", "|")
                val linkTitle = formatText(leftText)
                val linkURL = buildWebURL(leftText)
                links.add(Link(linkTitle, linkURL))
                desc = desc.replaceFirst(leftText, "")
            } else {
                val linkTitle = formatText(innerText)
                val linkURL = buildWebURL(innerText)
                links.add(Link(linkTitle, linkURL))
            }
            desc = desc.replaceFirst("[[", "").replaceFirst("]]", "")
        }

        // Loop until all <ref> tags are removed
        // These links don't really  matter, only care about Wikipedia links so don't bother preserving them
        while (desc.contains("<ref")) {
            val innerText = desc.substringBetween("<ref", "</ref>")
            desc = desc.replaceFirst(innerText, "")
            desc = desc.replaceFirst("<ref", "")
            desc = desc.replaceFirst("</ref>", "")
        }

        // TODO: Add italics ({{ should be <i> or something, not sure yet)
        while (desc.contains("{{")) {
            val innerText = desc.substringBetween("{{", "}}")
            val parsedInnerText = innerText.replace("|", " ")
            desc = desc.replaceFirst(innerText, parsedInnerText)
            desc = desc.replaceFirst("{{", "")
            desc = desc.replaceFirst("}}", "")
        }

        // Remove remaining characters
        desc = desc.replace("|", "")
        desc = desc.replace("\\", "")

        if (desc.contains("*")) {
            desc = desc.replace("*", "")
            if (type !== Type.OBSERVANCE) desc = desc.replaceFirst(" ", "")
        }

        return ParseResult(desc, links)
    }

    // Parses result of API call that has image URL embedded in it
    private fun parseImageURL(json: String): String {
        var url = ""
        try {
            url = JsonParser().parse(json)
                .asJsonObject.get("query")
                .asJsonObject.getAsJsonArray("pages").get(0)
                .asJsonObject.get("thumbnail")
                .asJsonObject.get("source")
                .toString()

            // Remove extra quotes
            url = url.substring(1, url.length - 1)
        } catch (e: Exception) {
            Log.e("ContentProvider", "parseImageURL error")
        }

        return url
    }

    // Capitalizes the first letter if it isn't already
    private fun formatText(text: String): String {
        return text.capitalize()
    }

    private fun String.substringBetween(str1: String, str2: String): String
            = this.substringAfter(str1).substringBefore(str2)
}