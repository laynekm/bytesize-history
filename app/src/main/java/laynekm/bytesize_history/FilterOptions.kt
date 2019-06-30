package laynekm.bytesize_history

import android.view.View
import android.widget.Switch

private val defaultOrder: Order = Order.ASCENDING
private val defaultTypes: MutableList<Type> = mutableListOf(Type.EVENT, Type.BIRTH, Type.DEATH)
private val defaultEras: MutableList<Era> = mutableListOf(Era.ANCIENT, Era.MEDIEVAL, Era.EARLYMODERN, Era.EIGHTEENS, Era.NINETEENS, Era.TWOTHOUSANDS)

class FilterOptions(
    var order: Order = defaultOrder,
    var types: MutableList<Type> = defaultTypes,
    var eras: MutableList<Era> = defaultEras) {

    // Sets view content based on filter options
    fun setViewContent(view: View) {
        if (order === Order.DESCENDING) (view.findViewById(R.id.switchOrder) as Switch).setChecked(true)

        if (types.contains(Type.EVENT)) (view.findViewById(R.id.switchEvents) as Switch).setChecked(true)
        if (types.contains(Type.BIRTH)) (view.findViewById(R.id.switchBirths) as Switch).setChecked(true)
        if (types.contains(Type.DEATH)) (view.findViewById(R.id.switchDeaths) as Switch).setChecked(true)

        if (eras.contains(Era.ANCIENT)) (view.findViewById(R.id.switchAncient) as Switch).setChecked(true)
        if (eras.contains(Era.MEDIEVAL)) (view.findViewById(R.id.switchMedieval) as Switch).setChecked(true)
        if (eras.contains(Era.EARLYMODERN)) (view.findViewById(R.id.switchEarlyModern) as Switch).setChecked(true)
        if (eras.contains(Era.EIGHTEENS)) (view.findViewById(R.id.switch1800s) as Switch).setChecked(true)
        if (eras.contains(Era.NINETEENS)) (view.findViewById(R.id.switch1900s) as Switch).setChecked(true)
        if (eras.contains(Era.TWOTHOUSANDS)) (view.findViewById(R.id.switch2000s) as Switch).setChecked(true)
    }

    // Applies amended view content to filter options
    fun setFilterOptions(view: View): Boolean {
        var newOrder = Order.ASCENDING
        val newTypes: MutableList<Type> = mutableListOf()
        val newEras: MutableList<Era> = mutableListOf()
        var changed = false

        if ((view.findViewById(R.id.switchOrder) as Switch).isChecked) newOrder = Order.DESCENDING

        if ((view.findViewById(R.id.switchEvents) as Switch).isChecked) newTypes.add(Type.EVENT)
        if ((view.findViewById(R.id.switchBirths) as Switch).isChecked) newTypes.add(Type.BIRTH)
        if ((view.findViewById(R.id.switchDeaths) as Switch).isChecked) newTypes.add(Type.DEATH)

        if ((view.findViewById(R.id.switchAncient) as Switch).isChecked) newEras.add(Era.ANCIENT)
        if ((view.findViewById(R.id.switchMedieval) as Switch).isChecked) newEras.add(Era.MEDIEVAL)
        if ((view.findViewById(R.id.switchEarlyModern) as Switch).isChecked) newEras.add(Era.EARLYMODERN)
        if ((view.findViewById(R.id.switch1800s) as Switch).isChecked) newEras.add(Era.EIGHTEENS)
        if ((view.findViewById(R.id.switch1900s) as Switch).isChecked) newEras.add(Era.NINETEENS)
        if ((view.findViewById(R.id.switch2000s) as Switch).isChecked) newEras.add(Era.TWOTHOUSANDS)

        if (order !== newOrder) {
            order = newOrder
            changed = true
        }

        if (!listsEqual(types, newTypes)) {
            types = newTypes
            changed = true
        }

        if (!listsEqual(eras, newEras)) {
            eras = newEras
            changed = true
        }

        return changed
    }

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