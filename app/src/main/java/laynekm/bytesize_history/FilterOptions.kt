package laynekm.bytesize_history

private val defaultOrder: Order = Order.ASCENDING
private val defaultTypes: MutableList<Type> = mutableListOf(Type.EVENT, Type.BIRTH, Type.DEATH, Type.OBSERVANCE)
private val defaultEras: MutableList<Era> = mutableListOf(Era.ANCIENT, Era.MEDIEVAL, Era.EARLYMODERN, Era.EIGHTEENS, Era.NINETEENS, Era.TWOTHOUSANDS, Era.NONE)

// Filter options include order, types, and eras
// If any of these are not specified, default values will be used
class FilterOptions(
    var order: Order = defaultOrder,
    var types: MutableList<Type> = defaultTypes,
    var eras: MutableList<Era> = defaultEras) {

    fun copy(): FilterOptions {
        val orderCopy = order
        val typesCopy = types.toMutableList()
        val erasCopy = eras.toMutableList()
        return FilterOptions(orderCopy, typesCopy, erasCopy)
    }

    fun equals(newFilterOptions: FilterOptions): Boolean {
        var equal = true
        if (newFilterOptions.order !== order) equal = false
        if (!listsEqual(newFilterOptions.eras, eras)) equal = false
        if (!listsEqual(newFilterOptions.types, types)) equal = false
        return equal
    }

    // Lists considered equal if they contain the same values, regardless of order
    fun <T> listsEqual(list1: MutableList<T>, list2: MutableList<T>): Boolean {
        var listsEqual = true
        list1.forEach { if (!list2.contains(it)) listsEqual = false }
        list2.forEach { if (!list1.contains(it)) listsEqual = false }
        return listsEqual
    }

    override fun toString(): String {
        return "Order: $order, types: $types, eras: $eras"
    }
}