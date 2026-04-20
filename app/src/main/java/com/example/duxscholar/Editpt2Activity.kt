package com.example.duxscholar

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
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
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class Editpt2Activity : AppCompatActivity() {
    lateinit var txtEditTitle : TextView
    lateinit var txtEditLoading : TextView
    lateinit var recvEditList : RecyclerView
    lateinit var entries : ArrayList<EditEntry>
    lateinit var editEntryAdapter : EditEntryAdapter
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
        txtEditLoading = findViewById(R.id.txtEditLoading)
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
        editEntryAdapter = EditEntryAdapter(entries)

        recvEditList = findViewById(R.id.recvEditList)
        recvEditList.adapter = editEntryAdapter

        databaseReference = FirebaseDatabase.getInstance().getReference(WHAT_TO_EDIT.lowercase())

        eventListener = databaseReference!!.addValueEventListener(object : ValueEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onDataChange(snapshot: DataSnapshot) {
                entries.clear()
                for (data in snapshot.children) {
                    val entry = EditEntry(data.child("name").value.toString())
                    entry.let { entries.add(it) }
                }
                editEntryAdapter.notifyDataSetChanged()
                txtEditLoading.visibility = View.GONE
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
                .setPositiveButton("OK") { dialog, _ ->
                    @Suppress("UNCHECKED_CAST") val editTexts = getViewsByType(dialogView as ViewGroup, EditText::class.java) as List<EditText>

                    val entryRef = databaseReference!!.push()
                    var dclass : Any? = 0
                    when (WHAT_TO_EDIT) {
                        "Noticias" -> dclass = Noticia(editTexts[0].text.toString(), editTexts[1].text.toString(), editTexts[2].text.toString())
                    }
                    entryRef.setValue(dclass)
                    Snackbar.make(window.decorView.rootView, "Adicionado com sucesso.", Snackbar.LENGTH_LONG).show()
                    dialog.dismiss()
                }
                .setNegativeButton("Cancelar") { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
            dialog.show()
        }
    }

    fun getViewsByType(view: View, type: Class<*>): List<View> {
        val result = mutableListOf<View>()
        if (type.isInstance(view)) {
            result.add(view)
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                result.addAll(getViewsByType(view.getChildAt(i), type))
            }
        }
        return result
    }
}