package com.example.duxscholar

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.FirebaseDatabase

class SettingsActivity : AppCompatActivity() {
    lateinit var auth: FirebaseAuth
    lateinit var edtxtConfigNewPass: EditText
    lateinit var edtxtConfigRepNewPass: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = Firebase.auth
        val user = auth.currentUser!!

        edtxtConfigNewPass = findViewById(R.id.edtxtConfigNewPass)
        edtxtConfigRepNewPass = findViewById(R.id.edtxtConfigRepNewPass)

        findViewById<Button>(R.id.btnSubmitChanges).setOnClickListener {
            if (edtxtConfigNewPass.text.isNotBlank() && edtxtConfigRepNewPass.text.isNotBlank() && edtxtConfigNewPass.text.toString() == edtxtConfigRepNewPass.text.toString()) {
                if (edtxtConfigNewPass.text.toString().length >= 6) {
                    user.updatePassword(edtxtConfigNewPass.text.toString()).addOnSuccessListener {
                        Snackbar.make(window.decorView.rootView, "Senha alterada com sucesso!", Snackbar.LENGTH_SHORT).show()
                        FirebaseDatabase.getInstance().getReference("alunos").child(user.uid).child("temppass").setValue("")
                        edtxtConfigNewPass.text.clear()
                        edtxtConfigRepNewPass.text.clear()
                    }
                } else {
                    Snackbar.make(window.decorView.rootView, "A nova senha deve ter no mínimo 6 caracteres!", Snackbar.LENGTH_LONG).show()
                }
            } else {
                Snackbar.make(window.decorView.rootView, "As senhas não coincidem!", Snackbar.LENGTH_LONG).show()
            }
        }

        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Confirmar Logout")
                .setMessage("Deseja mesmo deslogar?")
                .setPositiveButton("Sim") { dialog, _ ->
                    dialog.dismiss()
                    auth.signOut()

                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                .setNegativeButton("Não") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }

        findViewById<ImageView>(R.id.btnConfigClose).setOnClickListener {
            finish()
        }
    }
}