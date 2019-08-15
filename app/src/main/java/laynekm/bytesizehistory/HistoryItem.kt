package laynekm.bytesizehistory

class HistoryItem (
    val type: Type,
    val date: Date,
    var year: Int?,
    var desc: String,
    val links: MutableList<Link>,
    val depth: Int) {

    var image: String = ""
    var hasFetchedImage: Boolean = false
    var formattedYear = formatYear(year)
    var era: Era = determineEra(year)
    var linksVisible = false

    fun formatYear() {
        this.formattedYear = this.formatYear(this.year)
    }

    private fun formatYear(year: Int?): String {
        return when {
            year == null -> ""
            this.depth > 0 -> ""
            year < 0 -> "${year * -1} BC"
            year in 0..500 -> "$year AD"
            else -> "$year"
        }
    }

    private fun determineEra(year: Int?): Era {
        return when {
            year == null -> Era.NONE
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