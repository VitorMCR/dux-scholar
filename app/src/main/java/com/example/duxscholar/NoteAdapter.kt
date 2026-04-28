package com.example.duxscholar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class NoteAdapter(private var noteList: List<Note>) : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

    // 1. ViewHolder: segura as referências dos componentes de cada item
    class NoteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.textTitle)
        val desc: TextView = view.findViewById(R.id.textDescription)
    }

    // 2. Infla o layout do item (item_note.xml)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view)
    }

    // 3. Coloca os dados na tela
    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = noteList[position]
        holder.title.text = note.title
        holder.desc.text = note.description
    }

    override fun getItemCount(): Int = noteList.size

    // Função extra para atualizar a lista quando mudar o dia no calendário
    fun updateData(newNotes: List<Note>) {
        this.noteList = newNotes
        notifyDataSetChanged()
    }
}