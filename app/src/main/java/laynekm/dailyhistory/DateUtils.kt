package laynekm.dailyhistory

import java.text.DateFormatSymbols
import java.util.*

data class Date(val month: Int, val day: Int)

fun getToday(): Date {
    val date: Calendar = Calendar.getInstance()
    return Date(date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH))
}

fun buildDateURL(date: Date): String {
    return "${DateFormatSymbols().months[date.month]}_${date.day}"
}

// TODO: Add "Today" if current date
fun buildDateLabel(date: Date): String {
    return "${DateFormatSymbols().months[date.month]} ${date.day}"
}