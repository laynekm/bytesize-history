package laynekm.dailyhistory

enum class Type constructor(private val type: String) {
    EVENT("Event"), BIRTH("Birth"), DEATH("Death");
    override fun toString(): String = this.type
}

// TODO: Add images, links
class HistoryItem (
    val type: Type,
    val year: String,
    val desc: String,
    val image: String,
    val links: MutableList<Link>) {

    override fun toString(): String {
        var linkString = ""
        links.forEach { linkString += "\n - $it"}
        return "\nType: $type\nYear: $year\nDescription: $desc\nImage URL: $image\nLinks:$linkString\n"
    }
}

// TODO: Fetch content from links
class Link (
    val link: String
    /* val desc: String */) {

    override fun toString(): String {
        return link
    }
}