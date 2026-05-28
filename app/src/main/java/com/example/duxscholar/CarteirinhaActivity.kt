package com.example.duxscholar

import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import coil.load
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class CarteirinhaActivity : AppCompatActivity() {
    lateinit var imgCarteirinha: ImageView
    lateinit var txtCarteirinhaMissing: TextView
    val auth: FirebaseAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_carteirinha)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        imgCarteirinha = findViewById(R.id.imgCarteirinha)

        FirebaseDatabase.getInstance().getReference("alunos").child(auth.currentUser!!.uid).child("carteirinha").get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                if (snapshot.toString().isNotBlank()) {
                    val carteirinhab64 = Base64.decode(snapshot.value.toString(), Base64.DEFAULT)
                    imgCarteirinha.load(carteirinhab64)
                } else {
                    imgCarteirinha.visibility = View.GONE
                    txtCarteirinhaMissing.visibility = View.VISIBLE
                }
            }
        }

        findViewById<ImageButton>(R.id.imgbtnVoltarCarteirinha).setOnClickListener {
            finish()
        }
    }
}