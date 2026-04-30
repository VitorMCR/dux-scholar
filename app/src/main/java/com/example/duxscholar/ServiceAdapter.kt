package com.example.duxscholar

import android.util.Base64
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.duxscholar.databinding.ItemServiceBinding

class ServiceAdapter(
    private val lista: List<InfoAcademica>,
    private val onClick: (InfoAcademica) -> Unit
) : RecyclerView.Adapter<ServiceAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemServiceBinding)
        : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemServiceBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount() = lista.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = lista[position]

        holder.binding.txtNome.text = item.name

        try {
            if (item.icon.isNotEmpty() && item.icon.contains(",")) {
                val base64String = item.icon.substringAfter(",")
                val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
                holder.binding.imgIcon.load(imageBytes)
            } else {
                // fallback caso não seja base64
                holder.binding.imgIcon.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        } catch (e: Exception) {
            // evita crash
            holder.binding.imgIcon.setImageResource(android.R.drawable.ic_menu_report_image)
        }

        holder.itemView.setOnClickListener {
            onClick(item)
        }
    }
}