package laynekm.bytesizehistory

import android.content.Context
import android.view.View
import android.widget.Switch

// Handles filters and the shared preferences associated with them
class FilterManager(var context: Context) {

    private val preferencesKey = context.getString(R.string.preferences_key)
    private val hasPreferencesKey = context.getString(R.string.has_filter_prefs_key)
    private val sharedPref = context.getSharedPreferences(preferencesKey, Context.MODE_PRIVATE)

    // Sets view content based on filter options
    fun setViewContent(view: View, options: FilterOptions) {
        if (options.order === Order.DESCENDING) (view.findViewById(R.id.notification_switch) as Switch).setChecked(true)
        if (options.types.contains(Type.EVENT)) (view.findViewById(R.id.switchEvents) as Switch).setChecked(true)
        if (options.types.contains(Type.BIRTH)) (view.findViewById(R.id.switchBirths) as Switch).setChecked(true)
        if (options.types.contains(Type.DEATH)) (view.findViewById(R.id.switchDeaths) as Switch).setChecked(true)
        if (options.types.contains(Type.OBSERVANCE)) (view.findViewById(R.id.switchObservances) as Switch).setChecked(true)
        if (options.eras.contains(Era.ANCIENT)) (view.findViewById(R.id.switchAncient) as Switch).setChecked(true)
        if (options.eras.contains(Era.MEDIEVAL)) (view.findViewById(R.id.switchMedieval) as Switch).setChecked(true)
        if (options.eras.contains(Era.EARLYMODERN)) (view.findViewById(R.id.switchEarlyModern) as Switch).setChecked(true)
        if (options.eras.contains(Era.EIGHTEENS)) (view.findViewById(R.id.switch1800s) as Switch).setChecked(true)
        if (options.eras.contains(Era.NINETEENS)) (view.findViewById(R.id.switch1900s) as Switch).setChecked(true)
        if (options.eras.contains(Era.TWOTHOUSANDS)) (view.findViewById(R.id.switch2000s) as Switch).setChecked(true)
    }

    // Applies amended view content to filter options and updates preferences
    fun setFilterOptions(view: View): FilterOptions {
        var order = Order.ASCENDING
        val types: MutableList<Type> = mutableListOf()
        val eras: MutableList<Era> = mutableListOf()

        if ((view.findViewById(R.id.notification_switch) as Switch).isChecked) order = Order.DESCENDING
        if ((view.findViewById(R.id.switchEvents) as Switch).isChecked) types.add(Type.EVENT)
        if ((view.findViewById(R.id.switchBirths) as Switch).isChecked) types.add(Type.BIRTH)
        if ((view.findViewById(R.id.switchDeaths) as Switch).isChecked) types.add(Type.DEATH)
        if ((view.findViewById(R.id.switchObservances) as Switch).isChecked) types.add(Type.OBSERVANCE)
        if ((view.findViewById(R.id.switchAncient) as Switch).isChecked) eras.add(Era.ANCIENT)
        if ((view.findViewById(R.id.switchMedieval) as Switch).isChecked) eras.add(Era.MEDIEVAL)
        if ((view.findViewById(R.id.switchEarlyModern) as Switch).isChecked) eras.add(Era.EARLYMODERN)
        if ((view.findViewById(R.id.switch1800s) as Switch).isChecked) eras.add(Era.EIGHTEENS)
        if ((view.findViewById(R.id.switch1900s) as Switch).isChecked) eras.add(Era.NINETEENS)
        if ((view.findViewById(R.id.switch2000s) as Switch).isChecked) eras.add(Era.TWOTHOUSANDS)

        return FilterOptions(order, types, eras)
    }

    fun hasPreferences(): Boolean {
        return sharedPref.contains(hasPreferencesKey)
    }

    // Gets preferences if they exist
    fun getPreferences(): FilterOptions {
        var order: Order = Order.ASCENDING
        val types: MutableList<Type> = mutableListOf()
        val eras: MutableList<Era> = mutableListOf()

        if (sharedPref.getBoolean("${Order.DESCENDING}", false)) order = Order.DESCENDING

        Type.values().forEach {
            if (sharedPref.getBoolean("$it", false)) types.add(it)
        }

        Era.values().forEach {
            if (sharedPref.getBoolean("$it", false)) eras.add(it)
        }

        return FilterOptions(order, types, eras)
    }

    // Set preferences; in most cases, the enum string value itself is used as the key
    fun setPreferences(options: FilterOptions) {
        with (sharedPref.edit()) {
            putBoolean("${Order.DESCENDING}", options.order === Order.DESCENDING)
            Type.values().forEach { putBoolean("$it", options.types.contains(it)) }
            Era.values().forEach { putBoolean("$it", options.eras.contains(it)) }
            putBoolean(hasPreferencesKey, true)
            apply()
        }
    }
}