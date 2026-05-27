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
    lateinit var btnCarteirinha: Button
    lateinit var auth: FirebaseAuth

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
    }
}