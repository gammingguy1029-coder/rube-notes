package com.rubenotepad.app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textview.MaterialTextView
import com.google.android.material.textfield.TextInputEditText
import com.rubenotepad.app.data.Note
import com.rubenotepad.app.data.NoteRepository

/**
 * Full-screen editor styled like the reference: yellow header with the note title,
 * a "Saving…/Saved" indicator, relative timestamp, and a lined-paper content area.
 *
 * Changes autosave locally after a short debounce; anything pending is flushed in
 * [onPause], so switching notes, going home or closing the app never loses text.
 */
class EditorActivity : AppCompatActivity() {

    private lateinit var repository: NoteRepository

    private lateinit var etTitle: TextInputEditText
    private lateinit var etContent: TextInputEditText
    private lateinit var tvSaveState: MaterialTextView
    private lateinit var tvTimestamp: MaterialTextView

    private val saveHandler = Handler(Looper.getMainLooper())

    private var note: Note? = null
    private var isNewNote = false
    private var deletedManually = false
    private var dirty = false
    private var suppressWatcher = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)

        repository = NoteRepository.get(this)

        etTitle = findViewById(R.id.etTitle)
        etContent = findViewById(R.id.etContent)
        tvSaveState = findViewById(R.id.tvSaveState)
        tvTimestamp = findViewById(R.id.tvTimestamp)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.btnMenu).setOnClickListener { view ->
            showOverflowMenu(view)
        }

        loadOrCreateNote(savedInstanceState)
        bindNoteToViews()
        installWatchers()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        note?.let { outState.putLong(STATE_NOTE_ID, it.id) }
    }

    // ------------------------------------------------------------------
    // Load / create
    // ------------------------------------------------------------------

    private fun loadOrCreateNote(savedInstanceState: Bundle?) {
        // Survives rotation: prefer the saved note ID over the launch intent's.
        val requestedId = savedInstanceState?.getLong(STATE_NOTE_ID, 0L)
            ?.takeIf { it > 0L }
            ?: intent.getLongExtra(EXTRA_NOTE_ID, 0L)
        if (requestedId > 0L) {
            note = repository.get(requestedId)
            if (note == null) {
                // The note vanished (corrupt row, storage reset) - fail gracefully.
                android.widget.Toast.makeText(
                    this, R.string.note_unavailable, android.widget.Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        } else {
            // New note: create the row up front so autosave has a stable target.
            note = repository.create()
            isNewNote = true
            if (note == null) {
                android.widget.Toast.makeText(
                    this, R.string.storage_error, android.widget.Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }
    }

    private fun bindNoteToViews() {
        val n = note ?: return
        suppressWatcher = true
        etTitle.setText(n.title)
        etContent.setText(n.content)
        tvTimestamp.text = getString(R.string.edited_prefix, Note.relativeTime(n.updatedAt))
        tvSaveState.isVisible = false
        suppressWatcher = false
    }

    private fun installWatchers() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (suppressWatcher) return
                markDirtyAndScheduleSave()
            }
        }
        etTitle.addTextChangedListener(watcher)
        etContent.addTextChangedListener(watcher)
    }

    // ------------------------------------------------------------------
    // Debounced autosave
    // ------------------------------------------------------------------

    private fun markDirtyAndScheduleSave() {
        dirty = true
        tvSaveState.text = getString(R.string.saving)
        tvSaveState.isVisible = true
        saveHandler.removeCallbacks(commitRunnable)
        saveHandler.postDelayed(commitRunnable, SAVE_DEBOUNCE_MS)
    }

    private val commitRunnable = Runnable { commitPendingChanges() }

    private fun commitPendingChanges() {
        val n = note ?: return
        dirty = false
        val updated = n.copy(
            title = etTitle.text?.toString().orEmpty(),
            content = etContent.text?.toString().orEmpty()
        )
        if (repository.update(updated)) {
            note = updated.copy(updatedAt = System.currentTimeMillis())
            tvTimestamp.text =
                getString(R.string.edited_prefix, Note.relativeTime(note!!.updatedAt))
            tvSaveState.text = getString(R.string.saved)
            // Keep "Saved" visible briefly so the user sees confirmation.
            saveHandler.postDelayed({
                if (!dirty) tvSaveState.isVisible = false
            }, SAVED_VISIBLE_MS)
        } else {
            tvSaveState.text = getString(R.string.save_failed)
        }
    }

    /** Persist any pending change immediately (note switch, Home, app close). */
    private fun flushIfDirty() {
        saveHandler.removeCallbacks(commitRunnable)
        if (dirty && note != null) commitPendingChanges()
    }

    override fun onPause() {
        flushIfDirty()
        super.onPause()
    }

    override fun onDestroy() {
        saveHandler.removeCallbacksAndMessages(null)
        // A brand-new note that was left completely empty is removed so the list
        // never fills with junk drafts. Only when actually finishing (not rotation).
        val n = note
        if (isFinishing && !deletedManually && isNewNote && n != null &&
            n.title.isBlank() && etTitle.text.isNullOrBlank() &&
            n.content.isBlank() && etContent.text.isNullOrBlank()
        ) {
            repository.delete(n.id)
        }
        super.onDestroy()
    }

    // ------------------------------------------------------------------
    // Overflow menu (Pin / Share / Archive / Delete), matching the reference
    // ------------------------------------------------------------------

    private fun showOverflowMenu(anchor: View) {
        val n = note ?: return
        val popup = PopupMenu(this, anchor)
        popup.menu.apply {
            add(0, MENU_PIN, 0, getString(if (n.pinned) R.string.action_unpin else R.string.action_pin))
            add(0, MENU_SHARE, 1, getString(R.string.action_share))
            add(0, MENU_ARCHIVE, 2, getString(if (n.archived) R.string.action_unarchive else R.string.action_archive))
            add(0, MENU_DELETE, 3, getString(R.string.action_delete))
        }
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                MENU_PIN -> {
                    note = note?.copy(pinned = !n.pinned)?.let {
                        repository.update(it)
                        it
                    }
                    true
                }
                MENU_SHARE -> {
                    shareCurrent()
                    true
                }
                MENU_ARCHIVE -> {
                    note?.copy(archived = !n.archived)?.let {
                        repository.update(it)
                        note = it
                    }
                    true
                }
                MENU_DELETE -> {
                    confirmDeleteCurrent()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun shareCurrent() {
        flushIfDirty()
        val n = note ?: return
        val text = buildString {
            append(n.title.ifBlank { getString(R.string.untitled_note) })
            append("\n\n")
            append(n.content)
        }
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, n.title)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(send, getString(R.string.action_share)))
    }

    private fun confirmDeleteCurrent() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_confirm_title)
            .setMessage(R.string.delete_confirm_message_single)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                deletedManually = true
                note?.let { repository.delete(it.id) }
                finish()
            }
            .show()
    }

    companion object {
        const val EXTRA_NOTE_ID = "extra_note_id"
        private const val STATE_NOTE_ID = "state_note_id"

        private const val SAVE_DEBOUNCE_MS = 500L
        private const val SAVED_VISIBLE_MS = 1500L

        private const val MENU_PIN = 1
        private const val MENU_SHARE = 2
        private const val MENU_ARCHIVE = 3
        private const val MENU_DELETE = 4
    }
}
