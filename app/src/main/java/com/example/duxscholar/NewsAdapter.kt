package com.example.duxscholar

import android.util.Base64
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.duxscholar.databinding.ItemNewsBinding

class NewsAdapter(
    private val lista: List<Noticia>,
    private val onClick: (Noticia) -> Unit
) : RecyclerView.Adapter<NewsAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemNewsBinding)
        : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNewsBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount() = lista.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = lista[position]

        holder.binding.txtNewTitle.text = item.name
        holder.binding.txtNewHeader.text = item.header
        holder.binding.txtNewDate.text = item.date

        try {
            if (item.image.isNotEmpty()) {
                val base64String = if (item.image.contains(",")) {
                    item.image.substringAfter(",")
                } else {
                    item.image
                }
                val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
                holder.binding.imgNewImage.load(imageBytes)
            } else {
                holder.binding.imgNewImage.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        } catch (_: Exception) {
            holder.binding.imgNewImage.setImageResource(android.R.drawable.ic_menu_report_image)
        }

        holder.itemView.setOnClickListener {
            onClick(item)
        }
    }
}