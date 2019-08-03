package laynekm.bytesize_history

import android.util.Log
import com.google.gson.JsonParser
import java.net.URL
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection

// Store HistoryItems in singleton object so they are shared between activities and preserved through lifecycle
object HistoryItems {
    var allHistoryItems = getEmptyTypeMap()
    var filteredHistoryItems = getEmptyTypeMap()
    var filterOptions: FilterOptions = FilterOptions()
    var fetchedTypes: MutableSet<Type> = mutableSetOf()

    fun isEmpty(): Boolean {
        return mapIsEmpty(allHistoryItems) && mapIsEmpty(filteredHistoryItems)
    }
}

// Handles the fetching, parsing, and filtering of data into HistoryItems
class ContentManager {

    private val TAG = "ContentManager"
    private val API_BASE_URL = "https://en.wikipedia.org/w/api.php"
    private val WEB_BASE_URL = "https://en.wikipedia.org/wiki"

    // All the possible values that can be used to split year and desc
    // Some of these are edge cases found in literally one improperly formatted date, most use "&ndash;"
    private val yearDescSeparators = listOf("&ndash;", "&#x2013;", "{{snd}}", " – ", " - ", ": ", "[[1927|–]]")

    // Indicators of when lists of different history item types begin
    private val eventIndicators = listOf("==Events==", "== Events ==")
    private val birthIndicators = listOf("==Births==", "== Births ==")
    private val deathIndicators = listOf("==Deaths==", "== Deaths ==")
    private val observationIndicators = listOf("==Holidays and observances==", "== Holidays and observances ==")
    private val endOfContentIndicators = listOf(
        "==References==", "== References ==",
        "==External links==", "== External links ==",
        "==See also==", "== See also ==")

    // Fetches data and returns data from given endpoint, should only be called in async thread
    private fun fetchFromURL(url: URL): String {
        Log.d(TAG, "Connecting to $url")
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

    // Fetches data for given date, parses the data, returns filtered lists
    fun fetchHistoryItems(date: Date): HashMap<Type, MutableList<HistoryItem>>? {
        val url = buildURL(buildDateForURL(date))
        val result: String

        try {
            result = fetchFromURL(url)
        } catch (e: Exception) {
            Log.e(TAG, "$e")
            return null
        }

        val allHistoryItems = parseContent(result, date)
        for ((type) in HistoryItems.allHistoryItems) {
            HistoryItems.allHistoryItems[type] = filterType(allHistoryItems, type)
            HistoryItems.filteredHistoryItems[type] = filterErasAndSort(HistoryItems.allHistoryItems[type]!!)
        }

        return HistoryItems.filteredHistoryItems
    }

    // Fetches history items as above but returns a locally generated map instead of reassigning the global ones
    // Used in ContentManagerTest to make sure content for every day of year is fetched and parsed correctly
    fun fetchHistoryItemsTest(date: Date): HashMap<Type, MutableList<HistoryItem>>? {
        val url = buildURL(buildDateForURL(date))
        val result: String
        try {
           result = fetchFromURL(url)
        } catch (e: Exception) {
            return null
        }

        val allHistoryItems = parseContent(result, date)
        val mappedHistoryItems = getEmptyTypeMap()
        for ((type) in mappedHistoryItems) {
            mappedHistoryItems[type] = filterType(allHistoryItems, type)
        }

        return mappedHistoryItems
    }

    // Returns a single history event for the user's daily notification
    fun fetchDailyHistoryFact(): HistoryItem {
        val date = getToday()
        val url = buildURL(buildDateForURL(date))
        val result = fetchFromURL(url)
        val allHistoryItems = parseContent(result, date)
        return getRandomHistoryItem(allHistoryItems, Type.EVENT)
    }

    // Fetches image links from list of related facts
    // Some pages may not have images to keep fetching until one hopefully does
    // Note that item.image is assigned a direct URL associated with an image, not the image itself
    fun fetchImage(item: HistoryItem): HistoryItem {
        var imageURL = ""
        try {
            for (link in item.links) {
                imageURL = parseImageURL(fetchFromURL(buildImageURL(link.title)))
                if (imageURL != "") break
            }
        } catch (e: Exception) {
            Log.e(TAG, "$e")
            return item
        }

        item.image = imageURL
        item.hasFetchedImage = true
        return item
    }

    // Returns random history item of the specified type
    private fun getRandomHistoryItem(items: MutableList<HistoryItem>, type: Type): HistoryItem {
        val filteredItems = filterType(items, type)
        val randomIndex = (0 until filteredItems.size - 1).random()
        return filteredItems[randomIndex]
    }

    fun filterHistoryItems() {
        Log.d(TAG, "Filtering history items")
        for ((type) in HistoryItems.allHistoryItems) {
            HistoryItems.filteredHistoryItems[type] = filterErasAndSort(HistoryItems.allHistoryItems[type]!!)
        }
    }

    private fun filterType(items: MutableList<HistoryItem>, type: Type): MutableList<HistoryItem> {
        val filteredItems: MutableList<HistoryItem> = mutableListOf()
        items.forEach { if (it.type === type) filteredItems.add(it) }
        return filteredItems
    }

    private fun filterErasAndSort(items: MutableList<HistoryItem>): MutableList<HistoryItem> {
        var filteredItems: MutableList<HistoryItem> = mutableListOf()
        items.forEach {
            if (HistoryItems.filterOptions.eras.contains(it.era) || it.era === Era.NONE) filteredItems.add(it)
        }
        if (HistoryItems.filterOptions.order === Order.DESCENDING) {
            filteredItems = reverseOrderRespectingDepth(filteredItems)
        }
        return filteredItems
    }

    // Reverses order of items while making sure items of depth > 0 still appear beneath their parent
    private fun reverseOrderRespectingDepth(items: MutableList<HistoryItem>): MutableList<HistoryItem> {
        val reversedItems: MutableList<HistoryItem> = mutableListOf()
        items.forEach { reversedItems.add(it.depth, it) }
        return reversedItems
    }

    // Builds URL for the initial call to Wikipedia
    private fun buildURL(searchParam: String): URL {
        var urlString: String = API_BASE_URL
        urlString += "?action=query"
        urlString += "&prop=revisions"
        urlString += "&rvprop=content"
        urlString += "&rvslots=main"
        urlString += "&rvlimit=1"
        urlString += "&format=json"
        urlString += "&formatversion=2"
        urlString += "&titles=$searchParam"

        Log.d(TAG, "Built initial Wikipedia URL: $urlString")
        return URL(urlString)
    }

    // Builds URL to get the image link corresponding to each Wikipedia page
    private fun buildImageURL(searchParam: String): URL {
        var urlString: String = API_BASE_URL
        urlString += "?action=query"
        urlString += "&prop=pageimages"
        urlString += "&pithumbsize=200"
        urlString += "&format=json"
        urlString += "&formatversion=2"
        urlString += "&titles=$searchParam"

        Log.d(TAG, "Built image URL: $urlString")
        return URL(urlString)
    }

    // Builds URL for each Wikipedia link that appears in the dropdown menu of related facts
    // Needs to be a string since it's passed into WebView.loadUrl
    private fun buildWebURL(searchParam: String): String {
        var urlString: String = WEB_BASE_URL
        urlString += "/$searchParam"

        Log.d(TAG, "Built additional link URL: $urlString")
        return urlString
    }

    // Builds HistoryItem objects from json string input
    private fun parseContent(json: String, date: Date): MutableList<HistoryItem> {

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

        Log.d(TAG, "Parsed content for $json")
        lines.forEach { Log.d(TAG, it) }

        // Split array into events, births, and deaths; assumes this order is respected
        // Only care about strings starting with an asterisk, sublists are indicated by multiple asterisks
        // Set eventAdded after events added because some dates have second events list that's better suited as holidays/observations
        val historyItems = mutableListOf<HistoryItem>()
        var type: Type? = null
        var eventsAdded = false
        for (line in lines) {
            if (line.containsAny(eventIndicators) && !eventsAdded) type = Type.EVENT
            if (line.containsAny(birthIndicators)) {
                type = Type.BIRTH
                eventsAdded = true
            }
            if (line.containsAny(deathIndicators)) type = Type.DEATH
            if (line.containsAny(observationIndicators)) type = Type.OBSERVANCE
            if (line.containsAny(endOfContentIndicators)) break

            if (type !== null && shouldConstructHistoryItem(line)) {
                historyItems.add(buildHistoryItem(line, type, date))
            }
        }

        // If depth > 0, assign it the year of its parent (ie. most recent previous item with depth == 0), except observances
        var parentYear: Int? = null
        for (item in historyItems) {
            if (item.type == Type.OBSERVANCE) continue
            if (item.depth == 0) parentYear = item.year
            else {
                item.year = parentYear
                item.formatYear()
            }
        }

        return historyItems
    }

    private fun buildHistoryItem(line: String, type: Type, date: Date): HistoryItem {
        val year = parseYear(line, type)
        val depth = parseDepth(line)
        val (desc, links) = parseDescriptionAndLinks(line, type)
        return HistoryItem(type, date, year, desc, links, depth)
    }

    // Parse out unneeded chars and return integer representation of year, BC will be negative
    private fun parseYear(line: String, type: Type): Int? {
        if (type === Type.OBSERVANCE) return null

        var yearSection = ""
        for (separator in yearDescSeparators) {
            if (line.contains(separator)) {
                yearSection = line.substringBefore(separator)
                break
            }
        }

        // It's possible the line is just a year with no desc so attempt to parse directly, otherwise return null
        if (yearSection == "") {
            val yearInt: Int?
            try {
                yearInt = Regex("[^0-9]").replace(line, "").toInt()
            } catch (e: Exception) {
                Log.e(TAG, "$e")
                return null
            }

            return yearInt
        }

        if (yearSection.contains("(")) {
            val secondaryYear = yearSection.substringBetween("(", ")")
            yearSection = yearSection.replace(secondaryYear, "")
        }

        if (yearSection.contains("|")) {
            val secondaryYear = yearSection.substringAfter("|")
            yearSection = yearSection.replace(secondaryYear, "")
        }

        var yearInt: Int?
        try {
            yearInt = Regex("[^0-9]").replace(yearSection, "").toInt()
        } catch (e: Exception) {
            Log.e(TAG, "$e")
            return null
        }


        if (yearSection.contains("BC")) yearInt *= -1
        Log.d(TAG, "Parsed $yearInt from $line")
        return yearInt
    }

    // Depth of the item is determined by the number of asterisks (ie. item might have its own sublist)
    // Subtract 1 because root depth should be 0
    private fun parseDepth(line: String): Int {
        var depth = 0
        line.forEach { if (it == '*') depth++ }
        return --depth
    }

    // Parse line and return description and links
    private fun parseDescriptionAndLinks(line: String, type: Type): ParseResult {
        var desc = line
        if (type !== Type.OBSERVANCE) {
            for (separator in yearDescSeparators) {
                if (line.contains(separator)) {
                    desc = line.substringAfter(separator)
                    break
                }
            }

        }

        val links = mutableListOf<Link>()
        Log.d(TAG, desc)

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

        while (desc.contains("{{")) {
            val innerText = desc.substringBetween("{{", "}}")

            // Don't bother converting values for now, just show value and first given unit (ie. km)
            // TODO: Add support for converting values (example: July 28 1976)
            if (innerText.contains("convert")) {
                val splitText = innerText.split("|").toTypedArray()
                desc = desc.replaceFirst(innerText, splitText[1] + " " + splitText[2])
            }

            val parsedInnerText = innerText.replace("|", " ")
            desc = desc.replaceFirst(innerText, parsedInnerText)
            desc = desc.replaceFirst("{{", "")
            desc = desc.replaceFirst("}}", "")
        }

        // Remove/replace remaining unwanted characters
        desc = desc.replace("|", "")
        desc = desc.replace("\\", "")
        desc = desc.replace("&nbsp;", " ")
        desc = desc.trim('*', ' ')
        if (desc.contains("*")) {
            desc = desc.replace("*", "")
            if (type !== Type.OBSERVANCE) desc = desc.replaceFirst(" ", "")
        }

        Log.d(TAG, "Parsed desc ($desc) and links ($links) from $line")
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
            Log.e(TAG, "parseImageURL error")
        }

        Log.d(TAG,"Parsed image URL: $url")
        return url
    }

    // Misc. helper methods
    private fun shouldConstructHistoryItem(line: String): Boolean {
        var shouldConstruct = true

        if (!line.contains("*") || line.contains("<!--") || line.length <= 1) {
            shouldConstruct = false
        }

        for (separator in yearDescSeparators) {
            if (line.contains(separator)) {
                val yearSection = line.substringBefore(separator)
                if (yearSection.contains(" to ")) shouldConstruct = false
                break
            }
        }

        return shouldConstruct
    }

    private fun formatText(text: String): String {
        return text.capitalize()
    }

    private fun String.containsAny(list: List<String>): Boolean {
        var contains = false
        for (it in list) { if (this.contains(it)) contains = true }
        return contains
    }

    private fun String.substringBetween(str1: String, str2: String): String
            = this.substringAfter(str1).substringBefore(str2)
}