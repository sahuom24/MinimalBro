package dev.minimalphone.ui

import dev.minimalphone.session.MinimalRecord
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

fun totalMinutes(records: List<MinimalRecord>): Int = records.sumOf { it.minutes }

fun completedCount(records: List<MinimalRecord>): Int = records.count { it.completed }

fun todayMinutes(records: List<MinimalRecord>, zone: ZoneId = ZoneId.systemDefault()): Int {
    val today = Instant.now().atZone(zone).toLocalDate()
    return records
        .filter { Instant.ofEpochMilli(it.endedAtMillis).atZone(zone).toLocalDate() == today }
        .sumOf { it.minutes }
}

/** Countdown label: "29:59", "14:22:08", or "6d 23:59:59". */
fun formatCountdown(millis: Long): String {
    val totalSeconds = ((millis.coerceAtLeast(0L)) + 999) / 1000
    val days = totalSeconds / 86400
    val hours = (totalSeconds % 86400) / 3600
    val totalHours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return when {
        days >= 2 || totalHours > 24 -> String.format(
            Locale.ROOT,
            "%dd %02d:%02d:%02d",
            days,
            hours,
            minutes,
            seconds,
        )
        totalHours > 0 -> String.format(Locale.ROOT, "%d:%02d:%02d", totalHours, minutes, seconds)
        else -> String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)
    }
}

/** Elapsed compact readout for Ongoing mode, e.g. "2d 4h", "3h 12m", "18m". */
fun formatElapsedShort(millis: Long): String {
    val totalMinutes = (millis.coerceAtLeast(0L)) / 60_000L
    val days = totalMinutes / (24 * 60)
    val hours = (totalMinutes % (24 * 60)) / 60
    val minutes = totalMinutes % 60
    return when {
        days > 0 && hours > 0 -> "${days}d ${hours}h"
        days > 0 -> "${days}d"
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        else -> "${minutes}m"
    }
}

/** Compact label for timed/ongoing commitments (`hours == 0` -> "Ongoing"). */
fun formatCommitmentHours(hours: Int): String = when {
    hours <= 0 -> "Ongoing"
    hours < 24 -> "${hours}h"
    hours % 24 == 0 -> {
        val d = hours / 24
        if (d == 1) "1 day" else "$d days"
    }
    else -> "${hours / 24}d ${hours % 24}h"
}

/** Human summary of logged minutes ("42 min" or "3 h 20 min"). */
fun formatLoggedMinutes(minutes: Int): String = when {
    minutes < 60 -> "$minutes min"
    minutes % 60 == 0 -> "${minutes / 60} h"
    else -> "${minutes / 60} h ${minutes % 60} min"
}
