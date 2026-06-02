package com.example.duxscholar

import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class StudentScheduleActivity : AppCompatActivity() {
    private lateinit var llDaysContainer: LinearLayout
    private lateinit var auth: FirebaseAuth
    private val databaseReference = FirebaseDatabase.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_student_schedule)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        llDaysContainer = findViewById(R.id.llDaysContainer)

        findViewById<ImageButton>(R.id.imgbtnVoltarStuSched).setOnClickListener {
            finish()
        }

        loadSchedule()
    }

    private fun loadSchedule() {
        lifecycleScope.launch {
            try {
                val user = auth.currentUser ?: return@launch
                val studentSnapshot = databaseReference.getReference("alunos").child(user.uid).get().await()
                if (!studentSnapshot.exists()) return@launch

                val student = studentSnapshot.getValue(Aluno::class.java) ?: return@launch
                val courseSnapshot = databaseReference.getReference("cursos").child(student.curso).get().await()
                if (!courseSnapshot.exists()) return@launch

                val course = courseSnapshot.getValue(Curso::class.java) ?: return@launch
                val shift = course.shift
                val semester = student.semester

                // Mapeamento de ID para nome
                val disciplinesSnapshot = databaseReference.getReference("disciplinas").get().await()
                val disciplinesMap = mutableMapOf<String, String>()
                for (child in disciplinesSnapshot.children) {
                    val name = child.child("name").value?.toString() ?: ""
                    disciplinesMap[child.key ?: ""] = name
                }

                // Coletando horários de aula do aluno em específico
                val scheduleSnapshot = databaseReference.getReference("horarios_aula")
                    .child(student.curso)
                    .child(semester.toString())
                    .get().await()

                val days = listOf("Segunda-Feira", "Terça-Feira", "Quarta-Feira", "Quinta-Feira", "Sexta-Feira")
                val slotTimes = when (shift) {
                    "Matutino" -> listOf("07:40 - 09:20", "09:30 - 11:20", "11:30 - 13:00")
                    "Vespertino" -> listOf("14:00 - 15:40", "15:50 - 17:30")
                    "Noturno" -> listOf("19:00 - 20:40", "20:50 - 22:30")
                    else -> emptyList()
                }

                llDaysContainer.removeAllViews()

                for (dayName in days) {
                    val dayView = layoutInflater.inflate(R.layout.item_day_schedule, llDaysContainer, false)
                    dayView.findViewById<TextView>(R.id.txtDayName).text = dayName
                    val classesContainer = dayView.findViewById<LinearLayout>(R.id.llClassesContainer)
                    val txtNoClasses = dayView.findViewById<TextView>(R.id.txtNoClasses)

                    val daySnapshot = scheduleSnapshot.child(dayName)
                    
                    var hasClasses = false
                    if (daySnapshot.exists()) {
                        // Cada slot tem o ID da disciplina associada
                        val slotList = daySnapshot.children.map { it.value.toString() }
                        
                        slotList.forEachIndexed { index, discId ->
                            if (discId != "none" && index < slotTimes.size) {
                                hasClasses = true
                                val classText = "${slotTimes[index]}: ${disciplinesMap[discId] ?: discId}"
                                val classView = TextView(this@StudentScheduleActivity).apply {
                                    text = classText
                                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                                    setPadding(0, 8, 0, 8)
                                    setTextColor(getColor(R.color.textColor))
                                }
                                classesContainer.addView(classView)
                            }
                        }
                    }

                    if (!hasClasses) {
                        txtNoClasses.visibility = View.VISIBLE
                    } else {
                        txtNoClasses.visibility = View.GONE
                    }

                    llDaysContainer.addView(dayView)
                }

            } catch (e: Exception) {
                Log.e("StudentSchedule", "Erro ao carregar horários", e)
            }
        }
    }
}