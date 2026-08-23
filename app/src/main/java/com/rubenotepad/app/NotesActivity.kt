package com.rubenotepad.app

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.rubenotepad.app.data.AppPrefs
import com.rubenotepad.app.data.Note
import com.rubenotepad.app.data.NoteRepository

/**
 * Main notes screen: brown app bar, yellow sticky-note cards, search,
 * sort, list/grid layout toggle, archived filter, and a floating "+" button.
 */
class NotesActivity : AppCompatActivity() {

    private lateinit var prefs: AppPrefs
    private lateinit var repository: NoteRepository
    private lateinit var adapter: NoteAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyState: View

    private var currentQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notes)

        prefs = AppPrefs(this)
        repository = NoteRepository.get(this)

        val toolbar = findViewById<MaterialToolbar>(R.id.notesToolbar)
        setSupportActionBar(toolbar)

        recyclerView = findViewById(R.id.notesRecycler)
        emptyState = findViewById(R.id.emptyState)

        adapter = NoteAdapter(
            onNoteClick = { note -> openEditor(note.id) },
            onNoteLongClick = { note -> showQuickActions(note) },
            onPinClick = { note ->
                repository.update(note.copy(pinned = !note.pinned))
                reloadNotes()
            }
        )
        applyLayoutManager()
        // Restore list position across rotation.
        savedInstanceState?.getParcelable<Parcelable>(STATE_LIST_STATE)?.let {
            recyclerView.layoutManager?.onRestoreInstanceState(it)
        }
        recyclerView.adapter = adapter
        // Restore an active search across rotation.
        currentQuery = savedInstanceState?.getString(STATE_QUERY).orEmpty()

        findViewById<FloatingActionButton>(R.id.fabNewNote).setOnClickListener {
            openEditor(0L)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_QUERY, currentQuery)
        recyclerView.layoutManager?.let { outState.putParcelable(STATE_LIST_STATE, it.onSaveInstanceState()) }
    }

    override fun onResume() {
        super.onResume()
        // Reload so edits made in the editor (or deletions) are reflected.
        reloadNotes()
    }

    // ------------------------------------------------------------------
    // Data loading / search
    // ------------------------------------------------------------------

    private fun reloadNotes() {
        val notes = repository.list(
            includeArchived = prefs.showArchived,
            searchQuery = currentQuery,
            sortAlphabetical = prefs.sortAlphabetical
        )
        adapter.submitList(notes)
        emptyState.isVisible = notes.isEmpty()
        recyclerView.isVisible = notes.isNotEmpty()
        updateEmptyStateText()
    }

    private fun updateEmptyStateText() {
        val titleView = findViewById<com.google.android.material.textview.MaterialTextView>(R.id.tvEmptyTitle)
        val subtitleView = findViewById<com.google.android.material.textview.MaterialTextView>(R.id.tvEmptySubtitle)
        if (currentQuery.isNotBlank()) {
            titleView.setText(R.string.empty_search_title)
            subtitleView.setText(R.string.empty_search_subtitle)
        } else if (prefs.showArchived) {
            titleView.setText(R.string.empty_archive_title)
            subtitleView.setText(R.string.empty_archive_subtitle)
        } else {
            titleView.setText(R.string.empty_notes_title)
            subtitleView.setText(R.string.empty_notes_subtitle)
        }
    }

    /**
     * Adapts to any display size: phones stay single-column (like the reference),
     * tablets / landscape get extra columns. The grid preference adds one more.
     */
    private fun applyLayoutManager() {
        val wideScreen = resources.configuration.screenWidthDp >= TABLET_MIN_WIDTH_DP
        val columns = when {
            prefs.gridView && wideScreen -> 3
            prefs.gridView -> 2
            wideScreen -> 2
            else -> 1
        }
        recyclerView.layoutManager = GridLayoutManager(this, columns)
    }

    private fun openEditor(noteId: Long) {
        val intent = Intent(this, EditorActivity::class.java)
        intent.putExtra(EditorActivity.EXTRA_NOTE_ID, noteId)
        startActivity(intent)
    }

    // ------------------------------------------------------------------
    // Long-press quick actions (mirrors the reference overflow menu)
    // ------------------------------------------------------------------

    private fun showQuickActions(note: Note) {
        val items = mutableListOf(
            getString(if (note.pinned) R.string.action_unpin else R.string.action_pin),
            getString(if (note.archived) R.string.action_unarchive else R.string.action_archive),
            getString(R.string.action_share),
            getString(R.string.action_delete)
        )
        MaterialAlertDialogBuilder(this)
            .setTitle(note.displayTitle)
            .setItems(items.toTypedArray()) { _, which ->
                when (which) {
                    0 -> {
                        repository.update(note.copy(pinned = !note.pinned))
                        reloadNotes()
                    }
                    1 -> {
                        repository.update(note.copy(archived = !note.archived))
                        reloadNotes()
                    }
                    2 -> shareNote(note)
                    3 -> confirmDelete(listOf(note))
                }
            }
            .show()
    }

    private fun confirmDelete(notes: List<Note>) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.delete_confirm_title)
            .setMessage(
                resources.getQuantityString(
                    R.plurals.delete_confirm_message,
                    notes.size,
                    notes.size
                )
            )
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                notes.forEach { repository.delete(it.id) }
                reloadNotes()
            }
            .show()
    }

    private fun shareNote(note: Note) {
        val text = buildString {
            append(note.title.ifBlank { getString(R.string.untitled_note) })
            append("\n\n")
            append(note.content)
        }
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, note.title)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(send, getString(R.string.action_share)))
    }

    // ------------------------------------------------------------------
    // Toolbar menu: search, grid/list, sort, archived filter, about
    // ------------------------------------------------------------------

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_notes, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView
        searchView.queryHint = getString(R.string.search_hint)
        // Restore an active query after the options menu is rebuilt.
        if (currentQuery.isNotBlank()) {
            searchView.setQuery(currentQuery, false)
            searchItem.expandActionView()
        }
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = true
            override fun onQueryTextChange(newText: String?): Boolean {
                currentQuery = newText.orEmpty()
                reloadNotes()
                return true
            }
        })

        updateMenuIcons(menu)
        return true
    }

    private fun updateMenuIcons(menu: Menu) {
        menu.findItem(R.id.action_toggle_layout).setIcon(
            if (prefs.gridView) R.drawable.ic_view_list else R.drawable.ic_view_grid
        ).setTitle(
            if (prefs.gridView) R.string.action_list_view else R.string.action_grid_view
        )
        menu.findItem(R.id.action_sort).setTitle(
            if (prefs.sortAlphabetical) R.string.action_sort_by_recent else R.string.action_sort_az
        )
        menu.findItem(R.id.action_show_archived).isChecked = prefs.showArchived
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_toggle_layout -> {
                prefs.gridView = !prefs.gridView
                applyLayoutManager()
                invalidateOptionsMenu()
                true
            }
            R.id.action_sort -> {
                prefs.sortAlphabetical = !prefs.sortAlphabetical
                reloadNotes()
                invalidateOptionsMenu()
                true
            }
            R.id.action_show_archived -> {
                prefs.showArchived = !prefs.showArchived
                reloadNotes()
                invalidateOptionsMenu()
                true
            }
            R.id.action_about -> {
                MaterialAlertDialogBuilder(this)
                    .setTitle(OwnerConfig.APP_NAME)
                    .setMessage(getString(R.string.about_message))
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        private const val STATE_QUERY = "state_query"
        private const val STATE_LIST_STATE = "state_list_state"
        private const val TABLET_MIN_WIDTH_DP = 600
    }
}
