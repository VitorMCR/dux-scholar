package com.example.duxscholar

import android.view.*
import android.widget.TextView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import android.view.Gravity
import android.animation.ObjectAnimator
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator


class MessageAdapter(private val messages: MutableList<Message>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val TYPE_MESSAGE = 0
    private val TYPE_TYPING = 1

    class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textMessage: TextView = view.findViewById(R.id.textMessage)
        val textTimestamp: TextView = view.findViewById(R.id.textTimestamp)
        val imageBot: ImageView = view.findViewById(R.id.imageBot)
        val container: LinearLayout = view.findViewById(R.id.container)
    }

    class TypingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dot1: View = view.findViewById(R.id.dot1)
        val dot2: View = view.findViewById(R.id.dot2)
        val dot3: View = view.findViewById(R.id.dot3)
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isLoading) TYPE_TYPING else TYPE_MESSAGE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_TYPING) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chatbot_typing, parent, false)
            TypingViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chatbot_message, parent, false)
            MessageViewHolder(view)
        }
    }

    override fun getItemCount() = messages.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        val message = messages[position]

        if (holder is TypingViewHolder) {

            fun animateDot(view: View, delay: Long) {
                val moveUp = ObjectAnimator.ofFloat(view, "translationY", 0f, -15f, 0f)
                moveUp.duration = 850
                moveUp.startDelay = delay
                moveUp.interpolator = AccelerateDecelerateInterpolator()
                moveUp.repeatCount = ObjectAnimator.INFINITE
                moveUp.start()
            }

            animateDot(holder.dot1, 0)
            animateDot(holder.dot2, 100)
            animateDot(holder.dot3, 200)

        } else if (holder is MessageViewHolder) {

            holder.textMessage.text = message.text
            holder.textTimestamp.text = message.timestamp

            val params = holder.container.layoutParams as FrameLayout.LayoutParams

            if (message.isUser) {
                params.gravity = Gravity.END
                holder.textMessage.setBackgroundResource(R.drawable.bg_chatbot_message_user)
                holder.imageBot.visibility = View.GONE
                holder.textTimestamp.gravity = Gravity.END
                holder.itemView.setOnLongClickListener(null)
            } else {
                params.gravity = Gravity.START
                holder.textMessage.setBackgroundResource(R.drawable.bg_chatbot_message_bot)
                holder.imageBot.visibility = View.VISIBLE
                holder.textTimestamp.gravity = Gravity.START

                // Long press copia a mensagem do bot
                holder.itemView.setOnLongClickListener {
                    (it.context as? ChatbotActivity)?.copyMessageToClipboard(message.text)
                    true
                }
            }

            holder.container.layoutParams = params

            // Animação de entrada: slide de baixo + fade in
            holder.itemView.translationY = 40f
            holder.itemView.alpha = 0f
            holder.itemView.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(250)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }
}