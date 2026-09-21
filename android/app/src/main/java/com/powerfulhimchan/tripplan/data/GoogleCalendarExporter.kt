package com.powerfulhimchan.tripplan.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import com.powerfulhimchan.tripplan.model.GoogleCalendar
import com.powerfulhimchan.tripplan.model.Trip
import java.time.Instant

data class CalendarExportResult(val created: Int, val updated: Int)

class GoogleCalendarExporter(private val context: Context) {
    private val resolver = context.contentResolver
    private val mappings = context.getSharedPreferences("google_calendar_exports", Context.MODE_PRIVATE)

    fun calendars(): List<GoogleCalendar> {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
        )
        return buildList {
            resolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                "${CalendarContract.Calendars.ACCOUNT_TYPE} = ? AND ${CalendarContract.Calendars.VISIBLE} = 1 " +
                    "AND ${CalendarContract.Calendars.SYNC_EVENTS} = 1",
                arrayOf("com.google"),
                "${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} ASC",
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
                val nameIndex = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
                val accountIndex = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME)
                while (cursor.moveToNext()) {
                    add(GoogleCalendar(cursor.getLong(idIndex), cursor.getString(nameIndex), cursor.getString(accountIndex)))
                }
            }
        }
    }

    fun export(trip: Trip, calendarId: Long): CalendarExportResult {
        var created = 0
        var updated = 0
        trip.items.forEach { item ->
            val values = ContentValues().apply {
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.TITLE, item.title)
                put(CalendarContract.Events.DTSTART, Instant.parse(item.scheduledAt).toEpochMilli())
                put(CalendarContract.Events.DTEND, Instant.parse(item.endsAt).toEpochMilli())
                put(CalendarContract.Events.EVENT_TIMEZONE, trip.timezone)
                put(CalendarContract.Events.EVENT_LOCATION, item.place.orEmpty())
                put(CalendarContract.Events.DESCRIPTION, item.memo.orEmpty())
            }
            val mappingKey = "${trip.id}:${item.id}"
            val mapped = mappings.getString(mappingKey, null)?.split(':')
            val mappedCalendarId = mapped?.getOrNull(0)?.toLongOrNull()
            val eventId = mapped?.getOrNull(1)?.toLongOrNull()
            val canUpdate = mappedCalendarId == calendarId && eventId != null && eventExists(eventId)
            if (canUpdate) {
                resolver.update(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId!!), values, null, null)
                updated++
            } else {
                val uri = resolver.insert(CalendarContract.Events.CONTENT_URI, values)
                    ?: error("Google 캘린더 일정을 생성하지 못했습니다.")
                val newEventId = ContentUris.parseId(uri)
                mappings.edit().putString(mappingKey, "$calendarId:$newEventId").apply()
                created++
            }
        }
        return CalendarExportResult(created, updated)
    }

    private fun eventExists(eventId: Long): Boolean = resolver.query(
        ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId),
        arrayOf(CalendarContract.Events._ID),
        null,
        null,
        null,
    )?.use { it.moveToFirst() } == true
}
