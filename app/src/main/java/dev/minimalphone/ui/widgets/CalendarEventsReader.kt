package dev.minimalphone.ui.widgets

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class TodayScheduleEvent(
    val id: Long,
    val title: String,
    val startMillis: Long,
    val endMillis: Long,
    val isAllDay: Boolean,
    val location: String?,
) {
    fun formatTimeRange(): String {
        if (isAllDay) return "All day"
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val start = timeFormat.format(Date(startMillis))
        val end = timeFormat.format(Date(endMillis))
        return "$start - $end"
    }
}

/**
 * Reads today's live calendar meetings and agenda from the device's native Calendar provider.
 */
object CalendarEventsReader {

    private val PROJECTION = arrayOf(
        CalendarContract.Instances.EVENT_ID,
        CalendarContract.Instances.TITLE,
        CalendarContract.Instances.BEGIN,
        CalendarContract.Instances.END,
        CalendarContract.Instances.ALL_DAY,
        CalendarContract.Instances.EVENT_LOCATION,
    )

    fun hasCalendarPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR,
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun readTodayEvents(context: Context): List<TodayScheduleEvent> {
        if (!hasCalendarPermission(context)) return emptyList()

        return runCatching {
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfDay = cal.timeInMillis
            val endOfDay = startOfDay + 24 * 60 * 60 * 1000L - 1L

            val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
            ContentUris.appendId(builder, startOfDay)
            ContentUris.appendId(builder, endOfDay)

            val cursor = context.contentResolver.query(
                builder.build(),
                PROJECTION,
                null,
                null,
                "${CalendarContract.Instances.BEGIN} ASC",
            )

            val events = mutableListOf<TodayScheduleEvent>()
            cursor?.use {
                val idIdx = it.getColumnIndex(CalendarContract.Instances.EVENT_ID)
                val titleIdx = it.getColumnIndex(CalendarContract.Instances.TITLE)
                val beginIdx = it.getColumnIndex(CalendarContract.Instances.BEGIN)
                val endIdx = it.getColumnIndex(CalendarContract.Instances.END)
                val allDayIdx = it.getColumnIndex(CalendarContract.Instances.ALL_DAY)
                val locIdx = it.getColumnIndex(CalendarContract.Instances.EVENT_LOCATION)

                while (it.moveToNext()) {
                    val id = if (idIdx >= 0) it.getLong(idIdx) else 0L
                    val title = (if (titleIdx >= 0) it.getString(titleIdx) else null) ?: "(No title)"
                    val begin = if (beginIdx >= 0) it.getLong(beginIdx) else 0L
                    val end = if (endIdx >= 0) it.getLong(endIdx) else 0L
                    val allDay = if (allDayIdx >= 0) it.getInt(allDayIdx) == 1 else false
                    val loc = if (locIdx >= 0) it.getString(locIdx) else null

                    events.add(
                        TodayScheduleEvent(
                            id = id,
                            title = title,
                            startMillis = begin,
                            endMillis = end,
                            isAllDay = allDay,
                            location = loc,
                        ),
                    )
                }
            }
            events
        }.getOrDefault(emptyList())
    }

    fun openEvent(context: Context, eventId: Long) {
        val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
        val intent = Intent(Intent.ACTION_VIEW).setData(uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        if (runCatching { context.startActivity(intent) }.isFailure) {
            val fallback = Intent(Intent.ACTION_VIEW).setData(CalendarContract.CONTENT_URI).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            runCatching { context.startActivity(fallback) }
        }
    }
}
