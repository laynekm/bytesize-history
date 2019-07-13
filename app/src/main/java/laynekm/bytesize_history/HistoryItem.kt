package laynekm.bytesize_history

enum class Order constructor(private val type: String) {
    ASCENDING("Ascending"), DESCENDING("Descending");
    override fun toString(): String = this.type
}

enum class Type constructor(private val type: String) {
    EVENT("Event"), BIRTH("Birth"), DEATH("Death"), OBSERVANCE("Observance");
    override fun toString(): String = this.type
}

enum class Era constructor(private val type: String) {
    ANCIENT("Ancient"),
    MEDIEVAL("Medieval"),
    EARLYMODERN("Early Modern"),
    EIGHTEENS("1800s"),
    NINETEENS("1900s"),
    TWOTHOUSANDS("2000s"),
    NONE("None");
    override fun toString(): String = this.type
}

class HistoryItem (
    val type: Type,
    val year: Int?,
    var desc: String,
    val links: MutableList<Link>,
    val depth: Int) {

    var image: String = ""
    var hasFetchedImage: Boolean = false
    var era: Era = determineEra(year)

    private fun determineEra(year: Int?): Era {
        if (year === null) return Era.NONE

        return when {
            year < 500 -> Era.ANCIENT
            year in 500..1499 -> Era.MEDIEVAL
            year in 1500..1799 -> Era.EARLYMODERN
            year in 1800..1899 -> Era.EIGHTEENS
            year in 1900..1999 -> Era.NINETEENS
            else -> Era.TWOTHOUSANDS
        }
    }

    override fun toString(): String {
        var linkString = ""
        links.forEach { linkString += "\n - $it"}
        return "\nType: $type\nEra: $era\nYear: $year\nDescription: $desc\nLinks:$linkString\n"
    }
}

class Link (
    var title: String,
    var link: String) {

    override fun toString(): String {
        return "$title ($link)"
    }
}

fun getEmptyTypeMap(): MutableMap<Type, MutableList<HistoryItem>> {
    return mutableMapOf(
        Type.EVENT to mutableListOf(),
        Type.BIRTH to mutableListOf(),
        Type.DEATH to mutableListOf(),
        Type.OBSERVANCE to mutableListOf()
    )
}

fun mapTypeToLabel(type: Type): String {
    return when (type) {
        Type.EVENT -> "events"
        Type.BIRTH -> "births"
        Type.DEATH -> "deaths"
        Type.OBSERVANCE -> "holidays/observances"
    }
}