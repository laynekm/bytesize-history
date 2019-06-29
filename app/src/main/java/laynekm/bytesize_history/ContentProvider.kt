package laynekm.bytesize_history

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

    private val TAG = "ContentProvider"
    private val API_BASE_URL = "https://en.wikipedia.org/w/api.php"
    private val WEB_BASE_URL = "https://en.wikipedia.org/wiki"

    var historyItems = mutableMapOf<Type, MutableList<HistoryItem>>(
        Type.EVENT to mutableListOf(),
        Type.BIRTH to mutableListOf(),
        Type.DEATH to mutableListOf()
    )

    var filteredHistoryItems = mutableMapOf<Type, MutableList<HistoryItem>>(
        Type.EVENT to mutableListOf(),
        Type.BIRTH to mutableListOf(),
        Type.DEATH to mutableListOf()
    )

    private var selectedDate: Date = getToday()
    private var selectedFilters = FilterOptions(Order.ASCENDING, mutableListOf(), mutableListOf())

    // Fetches history data, parses into lists, fetches their images, returns them to MainActivity
    fun fetchHistoryItems(
        date: Date,
        options: FilterOptions,
        type: Type?,
        updateRecyclerView: (MutableMap<Type, MutableList<HistoryItem>>) -> Unit,
        onFetchError: () -> Unit) {

        if (type === null) {
            updateRecyclerView(mutableMapOf(
                Type.EVENT to mutableListOf(),
                Type.BIRTH to mutableListOf(),
                Type.DEATH to mutableListOf()
            ))
        }

        doAsync {
            if (!datesEqual(selectedDate, date)) {
                for ((_, list) in historyItems) list.clear()
            }

            selectedDate = date
            selectedFilters = options.copy()

            // Fetch items and put into their respective lists (events, births, deaths)
            val url = buildURL(buildDateURL(date))
            var result = ""

            try {
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 1000
                val reader = BufferedReader(InputStreamReader(connection.inputStream))

                var str = reader.readLine()
                while (str != null) {
                    result += str
                    str = reader.readLine()
                }

                reader.close()
            } catch (e: Exception) {
                Log.e("ContentProvider", "$e")

                // Callback function that updates error messaging in main thread
                uiThread {
                    onFetchError()
                }
            }

            val allHistoryItems = parseContent(result)
            for ((type) in historyItems) {
                if (selectedFilters.types.contains(type)) {
                    historyItems[type] = filterType(allHistoryItems, type)
                    filteredHistoryItems[type] = filterErasAndSort(historyItems[type]!!)
                }
            }

            // Callback function that updates recycler views in main thread
            uiThread {
                updateRecyclerView(filteredHistoryItems)
            }
        }
    }

    fun filterHistoryItems(
        options: FilterOptions,
        updateRecyclerView: (MutableMap<Type, MutableList<HistoryItem>>) -> Unit) {

        if (selectedFilters.equals(options)) return
        selectedFilters = options.copy()

        for ((type) in historyItems) {
            filteredHistoryItems[type]  = filterErasAndSort(historyItems[type]!!)
        }

        updateRecyclerView(filteredHistoryItems)
    }

    private fun filterType(items: MutableList<HistoryItem>, type: Type): MutableList<HistoryItem> {
        var filteredItems: MutableList<HistoryItem> = mutableListOf()
        items.forEach { if (it.type === type) filteredItems.add(it) }
        return filteredItems
    }

    private fun filterErasAndSort(items: MutableList<HistoryItem>): MutableList<HistoryItem> {
        var filteredItems: MutableList<HistoryItem> = mutableListOf()
        items.forEach { if (selectedFilters.eras.contains(it.era)) filteredItems.add(it) }
        if (selectedFilters.order === Order.DESCENDING) filteredItems.reverse()
        return filteredItems
    }

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

    // Image URL needs to be an actual URL since readText() is called on it
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

    // Web URL needs to be a string since it's being passed into webView.loadUrl
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

        // Split array into events, births, and deaths
        // Only care about strings starting with an asterisk
        // Assumes events, births, deaths proceed each other
        var historyItems = mutableListOf<HistoryItem>()
        var type: Type? = null
        for (line in lines) {
            if (line.contains("==Events==")) type = Type.EVENT
            if (line.contains("==Births==")) type = Type.BIRTH
            if (line.contains("==Deaths==")) type = Type.DEATH
            if (line.contains("==Holidays and observances==")) break
            if (type != null && line.contains("*")) historyItems.add(buildHistoryItem(line, type))
        }

        return historyItems
    }

    // TODO: Add support for sublists (see June 1)
    private fun buildHistoryItem(line: String, type: Type): HistoryItem {
        val year = parseYear(line)
        val (desc, links) = parseDescriptionAndLinks(line)
        return HistoryItem(type, year, desc, links, "")
    }

    // Parse out unneeded chars and return integer representation of year
    // BC values will be negative
    private fun parseYear(line: String): Int {
        var yearSection = "";
        if (line.contains(" &ndash; ")) yearSection = line.substringBefore(" &ndash; ")
        else if (line.contains(" – ")) yearSection = line.substringBefore(" – ")
        if (yearSection === "") return 0

        if (yearSection.contains("(")) {
            var secondaryYear = yearSection.substringAfter("(").substringBefore(")")
            yearSection = yearSection.replace(secondaryYear, "")
        }
        var yearInt = Regex("[^0-9]").replace(yearSection, "").toInt()
        if (yearSection.contains("BC")) yearInt *= -1
        return yearInt
    }

    // Fetches image URL based on provided webpage links
    // Some pages might not have images, so keep fetching until one of them does
    fun fetchImage(links: MutableList<Link>): String {
        var image = ""
        links.forEach {
            image = parseImageURL(buildImageURL(it.title).readText())
            Log.d(TAG, "Fetched image $image")
            if (image !== "") return image
        }

        return image
    }

    // Parse line and return description and links
    private fun parseDescriptionAndLinks(line: String): ParseResult {
        var desc = line.substringAfter(" &ndash; ")
        var links = mutableListOf<Link>()

        // Loop until all square brackets are removed
        // Link text is on the left side, text to display is on the right
        while (desc.contains("[[")) {
            val innerText = desc.substringAfter("[[").substringBefore("]]")
            if (innerText.contains("|")) {
                val leftText = innerText.substringAfter("[[").substringBefore("|")
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
        // These links don't really  matter, only care about wikipedia links
        while (desc.contains("<ref")) {
            val innerText = desc.substringAfter("<ref").substringBefore("</ref>")
            desc = desc.replaceFirst(innerText, "")
            desc = desc.replaceFirst("<ref", "")
            desc = desc.replaceFirst("</ref>", "")
        }

        // TODO: Add italics ({{ should be <i> or something, not sure yet)
        while (desc.contains("{{")) {
            val innerText = desc.substringAfter("{{").substringBefore("}}")
            val parsedInnerText = innerText.replace("|", " ")
            desc = desc.replaceFirst(innerText, parsedInnerText)
            desc = desc.replaceFirst("{{", "")
            desc = desc.replaceFirst("}}", "")
        }

        // Remove remaining characters
        desc = desc.replace("|", "")
        desc = desc.replace("\\", "")

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
}