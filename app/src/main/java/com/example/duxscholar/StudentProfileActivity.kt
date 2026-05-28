package com.example.duxscholar

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class StudentProfileActivity : AppCompatActivity() {
    lateinit var btnCarteirinha: Button
    lateinit var auth: FirebaseAuth
    lateinit var txtDataLabelValues: TextView
    lateinit var txtDataHeader: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_student_profile)

        auth = Firebase.auth

        btnCarteirinha = findViewById(R.id.btnStuCarteirinha)

        btnCarteirinha.setOnClickListener {
            val intent = Intent(this, CarteirinhaActivity::class.java)
            startActivity(intent)
        }

        txtDataHeader = findViewById(R.id.txtDataHeader)
        txtDataLabelValues = findViewById(R.id.txtDataLabelValues)

        var dataLabelString = "Em Curso\n"

        lifecycleScope.launch {
            val studentSnapshot = FirebaseDatabase.getInstance().getReference("alunos").child(auth.currentUser!!.uid).get().await()
            if (studentSnapshot.exists()) {
                val studentDataMap = studentSnapshot.getValue(Aluno::class.java) as Aluno
                val courseSnapshot = FirebaseDatabase.getInstance().getReference("cursos").child(studentDataMap.curso).get().await()

                if (courseSnapshot.exists()) {
                    val courseDataMap = courseSnapshot.getValue(Curso::class.java) as Curso

                    dataLabelString += "${studentDataMap.ra}\n${courseDataMap.name}\n${studentDataMap.semester}° (de ${courseDataMap.duration})\n${courseDataMap.shift}"

                    txtDataHeader.text = getString(R.string.studentprofileactivity_datatitle, studentDataMap.name)
                    txtDataLabelValues.text = dataLabelString
                }
            }
        }
    }
}