package laynekm.bytesize_history

data class Date(val month: Int, val day: Int)
data class Time(val hour: Int, val minute: Int)
data class ParseResult(val desc: String, val links: MutableList<Link>)

enum class Theme constructor(private val theme: String) {
    LIGHT("light"), DARK("dark");
    override fun toString(): String = this.theme
}

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

enum class Error constructor(private val type: String) {
    FETCH_ERROR("Fetch error"),
    TYPE_ERROR("Type error"),
    FILTER_ERROR("Filter error"),
}