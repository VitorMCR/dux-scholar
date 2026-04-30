package com.example.duxscholar

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

class TrocarSenhaActivity : AppCompatActivity() {

    lateinit var edtxtSenhaAtual: EditText
    lateinit var edtxtSenhaNova: EditText
    lateinit var edtxtNovaSenha2: EditText
    lateinit var btnAlterarSenha: Button
    lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_trocar_senha)

        auth = FirebaseAuth.getInstance()

        edtxtSenhaAtual = findViewById(R.id.edtxtSenhaAtual)
        edtxtSenhaNova = findViewById(R.id.edtxtSenhaNova)
        edtxtNovaSenha2 = findViewById(R.id.edtxtNovaSenha2)
        btnAlterarSenha = findViewById(R.id.btnAlterarSenha)

        btnAlterarSenha.setOnClickListener {
            val senhaAtual = edtxtSenhaAtual.text.toString()
            val novaSenha = edtxtSenhaNova.text.toString()
            val confirmarSenha = edtxtNovaSenha2.text.toString()

            if (senhaAtual.isEmpty() || novaSenha.isEmpty() || confirmarSenha.isEmpty()) {
                Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (novaSenha != confirmarSenha) {
                edtxtNovaSenha2.error = "As senhas não são iguais"
                return@setOnClickListener
            }

            if (novaSenha.length < 6) {
                edtxtSenhaNova.error = "A senha deve ter no mínimo 6 caracteres"
                return@setOnClickListener
            }

            val user = auth.currentUser
            val email = user?.email

            if (user != null && email != null) {
                val credential = EmailAuthProvider.getCredential(email, senhaAtual)

                user.reauthenticate(credential)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            user.updatePassword(novaSenha)
                                .addOnCompleteListener { updateTask ->
                                    if (updateTask.isSuccessful) {
                                        Toast.makeText(
                                            this,
                                            "Senha alterada com sucesso",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        finish()
                                    } else {
                                        Toast.makeText(
                                            this,
                                            "Erro ao alterar senha",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                        } else {
                            Toast.makeText(
                                this,
                                "Senha atual incorreta",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
            }
        }
    }
}