package laynekm.bytesize_history

import android.net.Uri
import android.util.Log
import com.google.gson.JsonParser
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import java.net.URL

class ContentProvider {

    private val TAG = "ContentProvider"
    private val API_BASE_URL = "https://en.wikipedia.org/w/api.php"
    private val WEB_BASE_URL = "https://en.wikipedia.org/wiki"

    private var allHistoryItems: MutableList<HistoryItem> = ArrayList()
    private var currentHistoryItems: MutableList<HistoryItem> = ArrayList()
    private var selectedDate: Date = getToday()
    private var selectedFilters = FilterOptions(Order.ASCENDING, mutableListOf(), mutableListOf())
    private val count = 15
    private var index = 0

    // Used to return desc and links from parseDescriptionAndLinks
    data class ParseResult(val desc: String, val links: MutableList<Link>)

    // If there are no history items or a new date, fetch all history items and image URLs for the first count
    // Otherwise, fetch image URLs for the next count
    fun fetchHistoryItems(date: Date, updateRecyclerView: (MutableList<HistoryItem>, Boolean) -> Unit, options: FilterOptions) {
        doAsync {
            Log.wtf("selectedFilters", selectedFilters.eras.toString())
            if (!datesEqual(selectedDate, date) || !options.equals(selectedFilters)) {
                allHistoryItems.clear()
                currentHistoryItems.clear()
                index = 0
            }

            selectedDate = date
            selectedFilters = options.copy()

            if (index === 0) {
                val url = buildURL(buildDateURL(date))
                val result = url.readText()
                allHistoryItems = filterAndSort(parseContent(result), options)
                currentHistoryItems.addAll(getAvailableItems())
                index += count
                currentHistoryItems.forEach { it.image = fetchImage(it.links) }
                uiThread {
                    updateRecyclerView(currentHistoryItems, isLastItem())
                }
            } else {
                val historyItemChunk = getAvailableItems()
                index += count
                historyItemChunk.forEach { it.image = fetchImage(it.links) }
                currentHistoryItems.addAll(historyItemChunk)
                uiThread {
                    updateRecyclerView(currentHistoryItems, isLastItem())
                }
            }
        }
    }

    private fun filterAndSort(items: MutableList<HistoryItem>, options: FilterOptions): MutableList<HistoryItem> {
        items.retainAll { options.eras.contains(it.era) && options.types.contains(it.type) }
        return items
    }

    private fun getAvailableItems(): MutableList<HistoryItem> {
        if (allHistoryItems.size >= index + count) return allHistoryItems.subList(index, index + count)
        return allHistoryItems.subList(index, allHistoryItems.size)
    }

    private fun isLastItem(): Boolean {
        return allHistoryItems.size === currentHistoryItems.size
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
            .appendQueryParameter("pithumbsize", "100")
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
    // TODO: Add error handling in case content does not exist or API call fails
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
    private fun fetchImage(links: MutableList<Link>): String {
        var image = ""
        links.forEach {
            image = parseImageURL(buildImageURL(it.title).readText())
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