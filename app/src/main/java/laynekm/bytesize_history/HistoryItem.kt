package laynekm.bytesize_history

enum class Type constructor(private val type: String) {
    EVENT("Event"), BIRTH("Birth"), DEATH("Death");
    override fun toString(): String = this.type
}

enum class Era constructor(private val type: String) {
    ANCIENT("Ancient"),
    MEDIEVAL("Medieval"),
    EARLYMODERN("Early Modern"),
    EIGHTEENS("1800s"),
    NINETEENS("1900s"),
    TWOTHOUSANDS("2000s");
    override fun toString(): String = this.type
}

class HistoryItem (
    val type: Type,
    val year: Int,
    val desc: String,
    val links: MutableList<Link>,
    var image: String?) {

   var era: Era = determineEra(year)

    private fun determineEra(year: Int): Era {
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
    val title: String,
    val link: String) {

    override fun toString(): String {
        return "$title ($link)"
    }
}