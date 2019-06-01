package laynekm.bytesize_history

enum class Order constructor(private val type: String) {
    ASCENDING("Event"), DESCENDING("Birth");
    override fun toString(): String = this.type
}

class FilterOptions {
    var order: Order = Order.ASCENDING
    var eras: MutableList<Era> = mutableListOf(Era.ANCIENT, Era.EARLYMODERN)
    var types: MutableList<Type> = mutableListOf(Type.EVENT)
}