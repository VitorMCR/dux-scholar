package com.example.duxscholar

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.auth.userProfileChangeRequest


class LoginActivity : AppCompatActivity() {

    lateinit var auth: FirebaseAuth
    lateinit var edtxtEmail: EditText
    lateinit var edtxtSenha: EditText
    lateinit var btnEntrar: Button
    lateinit var btnDeslogar: Button
    lateinit var btnDefinirNome: Button
    lateinit var edtxtNome: EditText

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

        btnEntrar = findViewById(R.id.btnEntrar)
        btnDeslogar = findViewById(R.id.btnDeslogar)

        btnDefinirNome = findViewById(R.id.btnDefinirNome)
        edtxtNome = findViewById(R.id.edttxtNome)

        btnEntrar.setOnClickListener {
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

        btnDeslogar.setOnClickListener {
            auth.signOut()
        }

        btnDefinirNome.setOnClickListener {
            val profileUpdate = userProfileChangeRequest {
                displayName = edtxtNome.text.toString()
            }

            auth.currentUser!!.updateProfile(profileUpdate)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(baseContext,
                            "Alterado com sucesso.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }
    }
}