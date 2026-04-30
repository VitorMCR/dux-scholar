package com.example.duxscholar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class NoteAdapter(
    private var notes: List<Note>, // Lista que o restante do código acessa
    private val onItemClick: (Note) -> Unit // Função de clique para editar/remover
) : RecyclerView.Adapter<NoteAdapter.NoteVH>() {

    class NoteVH(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(R.id.textTitle)
        val desc: TextView = v.findViewById(R.id.textDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteVH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_note, parent, false)
        return NoteVH(v)
    }

    override fun onBindViewHolder(holder: NoteVH, position: Int) {
        val note = notes[position]
        holder.title.text = note.title
        holder.desc.text = note.description

        // Quando clicar no item da lista, dispara o diálogo de opções
        holder.itemView.setOnClickListener {
            onItemClick(note)
        }
    }

    override fun getItemCount() = notes.size

    fun updateData(newNotes: List<Note>) {
        this.notes = newNotes
        notifyDataSetChanged()
    }
    //a
}