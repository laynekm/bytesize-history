package laynekm.dailyhistory

import android.net.Uri
import android.util.Log
import com.google.gson.JsonParser
import org.jetbrains.anko.doAsync
import java.net.URL

class ContentProvider {

    // After fetching data, calls function in main thread that populates recycler view
    fun getHistoryData(populateRecyclerView: (MutableList<HistoryItem>) -> Unit) {
         doAsync {
            val url = buildURL("May_11")
            val result = url.readText()
            val historyItems = parseContent(result)
             populateRecyclerView(historyItems)
        }
    }

    // Example URL:
    // https://en.wikipedia.org/w/api.php/w/api.php?action=query&format=json&prop=revisions&titles=May_11&formatversion=2&rvprop=content&rvslots=main&rvlimit=1
    private fun buildURL(searchParam: String): URL {
        val uri: Uri = Uri.parse("https://en.wikipedia.org/w/api.php").buildUpon()
            .appendQueryParameter("action", "query")
            .appendQueryParameter("prop", "revisions")
            .appendQueryParameter("rvprop", "content")
            .appendQueryParameter("rvslots", "main")
            .appendQueryParameter("rvlimit", "1")
            .appendQueryParameter("format", "json")
            .appendQueryParameter("formatversion" , "2")
            .appendQueryParameter("titles", searchParam)
            .build()

        Log.wtf("URL", uri.toString())
        return URL(uri.toString())
    }

    // Builds HistoryItem objects from json string input
    // TODO: Add error handling in case content does not exist or API call fails
    private fun parseContent(json: String): MutableList<HistoryItem> {
        Log.wtf("json", json)

        // extract content property from json
        val content = JsonParser().parse(json)
            .asJsonObject.get("query")
            .asJsonObject.getAsJsonArray("pages").get(0)
            .asJsonObject.getAsJsonArray("revisions").get(0)
            .asJsonObject.get("slots")
            .asJsonObject.get("main")
            .asJsonObject.get("content")
            .toString()

        // content itself is not in json format but can be split into an array
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
        return HistoryItem(type, year, desc, links)
    }

    // Return year with numbers only
    private fun parseYear(line: String): String {
        val numsOnly = Regex("[^0-9]")
        return numsOnly.replace(line.substringBefore(" &ndash; "), "")
    }

    // Used to return desc and links from parseDescriptionAndLinks
    data class ParseResult(val desc: String, val links: MutableList<Link>)

    // TODO: Fetch link content as well as returning the links (or maybe in another function?)
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
                desc = desc.replaceFirst(leftText, "")
                links.add(Link(leftText))
            } else {
                links.add(Link(innerText))
            }
            desc = desc.replaceFirst("[[", "").replaceFirst("]]", "")
        }

        // Loop until all <ref> tags are removed
        // URLs are either proceeded by "url=" or directly after "<ref>/" (other cases?)
        while (desc.contains("<ref")) {
            val innerText = desc.substringAfter("<ref").substringBefore("</ref>")
            if (innerText.contains("url=")) {
                val linkText = innerText.substringAfter("url=").substringBefore(" ")
                links.add(Link(linkText))
            } else {
                val linkText = innerText.substringAfter("/")
                links.add(Link(linkText))
            }
            desc = desc.replaceFirst(innerText, "").replaceFirst("<ref", "").replaceFirst("</ref>", "")
        }

        // Remove remaining characters
        desc = desc.replace("|", "")

        return ParseResult(desc, links)
    }
}