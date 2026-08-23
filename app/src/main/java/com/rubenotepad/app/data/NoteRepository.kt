package com.rubenotepad.app.data

import android.content.ContentValues
import android.content.Context
import android.database.SQLException
import android.util.Log

/**
 * Repository wrapping all note persistence. Every method is defensive: one broken
 * row or a database hiccup must never crash the application.
 */
class NoteRepository private constructor(context: Context) {

    private val dbHelper = NotesDbHelper(context)

    companion object {
        private const val TAG = "NoteRepository"

        @Volatile
        private var instance: NoteRepository? = null

        fun get(context: Context): NoteRepository =
            instance ?: synchronized(this) {
                instance ?: NoteRepository(context.applicationContext).also { instance = it }
            }
    }

    // ------------------------------------------------------------------
    // Queries
    // ------------------------------------------------------------------

    /**
     * Lists notes. Pinned notes always float to the top; the rest are ordered by
     * [sortAlphabetical] (A-Z) or most recently updated.
     */
    fun list(
        includeArchived: Boolean = false,
        searchQuery: String = "",
        sortAlphabetical: Boolean = false
    ): List<Note> {
        return try {
            val db = dbHelper.readableDatabase
            val clauses = mutableListOf<String>()
            val args = mutableListOf<String>()

            if (!includeArchived) {
                clauses.add("${NotesDbHelper.COL_ARCHIVED} = 0")
            }

            val q = searchQuery.trim()
            if (q.isNotEmpty()) {
                clauses.add(
                    "(${NotesDbHelper.COL_TITLE} LIKE ? OR ${NotesDbHelper.COL_CONTENT} LIKE ?)"
                )
                args.add("%$q%")
                args.add("%$q%")
            }

            val orderBy = buildString {
                append("${NotesDbHelper.COL_PINNED} DESC, ")
                append(if (sortAlphabetical) "${NotesDbHelper.COL_TITLE} COLLATE NOCASE ASC"
                else "${NotesDbHelper.COL_UPDATED_AT} DESC")
            }

            db.query(
                NotesDbHelper.TABLE_NOTES,
                null,
                if (clauses.isEmpty()) null else clauses.joinToString(" AND "),
                args.toTypedArray(),
                null,
                null,
                orderBy
            ).use { cursor ->
                val notes = ArrayList<Note>(cursor.count)
                while (cursor.moveToNext()) {
                    try {
                        notes.add(noteFromCursor(cursor))
                    } catch (e: Exception) {
                        // Skip a single corrupt row instead of failing the whole list.
                        Log.w(TAG, "Skipping unreadable note row", e)
                    }
                }
                notes
            }
        } catch (e: SQLException) {
            Log.e(TAG, "Failed to read notes", e)
            emptyList()
        }
    }

    fun get(id: Long): Note? = try {
        dbHelper.readableDatabase.query(
            NotesDbHelper.TABLE_NOTES,
            null,
            "${NotesDbHelper.COL_ID} = ?",
            arrayOf(id.toString()),
            null,
            null,
            null
        ).use { cursor ->
            if (cursor.moveToFirst()) noteFromCursor(cursor) else null
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to load note $id", e)
        null
    }

    // ------------------------------------------------------------------
    // Mutations
    // ------------------------------------------------------------------

    fun create(title: String = "", content: String = ""): Note? = try {
        val now = System.currentTimeMillis()
        val values = ContentValues().apply {
            put(NotesDbHelper.COL_TITLE, title)
            put(NotesDbHelper.COL_CONTENT, content)
            put(NotesDbHelper.COL_CREATED_AT, now)
            put(NotesDbHelper.COL_UPDATED_AT, now)
            put(NotesDbHelper.COL_PINNED, 0)
            put(NotesDbHelper.COL_ARCHIVED, 0)
        }
        val id = dbHelper.writableDatabase.insertOrThrow(NotesDbHelper.TABLE_NOTES, null, values)
        Note(id, title, content, now, now, pinned = false, archived = false)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to create note", e)
        null
    }

    fun update(note: Note): Boolean = try {
        val values = ContentValues().apply {
            put(NotesDbHelper.COL_TITLE, note.title)
            put(NotesDbHelper.COL_CONTENT, note.content)
            put(NotesDbHelper.COL_UPDATED_AT, System.currentTimeMillis())
            put(NotesDbHelper.COL_PINNED, if (note.pinned) 1 else 0)
            put(NotesDbHelper.COL_ARCHIVED, if (note.archived) 1 else 0)
        }
        dbHelper.writableDatabase.update(
            NotesDbHelper.TABLE_NOTES,
            values,
            "${NotesDbHelper.COL_ID} = ?",
            arrayOf(note.id.toString())
        ) > 0
    } catch (e: Exception) {
        Log.e(TAG, "Failed to update note ${note.id}", e)
        false
    }

    fun delete(id: Long): Boolean = try {
        dbHelper.writableDatabase.delete(
            NotesDbHelper.TABLE_NOTES,
            "${NotesDbHelper.COL_ID} = ?",
            arrayOf(id.toString())
        ) > 0
    } catch (e: Exception) {
        Log.e(TAG, "Failed to delete note $id", e)
        false
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private fun noteFromCursor(cursor: android.database.Cursor): Note {
        val idIdx = cursor.getColumnIndexOrThrow(NotesDbHelper.COL_ID)
        val titleIdx = cursor.getColumnIndexOrThrow(NotesDbHelper.COL_TITLE)
        val contentIdx = cursor.getColumnIndexOrThrow(NotesDbHelper.COL_CONTENT)
        val createdIdx = cursor.getColumnIndexOrThrow(NotesDbHelper.COL_CREATED_AT)
        val updatedIdx = cursor.getColumnIndexOrThrow(NotesDbHelper.COL_UPDATED_AT)
        val pinnedIdx = cursor.getColumnIndexOrThrow(NotesDbHelper.COL_PINNED)
        val archivedIdx = cursor.getColumnIndexOrThrow(NotesDbHelper.COL_ARCHIVED)

        return Note(
            id = cursor.getLong(idIdx),
            title = cursor.getString(titleIdx) ?: "",
            content = cursor.getString(contentIdx) ?: "",
            createdAt = cursor.getLong(createdIdx),
            updatedAt = cursor.getLong(updatedIdx),
            pinned = cursor.getInt(pinnedIdx) == 1,
            archived = cursor.getInt(archivedIdx) == 1
        )
    }
}
