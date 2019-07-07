package laynekm.bytesize_history

import java.text.DateFormatSymbols
import java.util.*

data class Date(val month: Int, val day: Int)
data class Time(val hour: Int, val minute: Int)

fun datesEqual(date1: Date, date2: Date): Boolean {
    return date1.month == date2.month && date1.day == date2.day
}

fun getToday(): Date {
    val date: Calendar = Calendar.getInstance()
    return Date(date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH))
}

fun buildDateForURL(date: Date): String {
    return "${DateFormatSymbols().months[date.month]}_${date.day}"
}

fun buildDateForLabel(date: Date): String {
    val today = getToday()
    if (datesEqual(date, today)) return "Today (${DateFormatSymbols().months[date.month]} ${date.day})"
    return "${DateFormatSymbols().months[date.month]} ${date.day}"
}

fun buildDateForNotification(date: Date): String {
    return "${DateFormatSymbols().months[date.month]} ${date.day}"
}

fun dateToString(date: Date): String {
    return "${date.month}-${date.day}"
}

fun stringToDate(date: String?): Date {
    return Date(date!!.substringBefore("-").toInt(), date.substringAfter("-").toInt())
}

fun timesEqual(time1: Time, time2: Time): Boolean {
    return time1.hour == time2.hour && time1.minute == time2.minute
}

fun timeToString(time: Time): String {
    return "${time.hour}:${time.minute.toString().padStart(2, '0')}"
}

fun stringToTime(time: String): Time {
    return Time(time.substringBefore(":").toInt(), time.substringAfter(":").toInt())
}