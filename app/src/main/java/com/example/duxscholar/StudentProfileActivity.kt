package com.example.duxscholar

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class StudentProfileActivity : AppCompatActivity() {

    lateinit var edttxtNome: EditText
    lateinit var edttxtEmail: EditText
    lateinit var edttxtRA: EditText
    lateinit var btnCarteirinha: Button
    lateinit var btnSairuser: Button
    lateinit var txtLink: TextView
    lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_student_profile)

        auth = Firebase.auth

        edttxtNome = findViewById(R.id.edttxtNome)
        edttxtEmail = findViewById(R.id.edttxtEmail)
        edttxtRA = findViewById(R.id.edttxtRa)
        btnCarteirinha = findViewById(R.id.btnStuCarteirinha)
        btnSairuser = findViewById(R.id.btnSairuser)
        txtLink = findViewById(R.id.txtLink)

        txtLink.paint.isUnderlineText = true

        txtLink.setOnClickListener {
            val intent = Intent(this, TrocarSenhaActivity::class.java)
            startActivity(intent)
        }

        val prefs = getSharedPreferences("perfil_usuario", MODE_PRIVATE)

        edttxtNome.setText(prefs.getString("nome", ""))
        edttxtEmail.setText(prefs.getString("email", ""))
        edttxtRA.setText(prefs.getString("ra", ""))

        btnSairuser.setOnClickListener {
            val builder = AlertDialog.Builder(this)

            builder.setTitle("Sair da conta")
            builder.setMessage("Tem certeza que deseja sair?")

            builder.setPositiveButton("Sim") { _, _ ->
                auth.signOut()

                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                finish()
            }

            builder.setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }

            builder.show()
        }

        btnCarteirinha.setOnClickListener {
            val intent = Intent(this, CarteirinhaActivity::class.java)
            startActivity(intent)
        }
    }
}