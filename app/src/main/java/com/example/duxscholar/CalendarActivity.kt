package com.example.duxscholar

import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.MaterialCalendarView

class CalendarActivity : AppCompatActivity() {

    private val notes = mutableMapOf<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_calendar)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val calendarView = findViewById<MaterialCalendarView>(R.id.calendarView)
        val textViewNotes = findViewById<TextView>(R.id.TextView)

        calendarView.state().edit()
            .setMinimumDate(CalendarDay.from(2020, 1, 1))
            .setMaximumDate(CalendarDay.from(2030, 12, 31))
            .commit()

        calendarView.setOnDateChangedListener { _, date, selected ->
            val keyDate = "${date.year}-${date.month}-${date.day}"
            if (selected) {
                val texto = notes[keyDate] ?: "Nenhum lembrete para esse dia!"
                textViewNotes.text = texto
            }
        }
    }
    private fun showAddDialogue(keyDate: String, showTextView: TextView) {
        val input = EditText(this)

        AlertDialog.Builder(this)
            .setTitle("Adicionar Lembrete")
            .setMessage("O que você quer adicionar nessa data?")
            .setView(input)
            .setPositiveButton("Salvar") {_, _ ->
                val newText = input.text.toString()
                if (newText.isNotEmpty()) {
                    notes[keyDate] = newText
                    showTextView.text = newText
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}