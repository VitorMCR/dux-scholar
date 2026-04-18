package com.example.duxscholar

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Editpt2Activity : AppCompatActivity() {
    lateinit var txtEditTitle : TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_editpt2)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<ImageView>(R.id.btnEdit2Close).setOnClickListener {
            finish()
        }

        txtEditTitle = findViewById(R.id.txtEdit2Title)
        val WHAT_TO_EDIT = intent.getStringExtra("WHAT_TO_EDIT")

        when (WHAT_TO_EDIT) {
            "Noticias" -> {
                txtEditTitle.text = "EDITANDO Notícias"
            }
            "InfAcademicas" -> {
                txtEditTitle.text = "EDITANDO Informações Acadêmicas"
            }
            else -> {
                txtEditTitle.text = "EDITANDO ${WHAT_TO_EDIT}"
            }
        }
    }
}