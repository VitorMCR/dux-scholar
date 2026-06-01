package com.example.duxscholar

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.core.graphics.toColorInt

class EditCourseScheduleActivity : AppCompatActivity() {

    private lateinit var llSemesterSelectors: LinearLayout
    private lateinit var llDailyTablesContainer: LinearLayout
    private lateinit var txtHorarioCursoNome: TextView
    private lateinit var btnSaveHorario: Button
    private lateinit var imgbtnVoltarHorario: ImageView

    private var courseId: String = ""
    private var courseName: String = ""
    private var courseShift: String = ""
    private var courseDuration: Int = 0
    private var selectedSemester: Int = 1

    private val databaseReference = FirebaseDatabase.getInstance()

    private val allDisciplinesMap = mutableMapOf<String, DisciplinaEntry>()
    // Lista filtrada de disciplinas para o turno do curso
    private val filteredDisciplines = mutableListOf<DisciplinaEntry>()
    
    private val allSchedules = mutableMapOf<String, Map<String, Map<String, List<String>>>>()
    
    // Map da seleção atual: número do semestre, (dia da semana, (índice, ID da disciplina))
    private val currentSelections = mutableMapOf<Int, MutableMap<String, MutableMap<Int, String>>>()

    data class DisciplinaEntry(val id: String, val name: String, val professorId: String, val shift: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_edit_course_schedule)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        courseId = intent.getStringExtra("COURSE_ID") ?: ""
        courseName = intent.getStringExtra("COURSE_NAME") ?: ""

        llSemesterSelectors = findViewById(R.id.llSemesterSelectors)
        llDailyTablesContainer = findViewById(R.id.llDailyTablesContainer)
        txtHorarioCursoNome = findViewById(R.id.txtHorarioCursoNome)
        btnSaveHorario = findViewById(R.id.btnSaveHorario)
        imgbtnVoltarHorario = findViewById(R.id.imgbtnVoltarHorario)

        txtHorarioCursoNome.text = courseName

        imgbtnVoltarHorario.setOnClickListener {
            finish()
        }

        loadData()

        btnSaveHorario.setOnClickListener {
            saveSchedule()
        }
    }

    private fun loadData() {
        lifecycleScope.launch {
            try {
                val courseSnapshot = databaseReference.getReference("cursos").child(courseId).get().await()
                courseShift = courseSnapshot.child("shift").value.toString()
                courseDuration = (courseSnapshot.child("duration").value as? Long)?.toInt() ?: 0
                
                val discSnapshot = databaseReference.getReference("disciplinas").get().await()
                allDisciplinesMap.clear()
                filteredDisciplines.clear()
                filteredDisciplines.add(DisciplinaEntry("none", "Nenhuma", "", ""))
                
                for (child in discSnapshot.children) {
                    val id = child.key ?: continue
                    val name = child.child("name").value.toString()
                    val profId = child.child("professor").value.toString()
                    val shift = child.child("shift").value.toString()
                    val entry = DisciplinaEntry(id, name, profId, shift)
                    allDisciplinesMap[id] = entry
                    if (shift == courseShift) {
                        filteredDisciplines.add(entry)
                    }
                }

                // Carregar todas as grades horárias para conferir conflitos
                val schedulesSnapshot = databaseReference.getReference("horarios_aula").get().await()
                allSchedules.clear()
                for (courseChild in schedulesSnapshot.children) {
                    val cId = courseChild.key ?: continue
                    val semMap = mutableMapOf<String, Map<String, List<String>>>()
                    for (semChild in courseChild.children) {
                        val semKey = semChild.key ?: continue
                        val dayMap = mutableMapOf<String, List<String>>()
                        for (dayChild in semChild.children) {
                            val dayKey = dayChild.key ?: continue
                            val slots = dayChild.children.map { it.value.toString() }
                            dayMap[dayKey] = slots
                        }
                        semMap[semKey] = dayMap
                    }
                    allSchedules[cId] = semMap
                }

                // Carregar grade horária desse curso (se já existir)
                val mySchedule = allSchedules[courseId]
                if (mySchedule != null) {
                    for ((semKey, dayMap) in mySchedule) {
                        val semInt = semKey.toIntOrNull() ?: continue
                        val semSelections = mutableMapOf<String, MutableMap<Int, String>>()
                        for ((dayKey, slots) in dayMap) {
                            val daySelections = mutableMapOf<Int, String>()
                            slots.forEachIndexed { index, discId ->
                                daySelections[index] = discId
                            }
                            semSelections[dayKey] = daySelections
                        }
                        currentSelections[semInt] = semSelections
                    }
                }

                setupSemesterSelectors()
                updateTable()
            } catch (e: Exception) {
                Log.e("HorarioActivity", "Error loading data", e)
                Toast.makeText(this@EditCourseScheduleActivity, "Erro ao carregar dados", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupSemesterSelectors() {
        llSemesterSelectors.removeAllViews()
        for (i in 1..courseDuration) {
            val btn = Button(this).apply {
                text = "$i° Semestre"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(8, 0, 8, 0)
                    setPadding(40, 40, 40, 40)
                }
                setOnClickListener {
                    selectedSemester = i
                    updateTable()
                    updateSelectorStyles()
                }
            }
            llSemesterSelectors.addView(btn)
        }
        updateSelectorStyles()
    }

    private fun updateSelectorStyles() {
        for (i in 0 until llSemesterSelectors.childCount) {
            val btn = llSemesterSelectors.getChildAt(i) as Button
            if (i + 1 == selectedSemester) {
                btn.alpha = 1.0f
                btn.setBackgroundColor(getColor(R.color.editActivityButtonColor))
            } else {
                btn.alpha = 0.5f
                btn.setBackgroundColor(getColor(android.R.color.darker_gray))
            }
        }
    }

    private fun updateTable() {
        llDailyTablesContainer.removeAllViews()

        val days = listOf("Segunda-Feira", "Terça-Feira", "Quarta-Feira", "Quinta-Feira", "Sexta-Feira")
        val slots = when (courseShift) {
            "Matutino" -> listOf("07:40-09:20", "09:30-11:20", "11:30-13:00")
            "Vespertino" -> listOf("14:00-15:40", "15:50-17:30")
            "Noturno" -> listOf("19:00-20:40", "20:50-22:30")
            else -> emptyList()
        }

        days.forEach { day ->
            val txtDayTitle = TextView(this).apply {
                text = day
                textSize = 18f
                setTypeface(null, Typeface.BOLD)
                setPadding(16, 24, 16, 8)
                setTextColor(getColor(R.color.textColor))
            }
            llDailyTablesContainer.addView(txtDayTitle)

            val table = TableLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setPadding(8, 0, 8, 16)
                setColumnStretchable(1, true)
                setColumnShrinkable(1, true)
                clipChildren = true
            }

            val headerRow = TableRow(this).apply {
                setBackgroundColor("#DDDDDD".toColorInt())
                setPadding(4, 4, 4, 4)
            }
            
            headerRow.addView(TextView(this).apply {
                text = "Hora"
                gravity = Gravity.CENTER
                setPadding(16, 16, 16, 16)
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.BLACK)
            })
            headerRow.addView(TextView(this).apply {
                text = "Disciplina"
                gravity = Gravity.CENTER
                setPadding(16, 16, 16, 16)
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.BLACK)
            })
            table.addView(headerRow)

            slots.forEachIndexed { slotIndex, slotLabel ->
                val row = TableRow(this).apply {
                    clipChildren = true
                }

                val txtTime = TextView(this).apply {
                    text = slotLabel
                    gravity = Gravity.CENTER
                    setPadding(16, 16, 16, 16)
                    setTextColor(getColor(R.color.textColor))
                    layoutParams = TableRow.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                        gravity = Gravity.CENTER
                    }
                }
                row.addView(txtTime)

                val textInputLayout = layoutInflater.inflate(R.layout.item_courseschedule_dropdown, row, false) as TextInputLayout
                textInputLayout.layoutParams = TableRow.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    gravity = Gravity.CENTER_VERTICAL
                }
                textInputLayout.hint = null

                val autoCompleteTextView = textInputLayout.findViewById<MaterialAutoCompleteTextView>(R.id.grade_autocomplete)
                
                val displayNames = filteredDisciplines.map {
                    if (it.name.length > 35) it.name.take(32) + "..." else it.name
                }
                val adapter = ArrayAdapter(this, R.layout.item_dropdown, displayNames)
                autoCompleteTextView.setAdapter(adapter)

                val selectedDiscId = currentSelections[selectedSemester]?.get(day)?.get(slotIndex) ?: "none"
                val selectionPos = filteredDisciplines.indexOfFirst { it.id == selectedDiscId }.coerceAtLeast(0)
                autoCompleteTextView.setText(displayNames[selectionPos], false)

                autoCompleteTextView.setOnItemClickListener { _, _, position, _ ->
                    val discipline = filteredDisciplines[position]
                    if (discipline.id != "none") {
                        if (hasConflict(selectedSemester, day, slotIndex, discipline)) {
                            Toast.makeText(this@EditCourseScheduleActivity, "Conflito: Professor(a) já possui aula nesse horário!", Toast.LENGTH_LONG).show()
                            autoCompleteTextView.setText(displayNames[0], false)
                            updateSelection(selectedSemester, day, slotIndex, "none")
                            return@setOnItemClickListener
                        }
                    }
                    updateSelection(selectedSemester, day, slotIndex, discipline.id)
                }
                row.addView(textInputLayout)
                table.addView(row)
            }
            
            llDailyTablesContainer.addView(table)
        }
    }

    private fun updateSelection(semester: Int, day: String, slotIndex: Int, disciplineId: String) {
        val semMap = currentSelections.getOrPut(semester) { mutableMapOf() }
        val dayMap = semMap.getOrPut(day) { mutableMapOf() }
        dayMap[slotIndex] = disciplineId
    }

    private fun hasConflict(semester: Int, day: String, slotIndex: Int, discipline: DisciplinaEntry): Boolean {
        val professorId = discipline.professorId
        if (professorId.isEmpty()) return false

        // Primeiro, checa outros cursos/semestres já no banco
        for ((cId, courseSchedule) in allSchedules) {
            if (cId == courseId) continue 
            for ((_, dayMap) in courseSchedule) {
                val slots = dayMap[day]
                if (slots != null && slotIndex < slots.size) {
                    val discIdAtSlot = slots[slotIndex]
                    val profAtSlot = allDisciplinesMap[discIdAtSlot]?.professorId
                    if (profAtSlot == professorId) return true
                }
            }
        }

        // Depois, checa as escolhas no mesmo curso (outros semestres)
        for ((semInt, semMap) in currentSelections) {
            if (semInt == semester) continue 
            val discIdAtSlot = semMap[day]?.get(slotIndex)
            if (discIdAtSlot != null && discIdAtSlot != "none") {
                val profAtSlot = allDisciplinesMap[discIdAtSlot]?.professorId
                if (profAtSlot == professorId) return true
            }
        }

        return false
    }

    private fun saveSchedule() {
        lifecycleScope.launch {
            try {
                val saveMap = mutableMapOf<String, Any>()
                for ((semInt, semMap) in currentSelections) {
                    val dayMapSave = mutableMapOf<String, List<String>>()
                    for ((day, slotMap) in semMap) {
                        val slotList = mutableListOf<String>()
                        val maxSlot = when (courseShift) {
                            "Matutino" -> 2
                            else -> 1
                        }
                        for (i in 0..maxSlot) {
                            slotList.add(slotMap[i] ?: "none")
                        }
                        dayMapSave[day] = slotList
                    }
                    saveMap[semInt.toString()] = dayMapSave
                }

                databaseReference.getReference("horarios_aula").child(courseId).setValue(saveMap).await()
                Snackbar.make(llDailyTablesContainer, "Grade horária salva com sucesso!", Snackbar.LENGTH_LONG).show()
                
                // Atualiza o cache local
                val newCourseSchedule = mutableMapOf<String, Map<String, List<String>>>()
                for ((sem, days) in saveMap) {
                    @Suppress("UNCHECKED_CAST")
                    newCourseSchedule[sem] = days as Map<String, List<String>>
                }
                allSchedules[courseId] = newCourseSchedule

            } catch (e: Exception) {
                Log.e("HorarioActivity", "Error ao salvar grade", e)
                Toast.makeText(this@EditCourseScheduleActivity, "Erro ao salvar grade.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
