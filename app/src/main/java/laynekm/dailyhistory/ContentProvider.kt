package laynekm.dailyhistory

import android.net.Uri
import android.util.Log
import com.google.gson.JsonParser
import org.jetbrains.anko.doAsyncResult
import java.net.URL

class ContentProvider {

    fun getHistoryData() {
        doAsyncResult {
            val url = buildURL("May_11")
            val result = url.readText()
            Log.wtf("result", result)
            parseContent(result)
            Log.wtf("result: ", result)
//            activityUiThread {
//                longToast(result)
//            }
        }
    }

    // /w/api.php?action=query&format=json&prop=revisions&titles=May_11&formatversion=2&rvprop=content&rvslots=main&rvlimit=1
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

    // TODO: Add error handling in case content does not exist or API call fails
    private fun parseContent(json: String) {
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
        lines.forEach{ Log.wtf("lines", it) }

        // Split array into events, births, and deaths
        // Only care about strings starting with an asterisk
        val events = mutableListOf<String>()
        val births = mutableListOf<String>()
        val deaths = mutableListOf<String>()
        var addTo: MutableList<String>? = null
        lines.forEach {
            if (it.contains("==Events==")) addTo = events
            if (it.contains("==Births==")) addTo = births
            if (it.contains("==Deaths==")) addTo = deaths
            if (it.contains("==Holidays and observances==")) addTo = null
            if (addTo != null && it.contains("*")) addTo!!.add(it)

        }

        events!!.forEach{ Log.wtf("events", it) }
        births!!.forEach{ Log.wtf("births", it) }
        deaths!!.forEach{ Log.wtf("deaths", it) }
    }
}