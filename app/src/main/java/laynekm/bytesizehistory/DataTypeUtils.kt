package laynekm.bytesizehistory

fun getEmptyTypeMap(): HashMap<Type, MutableList<HistoryItem>> {
    return hashMapOf(
        Type.EVENT to mutableListOf(),
        Type.BIRTH to mutableListOf(),
        Type.DEATH to mutableListOf(),
        Type.OBSERVANCE to mutableListOf()
    )
}

fun stringToType(type: String?): Type? {
    return when (type) {
        "Birth" -> Type.BIRTH
        "Death" -> Type.DEATH
        "Observance" -> Type.OBSERVANCE
        else -> Type.EVENT
    }
}

fun typeToString(type: Type): String {
    return when (type) {
        Type.EVENT -> "events"
        Type.BIRTH -> "births"
        Type.DEATH -> "deaths"
        Type.OBSERVANCE -> "holidays/observances"
    }
}

fun stringToTheme(theme: String?): Theme {
    return when (theme) {
        "light" -> Theme.LIGHT
        else -> Theme.DARK
    }
}

// Returns true if all lists in map are empty
fun mapIsEmpty(map: HashMap<Type, MutableList<HistoryItem>>): Boolean {
    return map.values.all { it.isEmpty() }
}