package laynekm.dailyhistory

enum class Type constructor(private val type: String) {
    EVENT("Event"), BIRTH("Birth"), DEATH("Death");
    override fun toString(): String = this.type
}

// TODO: Add images, links
class HistoryItem (
    val type: Type,
    val year: String,
    val description: String) {

    // For debugging purposes
    override fun toString(): String {
        return "Type: ${type.toString()}\\nYear: $year\\nDescription: $description"
    }
}

class Link (
    val content: String,
    val link: String)