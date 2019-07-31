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
        1 to 31, 2 to 29, 3 to 31, 4 to 30, 5 to 31, 6 to 30,
        7 to 31, 8 to 31, 9 to 30, 10 to 31, 11 to 30, 12 to 31
    )

    val dates: MutableList<Date> = mutableListOf()
    for (month in 1..daysInMonthMap.keys.size) {
        for (day in 1..daysInMonthMap.getValue(month)) {
            dates.add(Date(month, day))
        }
    }

    System.out.println(dates)
    return dates
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