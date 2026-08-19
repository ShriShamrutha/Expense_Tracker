package com.example.expense__tracker

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

fun formatDate(millis: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return sdf.format(Date(millis))
}

// DatePicker returns midnight UTC for the picked calendar date.
// These convert that into the actual start/end of that same calendar
// date in the device's local timezone, so filtering by timestamp is accurate.
fun startOfLocalDay(utcMidnightMillis: Long): Long {
    val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    utcCal.timeInMillis = utcMidnightMillis
    val localCal = Calendar.getInstance()
    localCal.set(
        utcCal.get(Calendar.YEAR),
        utcCal.get(Calendar.MONTH),
        utcCal.get(Calendar.DAY_OF_MONTH),
        0, 0, 0
    )
    localCal.set(Calendar.MILLISECOND, 0)
    return localCal.timeInMillis
}

fun endOfLocalDay(utcMidnightMillis: Long): Long {
    return startOfLocalDay(utcMidnightMillis) + (24 * 60 * 60 * 1000L) - 1
}

fun startOfCurrentMonth(): Long {
    val cal = Calendar.getInstance()
    cal.set(Calendar.DAY_OF_MONTH, 1)
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}