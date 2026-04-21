package com.example.duxscholar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class EditEntryAdapter(private val entries: MutableList<EditEntry>, private val listener: EntryInteractionListener) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    class EditEntryViewHolder(view: View, private val listener: EntryInteractionListener) : RecyclerView.ViewHolder(view) {
        val txtItemTitle : TextView = view.findViewById(R.id.txtItemtitle)
        val imgbtnItemEdit : ImageButton = view.findViewById(R.id.imgbtnItemEdit)
        val imgbtnItemDelete : ImageButton = view.findViewById(R.id.imgbtnItemDelete)

        init {
            imgbtnItemEdit.setOnClickListener {
                val position = absoluteAdapterPosition
                if (position != RecyclerView.NO_POSITION) listener.onEditClick(position)
            }

            imgbtnItemDelete.setOnClickListener {
                val position = absoluteAdapterPosition
                if (position != RecyclerView.NO_POSITION) listener.onDeleteClick(position)
            }
        }
    }

    interface EntryInteractionListener {
        fun onEditClick(position: Int)
        fun onDeleteClick(position: Int)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_editlist_entry, parent, false)
        return EditEntryViewHolder(view, listener)
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        val entry = entries[position]

        if (holder is EditEntryViewHolder) {
            holder.txtItemTitle.text = entry.title
        }
    }

    override fun getItemCount() = entries.size

}