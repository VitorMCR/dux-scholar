package com.example.duxscholar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class EditEntryAdapter(
    private val entries: MutableList<EditEntry>,
    private val listener: EntryInteractionListener,
    private val showCourseHours: Boolean = false
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    class EditEntryViewHolder(view: View, private val listener: EntryInteractionListener) : RecyclerView.ViewHolder(view) {
        val txtItemTitle : TextView = view.findViewById(R.id.txtItemtitle)
        val imgbtnItemEdit : ImageButton = view.findViewById(R.id.imgbtnItemEdit)
        val imgbtnItemDelete : ImageButton = view.findViewById(R.id.imgbtnItemDelete)
        val imgbtnItemCourseEditHours : ImageButton = view.findViewById(R.id.imgbtnItemCourseEditHours)
        val spcCourseEditHours : View = view.findViewById(R.id.spcCourseEditHours)

        init {
            imgbtnItemEdit.setOnClickListener {
                val position = absoluteAdapterPosition
                if (position != RecyclerView.NO_POSITION) listener.onEditClick(position)
            }

            imgbtnItemDelete.setOnClickListener {
                val position = absoluteAdapterPosition
                if (position != RecyclerView.NO_POSITION) listener.onDeleteClick(position)
            }

            imgbtnItemCourseEditHours.setOnClickListener {
                val position = absoluteAdapterPosition
                if (position != RecyclerView.NO_POSITION) listener.onCourseHoursClick(position)
            }
        }
    }

    interface EntryInteractionListener {
        fun onEditClick(position: Int)
        fun onDeleteClick(position: Int)
        fun onCourseHoursClick(position: Int)
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
            if (showCourseHours) {
                holder.txtItemTitle.layoutParams.width = (220 * holder.itemView.resources.displayMetrics.density).toInt()
                holder.imgbtnItemCourseEditHours.visibility = View.VISIBLE
                holder.spcCourseEditHours.visibility = View.VISIBLE
            } else {
                holder.imgbtnItemCourseEditHours.visibility = View.GONE
                holder.spcCourseEditHours.visibility = View.GONE
            }
        }
    }

    override fun getItemCount() = entries.size

}