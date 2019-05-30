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
    private val count = 15
    private var index = 0

    // Used to return desc and links from parseDescriptionAndLinks
    data class ParseResult(val desc: String, val links: MutableList<Link>)

    // If there are no history items or a new date, fetch all history items and image URLs for the first count
    // Otherwise, fetch image URLs for the next count
    fun fetchHistoryItems(newDate: Boolean, date: String, updateRecyclerView: (MutableList<HistoryItem>, Int) -> Unit) {
        Log.wtf("fetchHistoryItems", "index: $index, count: $count, newDate: $newDate, currentHistoryItems: ${currentHistoryItems.size},  allHistoryItems: ${allHistoryItems.size}")
        doAsync {
            if (index === 0 || newDate) {
                if (newDate) {
                    allHistoryItems.clear()
                    index = 0
                }
                Log.wtf("fetchHistoryItems", "if called")
                val url = buildURL(date)
                val result = url.readText()
                allHistoryItems = parseContent(result)
                currentHistoryItems = allHistoryItems.subList(index, index + count)
                index += count
                currentHistoryItems.forEach { it.image = fetchImage(it.links) }
                uiThread {
                    updateRecyclerView(currentHistoryItems, 0)
                }
            } else {
                Log.wtf("fetchHistoryItems", "else called")
                val historyItemChunk = allHistoryItems.subList(index, index + count)
                index += count
                historyItemChunk.forEach { it.image = fetchImage(it.links) }
                currentHistoryItems.addAll(historyItemChunk)
                uiThread {
                    updateRecyclerView(currentHistoryItems, 250)
                }
            }
        }
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
        Log.wtf("json", json)

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

    private fun buildHistoryItem(line: String, type: Type): HistoryItem {
        val year = parseYear(line)
        val (desc, links) = parseDescriptionAndLinks(line)
        return HistoryItem(type, year, desc, links, "")
    }

    // Return year with numbers only
    // TODO: Account for some including "BC"
    private fun parseYear(line: String): String {
        val numsOnly = Regex("[^0-9]")
        return numsOnly.replace(line.substringBefore(" &ndash; "), "")
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
    private fun parseDescriptionAndLinks(line: String): ParseResult {
        Log.wtf("Line", line)
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