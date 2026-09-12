package com.dena.core

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

fun formatRelativeDate(timestamp: Long): String {
    val cal = Calendar.getInstance()
    val now = cal.timeInMillis
    
    cal.timeInMillis = timestamp
    val dateDay = cal.get(Calendar.DAY_OF_YEAR)
    val dateYear = cal.get(Calendar.YEAR)
    
    cal.timeInMillis = now
    val todayDay = cal.get(Calendar.DAY_OF_YEAR)
    val todayYear = cal.get(Calendar.YEAR)
    
    return if (dateYear == todayYear) {
        when (dateDay - todayDay) {
            0 -> "Today"
            -1 -> "Yesterday"
            1 -> "Tomorrow"
            else -> SimpleDateFormat("dd MMM", Locale.getDefault()).format(timestamp)
        }
    } else {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(timestamp)
    }
}
