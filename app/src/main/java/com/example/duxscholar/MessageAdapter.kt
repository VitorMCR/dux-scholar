package com.example.duxscholar

import android.view.*
import android.widget.TextView
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import android.widget.LinearLayout
import android.view.Gravity
import android.widget.FrameLayout

class MessageAdapter(private val messages: MutableList<Message>) :
    RecyclerView.Adapter<MessageAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textMessage: TextView = view.findViewById(R.id.textMessage)
        val imageBot: ImageView = view.findViewById(R.id.imageBot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = messages.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val message = messages[position]

        holder.textMessage.text = message.text

        val container = holder.itemView.findViewById<LinearLayout>(R.id.container)
        val params = container.layoutParams as FrameLayout.LayoutParams

        if (message.isUser) {
            params.gravity = Gravity.END

            holder.textMessage.setBackgroundResource(R.drawable.bg_message_user)
            holder.imageBot.visibility = View.GONE

        } else {
            params.gravity = Gravity.START

            holder.textMessage.setBackgroundResource(R.drawable.bg_message_bot)
            holder.imageBot.visibility = View.VISIBLE
        }

        container.layoutParams = params
    }
}