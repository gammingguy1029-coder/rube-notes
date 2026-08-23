package com.rubenotepad.app.data

import android.content.Context

/**
 * Small local key/value preferences (SharedPreferences).
 * Holds onboarding state and UI choices - all on-device only.
 */
class AppPrefs(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** True once the user has completed first-launch onboarding. */
    var onboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDED, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDED, value).apply()

    /** Sort notes A-Z instead of by recency. */
    var sortAlphabetical: Boolean
        get() = prefs.getBoolean(KEY_SORT_ALPHA, false)
        set(value) = prefs.edit().putBoolean(KEY_SORT_ALPHA, value).apply()

    /** Two-column grid layout for the notes list. */
    var gridView: Boolean
        get() = prefs.getBoolean(KEY_GRID, false)
        set(value) = prefs.edit().putBoolean(KEY_GRID, value).apply()

    /** Show archived notes instead of active ones. */
    var showArchived: Boolean
        get() = prefs.getBoolean(KEY_ARCHIVED, false)
        set(value) = prefs.edit().putBoolean(KEY_ARCHIVED, value).apply()

    companion object {
        private const val PREFS_NAME = "rube_note_pad_prefs"
        private const val KEY_ONBOARDED = "onboarding_completed"
        private const val KEY_SORT_ALPHA = "sort_alphabetical"
        private const val KEY_GRID = "grid_view"
        private const val KEY_ARCHIVED = "show_archived"
    }
}
