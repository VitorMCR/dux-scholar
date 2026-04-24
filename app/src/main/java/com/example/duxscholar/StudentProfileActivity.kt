package com.example.duxscholar

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class StudentProfileActivity : AppCompatActivity() {

    lateinit var edttxtNome: EditText
    lateinit var edttxtEmail: EditText
    lateinit var edttxtRA: EditText
    lateinit var btnSalvar: Button
    lateinit var btnCarteirinha: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_student_profile)

        edttxtNome = findViewById(R.id.edttxtNome)
        edttxtEmail = findViewById(R.id.edttxtEmail)
        edttxtRA = findViewById(R.id.edttxtRa)
        btnSalvar = findViewById(R.id.btnSalvar)
        btnCarteirinha = findViewById(R.id.btnCarteirinha)

        val prefs = getSharedPreferences("perfil_usuario", MODE_PRIVATE)


        edttxtNome.setText(prefs.getString("nome", ""))
        edttxtEmail.setText(prefs.getString("email", ""))
        edttxtRA.setText(prefs.getString("ra", ""))

        btnSalvar.setOnClickListener {
            prefs.edit()
                .putString("nome", edttxtNome.text.toString())
                .putString("email", edttxtEmail.text.toString())
                .putString("ra", edttxtRA.text.toString())
                .apply()
        }



        btnCarteirinha.setOnClickListener {
            val intent = Intent(this, CarteirinhaActivity::class.java)
            startActivity(intent)
        }
    }
}