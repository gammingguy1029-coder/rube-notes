package com.rubenotepad.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rubenotepad.app.data.Note

/**
 * Yellow sticky-note cards for the notes list (see reference design).
 */
class NoteAdapter(
    private val onNoteClick: (Note) -> Unit,
    private val onNoteLongClick: (Note) -> Unit,
    private val onPinClick: (Note) -> Unit
) : ListAdapter<Note, NoteAdapter.NoteViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tvNoteTitle)
        private val tvDate: TextView = itemView.findViewById(R.id.tvNoteDate)
        private val btnPin: ImageButton = itemView.findViewById(R.id.btnPin)
        private var current: Note? = null

        init {
            itemView.setOnClickListener { current?.let(onNoteClick) }
            itemView.setOnLongClickListener {
                current?.let(onNoteLongClick)
                true
            }
            btnPin.setOnClickListener { current?.let(onPinClick) }
        }

        fun bind(note: Note) {
            current = note
            tvTitle.text = note.displayTitle
            tvDate.text = Note.relativeTime(note.updatedAt)
            btnPin.isVisible = note.pinned
            btnPin.contentDescription =
                itemView.context.getString(R.string.action_unpin)
            itemView.contentDescription = itemView.context.getString(
                R.string.note_card_a11y, note.displayTitle
            )
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Note>() {
            override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean =
                oldItem == newItem
        }
    }
}
