package com.example.duxscholar

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class CarteirinhaActivity : AppCompatActivity() {

    lateinit var txtNomeCarteirinha: TextView
    lateinit var txtEmailCarteirinha: TextView

    lateinit var txtRACarteirinha: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_carteirinha)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        txtNomeCarteirinha = findViewById(R.id.txtNomeCarteirinha)
        txtEmailCarteirinha = findViewById(R.id.txtEmailCarteirinha)
        txtRACarteirinha = findViewById(R.id.txtRACarteirinha)


        val prefs = getSharedPreferences("perfil_usuario", MODE_PRIVATE)

        txtNomeCarteirinha.text = prefs.getString("nome", "")
        txtEmailCarteirinha.text = prefs.getString("email", "")
        txtRACarteirinha.text = prefs.getString("ra", "")
    }
}