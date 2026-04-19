package com.example.duxscholar

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class Editpt2Activity : AppCompatActivity() {
    lateinit var txtEditTitle : TextView
    lateinit var recvEditList : RecyclerView
    lateinit var entries : ArrayList<EditEntry>
    lateinit var editadapter : EditEntryAdapter
    var databaseReference : DatabaseReference? = null
    var eventListener : ValueEventListener? = null

    @SuppressLint("DiscouragedApi")
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
        val WHAT_TO_EDIT = intent.getStringExtra("WHAT_TO_EDIT").toString()

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

        entries = ArrayList()
        editadapter = EditEntryAdapter(entries)

        recvEditList = findViewById(R.id.recvEditList)
        recvEditList.adapter = editadapter

        databaseReference = FirebaseDatabase.getInstance().getReference(WHAT_TO_EDIT.lowercase())

        eventListener = databaseReference!!.addValueEventListener(object : ValueEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onDataChange(snapshot: DataSnapshot) {
                entries.clear()
                for (data in snapshot.children) {
                    val entry = EditEntry(data.value.toString())
                    entry.let { entries.add(it) }
                }
                editadapter.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(baseContext, "ERRO: ${error}", Toast.LENGTH_LONG).show()
            }
        })

        findViewById<FloatingActionButton>(R.id.fabAdd).setOnClickListener {
            val editPrompt = "dialog_prompt_" + WHAT_TO_EDIT.lowercase()
            val editPromptLayout = resources.getLayout(resources.getIdentifier(editPrompt, "layout", packageName))

            val dialogView = layoutInflater.inflate(editPromptLayout, null)
            val dialog = AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("OK") { dialog, which ->
                    dialog.dismiss()
                }
                .setNegativeButton("Cancelar") { dialog, which ->
                    dialog.dismiss()
                }
                .create()
            dialog.show()
        }
    }
}