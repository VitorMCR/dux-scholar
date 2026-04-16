package com.example.duxscholar

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class LoginActivity : AppCompatActivity() {

    lateinit var auth: FirebaseAuth
    lateinit var edtxtEmail: EditText
    lateinit var edtxtSenha: EditText

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = Firebase.auth

        edtxtEmail = findViewById(R.id.edtxtEmail)
        edtxtSenha = findViewById(R.id.edtxtSenha)
    }

    fun btnLoginClicked(view: View) {
        val email: String = edtxtEmail.text.toString()
        val senha: String = edtxtSenha.text.toString()

        if (email.isBlank() || senha.isBlank()) {
            Toast.makeText(baseContext,
                "Preencha todos os campos!",
                Toast.LENGTH_SHORT
            ).show()
        }

        auth.signInWithEmailAndPassword(edtxtEmail.text.toString(), edtxtSenha.text.toString())
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    Toast.makeText(baseContext,
                        "Logado com sucesso.",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                } else {
                    Toast.makeText(
                        baseContext,
                        "Falha na autenticação.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }
}