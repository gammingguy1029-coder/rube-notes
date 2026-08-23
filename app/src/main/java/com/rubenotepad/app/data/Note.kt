package com.rubenotepad.app.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Local note model. Persisted in the private on-device SQLite database.
 */
data class Note(
    val id: Long,
    val title: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long,
    val pinned: Boolean,
    val archived: Boolean
) {
    /** Never show a blank title in lists; fall back to a readable placeholder. */
    val displayTitle: String
        get() = title.ifBlank { "(Untitled)" }

    companion object {

        private val dateFormat = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())

        /**
         * Human friendly relative timestamp ("5 hr ago"), matching the reference design,
         * falling back to an absolute date for older notes.
         */
        fun relativeTime(timeMs: Long, now: Long = System.currentTimeMillis()): String {
            if (timeMs <= 0L) return ""
            val diff = now - timeMs
            val minute = 60_000L
            val hour = 60 * minute
            val day = 24 * hour
            return when {
                diff < minute -> "Just now"
                diff < hour -> "${diff / minute} min ago"
                diff < day -> "${diff / hour} hr ago"
                diff < 7 * day -> "${diff / day} days ago"
                else -> dateFormat.format(Date(timeMs))
            }
        }
    }
}
