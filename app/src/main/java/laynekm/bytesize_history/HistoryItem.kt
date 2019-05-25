package laynekm.bytesize_history

import java.net.URL

enum class Type constructor(private val type: String) {
    EVENT("Event"), BIRTH("Birth"), DEATH("Death");
    override fun toString(): String = this.type
}

class HistoryItem (
    val type: Type,
    val year: String,
    val desc: String,
    val links: MutableList<Link>) {

    override fun toString(): String {
        var linkString = ""
        links.forEach { linkString += "\n - $it"}
        return "\nType: $type\nYear: $year\nDescription: $desc\nLinks:$linkString\n"
    }
}

class Link (
    val title: String,
    val link: String) {

    override fun toString(): String {
        return "$title ($link)"
    }
}