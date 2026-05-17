package com.example.duxscholar

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


class LoginActivity : AppCompatActivity() {

    lateinit var auth: FirebaseAuth
    lateinit var edtxtEmail: EditText
    lateinit var edtxtSenha: EditText
    lateinit var btnEntrar: Button
    lateinit var btnDeslogar: Button
    lateinit var btnDefinirNome: Button
    lateinit var edtxtNome: EditText
    var databaseReference: DatabaseReference? = null

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
                Toast.makeText(
                    baseContext,
                    "Preencha todos os campos!",
                    Toast.LENGTH_SHORT
                ).show()
            }

            auth.signInWithEmailAndPassword(edtxtEmail.text.toString(), edtxtSenha.text.toString())
                .addOnCompleteListener(this) { task ->
                    // Confirma que o usuário existe
                    if (task.isSuccessful) {
                        lifecycleScope.launch {
                            treatBasicUser()
                            finish()
                        }
                    } else {
                        Toast.makeText(
                            baseContext,
                            "Falha na autenticação. Tente novamente mais tarde.",
                            Toast.LENGTH_LONG
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
                        Toast.makeText(
                            baseContext,
                            "Alterado com sucesso.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }
    }

    /**
     * Usa-se a seguinte definição de básico neste contexto:
     * Básico = aluno/professor;
     * Não Básico = administrador.
     */
    private suspend fun treatBasicUser() {
        val user = auth.currentUser

        for (usertype in listOf("alunos", "professores")) {
            databaseReference = FirebaseDatabase.getInstance().getReference(usertype)

            val snapshot = databaseReference!!.child(user!!.uid).get().await()

            if (snapshot.exists()) {
                val isActive = snapshot.child("active").getValue(Boolean::class.java) ?: true

                if (isActive) {

                    val dName = snapshot.child("name").getValue(String::class.java)
                    // Atualiza o nome do usuário, caso necessário
                    if (user.displayName != dName) {
                        user.updateProfile(userProfileChangeRequest {
                            displayName = dName
                        })
                    }

                    Toast.makeText(
                        baseContext,
                        "Logado com sucesso. Bem-vindo(a)!",
                        Toast.LENGTH_SHORT
                    ).show()
                    break
                } else {
                    auth.signOut()
                    Toast.makeText(baseContext, "Esta conta está desativada.", Toast.LENGTH_LONG)
                        .show()
                    break
                }
            } else {
                Toast.makeText(baseContext, "Logado como administrador.", Toast.LENGTH_LONG).show()
                break
            }
        }
    }
}
