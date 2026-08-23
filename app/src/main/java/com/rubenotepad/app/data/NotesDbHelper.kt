package com.rubenotepad.app.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Private on-device SQLite database. No cloud, no account, no tracking.
 */
class NotesDbHelper(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_NOTES (
                ${COL_ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                ${COL_TITLE} TEXT NOT NULL DEFAULT '',
                ${COL_CONTENT} TEXT NOT NULL DEFAULT '',
                ${COL_CREATED_AT} INTEGER NOT NULL,
                ${COL_UPDATED_AT} INTEGER NOT NULL,
                ${COL_PINNED} INTEGER NOT NULL DEFAULT 0,
                ${COL_ARCHIVED} INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_notes_updated ON $TABLE_NOTES($COL_UPDATED_AT)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Version 1 schema is the only one so far; future migrations go here.
    }

    companion object {
        const val DB_NAME = "rube_note_pad.db"
        const val DB_VERSION = 1
        const val TABLE_NOTES = "notes"
        const val COL_ID = "id"
        const val COL_TITLE = "title"
        const val COL_CONTENT = "content"
        const val COL_CREATED_AT = "created_at"
        const val COL_UPDATED_AT = "updated_at"
        const val COL_PINNED = "pinned"
        const val COL_ARCHIVED = "archived"
    }
}
