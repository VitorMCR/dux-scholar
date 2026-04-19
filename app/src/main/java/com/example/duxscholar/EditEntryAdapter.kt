package com.example.duxscholar

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class EditEntryAdapter(private val entries: MutableList<EditEntry>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    class EditEntryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtItemTitle : TextView = view.findViewById(R.id.txtItemtitle)
        val frmEditableItem : FrameLayout = view.findViewById(R.id.frmEditableItem)
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

            val params = holder.frmEditableItem.layoutParams as FrameLayout.LayoutParams
            params.gravity = Gravity.CENTER_HORIZONTAL
            holder.frmEditableItem.layoutParams = params
        }
    }

    override fun getItemCount() = entries.size

}