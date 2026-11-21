package com.example.ui.components

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

fun formatDurationHms(millis: Long): String {
    val totalSeconds = (millis / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
}

fun formatAccelSeconds(millis: Long?): String {
    if (millis == null) return "--.-- s"
    val sec = millis / 1000.0
    return String.format(Locale.US, "%.2f s", sec)
}

fun formatDistance(value: Double, decimals: Int = 2): String {
    return String.format(Locale.US, "%.${decimals}f", value)
}

fun bearingToCardinal(degrees: Float): String {
    val normalized = ((degrees % 360f) + 360f) % 360f
    val index = ((normalized + 22.5f) / 45f).toInt() % 8
    return when (index) {
        0 -> "N"
        1 -> "NE"
        2 -> "E"
        3 -> "SE"
        4 -> "S"
        5 -> "SW"
        6 -> "W"
        else -> "NW"
    }
}

fun formatDateTime(millis: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    return sdf.format(Date(millis))
}

fun formatCoord(value: Double, isLat: Boolean): String {
    val hemi = if (isLat) {
        if (value >= 0) "N" else "S"
    } else {
        if (value >= 0) "E" else "W"
    }
    return String.format(Locale.US, "%.4f°%s", kotlin.math.abs(value), hemi)
}

fun convertWarningKmhToUnit(limitKmh: Int, multiplierFromMps: Float): Int {
    val mps = limitKmh / 3.6f
    return (mps * multiplierFromMps).roundToInt()
}
