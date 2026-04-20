package com.example.duxscholar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class EditEntryAdapter(private val entries: MutableList<EditEntry>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    class EditEntryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtItemTitle : TextView = view.findViewById(R.id.txtItemtitle)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_editlist_entry, parent, false)
        return EditEntryViewHolder(view)
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