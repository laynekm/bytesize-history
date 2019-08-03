package laynekm.bytesize_history

import java.text.DateFormatSymbols
import java.util.*

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

fun generateAllDatesInYear(): MutableList<Date> {
    val daysInMonthMap: Map<Int, Int> = mapOf(
        0 to 31, 1 to 29, 2 to 31, 3 to 30, 4 to 31, 5 to 30,
        6 to 31, 7 to 31, 8 to 30, 9 to 31, 10 to 30, 11 to 31
    )

    val dates: MutableList<Date> = mutableListOf()
    for (month in 0..11) {
        for (day in 1..daysInMonthMap.getValue(month)) {
            dates.add(Date(month, day))
        }
    }

    return dates
}

fun timeToString(time: Time): String {
    return "${time.hour}:${time.minute.toString().padStart(2, '0')}"
}

fun stringToTime(time: String): Time {
    return Time(time.substringBefore(":").toInt(), time.substringAfter(":").toInt())
}

fun stringTo12HourString(time: String): String {
    val hour = stringToTime(time).hour
    val minute = stringToTime(time).minute.toString().padStart(2, '0')

    return when (hour) {
        0 -> "12:$minute AM"
        12 -> "12:$minute PM"
        in 1..11 -> "$hour:$minute AM"
        else -> "${hour - 12}:$minute PM"
    }
}

fun timeTo12HourString(time: Time): String {
    return stringTo12HourString(timeToString(time))
}