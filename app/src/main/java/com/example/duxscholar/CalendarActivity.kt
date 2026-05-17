package com.example.duxscholar

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import com.prolificinteractive.materialcalendarview.spans.DotSpan
import java.util.UUID


class CalendarActivity : AppCompatActivity() {

    private lateinit var databaseReference: DatabaseReference
    private lateinit var calendarView: MaterialCalendarView
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: NoteAdapter
    private lateinit var emptyStateText: TextView
    private lateinit var btnAdd: ImageButton
    //a

    // Lista local para o RecyclerView
    private var currentNotesList = mutableListOf<Note>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendar)

        // Inicializar Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference("anotacoes")

        // Inicializar Views
        calendarView = findViewById(R.id.calendarView)
        recyclerView = findViewById(R.id.recyclerViewNotes)
        emptyStateText = findViewById(R.id.emptyStateText) // Usando o ID do seu XML
        btnAdd = findViewById<ImageButton>(R.id.btnAdd)

        // Configurar RecyclerView
        // Dentro do onCreate da CalendarActivity
        adapter = NoteAdapter(currentNotesList) { note ->
            // Chama a função que criamos para mostrar Editar/Excluir
            showOptionsDialog(note)
        }
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Configuração do Calendário
        setupCalendar()

        // Carregar bolinhas (indicadores visuais)
        fetchAllNotesForDecorators()

        // Listener de clique na data
        calendarView.setOnDateChangedListener { _, date, selected ->
            if (selected) {
                fetchNotesForDate(date)
            }
        }

        // Clique no texto vazio para adicionar nova nota
        btnAdd.setOnClickListener {
            val selectedDate = calendarView.selectedDate ?: CalendarDay.today()
            showAddDialogue(selectedDate)
        }
    }

    private fun showOptionsDialog(note: Note) {
        val options = arrayOf("Editar", "Excluir")
        AlertDialog.Builder(this)
            .setTitle("O que deseja fazer?")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditDialogue(note) // Editar
                    1 -> confirmDelete(note)    // Excluir
                }
            }
            .show()
    }

    // --- FUNÇÃO REMOVER ---
    private fun confirmDelete(note: Note) {
        AlertDialog.Builder(this)
            .setTitle("Excluir Anotação")
            .setMessage("Tem certeza que deseja apagar esta nota?")
            .setPositiveButton("Sim") { _, _ ->
                note.id?.let { id ->
                    databaseReference.child(id).removeValue()
                        .addOnSuccessListener {
                            Toast.makeText(this, "Removido com sucesso!", Toast.LENGTH_SHORT).show()
                                // A lista atualizará sozinha pelo addValueEventListener
                        }
                }
            }
            .setNegativeButton("Não", null)
            .show()
    }

    // --- FUNÇÃO EDITAR ---
    private fun showEditDialogue(note: Note) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_note, null)
        val editTitle = dialogView.findViewById<EditText>(R.id.editTitle)
        val editDesc = dialogView.findViewById<EditText>(R.id.editDescription)

        // Preenche os campos com os dados atuais
        editTitle.setText(note.title)
        editDesc.setText(note.description)

        AlertDialog.Builder(this)
            .setTitle("Editar Anotação")
            .setView(dialogView)
            .setPositiveButton("Salvar Alterações") { _, _ ->
                val newTitle = editTitle.text.toString()
                val newDesc = editDesc.text.toString()

                if (newTitle.isNotEmpty()) {
                    val updatedNote = note.copy(title = newTitle, description = newDesc)
                    note.id?.let { id ->
                        databaseReference.child(id).setValue(updatedNote)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Atualizado!", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    private fun setupCalendar() {
        calendarView.state().edit()
            .setMinimumDate(CalendarDay.from(2020, 1, 1))
            .setMaximumDate(CalendarDay.from(2030, 12, 31))
            .commit()

        // Seleciona o dia de hoje por padrão e carrega as notas
        calendarView.setSelectedDate(CalendarDay.today())
        fetchNotesForDate(CalendarDay.today())
    }

    // Busca as notas de um dia específico no Firebase
    private fun fetchNotesForDate(date: CalendarDay) {
        val key = "${date.year}-${date.month}-${date.day}"

        databaseReference.orderByChild("dateKey").equalTo(key)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    currentNotesList.clear()
                    for (noteSnapshot in snapshot.children) {
                        val note = noteSnapshot.getValue(Note::class.java)
                        note?.let { currentNotesList.add(it) } // Adiciona na mesma lista
                    }
                    adapter.notifyDataSetChanged()

                    if (currentNotesList.isEmpty()) {
                        emptyStateText.text = "Nenhum lembrete. Toque para adicionar (+)"
                        recyclerView.visibility = View.GONE
                    } else {
                        emptyStateText.text = "Lembretes do dia:"
                        recyclerView.visibility = View.VISIBLE
                        adapter.notifyDataSetChanged()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@CalendarActivity, "Erro ao carregar", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // Busca todas as notas apenas para marcar as bolinhas no calendário
    private fun fetchAllNotesForDecorators() {
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val datesWithNotes = HashSet<CalendarDay>()
                for (noteSnapshot in snapshot.children) {
                    val note = noteSnapshot.getValue(Note::class.java)
                    note?.let {
                        val parts = it.dateKey.split("-")
                        val day = CalendarDay.from(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
                        datesWithNotes.add(day)
                    }
                }
                calendarView.removeDecorators()
                calendarView.addDecorator(EventDecorator(Color.parseColor("#8218f2"), datesWithNotes))
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun showAddDialogue(date: CalendarDay) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_note, null)
        val editTitle = dialogView.findViewById<EditText>(R.id.editTitle)
        val editDesc = dialogView.findViewById<EditText>(R.id.editDescription)

        AlertDialog.Builder(this)
            .setTitle("Adicionar para ${date.day.toString().padStart(2, '0')}/${date.month.toString().padStart(2, '0')}")
            .setView(dialogView)
            .setPositiveButton("Salvar") { _, _ ->
                val title = editTitle.text.toString()
                val desc = editDesc.text.toString()
                val dateKey = "${date.year}-${date.month}-${date.day}"

                if (title.isNotEmpty()) {
                    val noteId = databaseReference.push().key ?: UUID.randomUUID().toString()
                    val newNote = Note(noteId, title, desc, dateKey)

                    databaseReference.child(noteId).setValue(newNote)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Salvo!", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}