package com.example.duxscholar

import android.annotation.SuppressLint
import android.content.res.XmlResourceParser
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
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

        databaseReference = FirebaseDatabase.getInstance().getReference(WHAT_TO_EDIT.lowercase())

        entries = ArrayList()
        editEntryAdapter = EditEntryAdapter(entries, object : EditEntryAdapter.EntryInteractionListener {
            override fun onEditClick(position: Int) {
                val editPrompt = "dialog_prompt_" + WHAT_TO_EDIT.lowercase()
                val editPromptLayout = resources.getLayout(resources.getIdentifier(editPrompt, "layout", packageName))

                loadAlertDialog(editPromptLayout, WHAT_TO_EDIT, true, position)
            }

            override fun onDeleteClick(position: Int) {
                val deleteDialog = AlertDialog.Builder(this@Editpt2Activity)
                    .setTitle("Confirmar Ação")
                    .setMessage("Deseja mesmo deletar esta entrada?")
                    .setPositiveButton("Sim") { dialog, _ ->
                        databaseReference!!.child(entries[position].id).removeValue()
                        dialog.dismiss()
                    }
                    .setNegativeButton("Não") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .create()
                deleteDialog.show()
            }
        })

        recvEditList = findViewById(R.id.recvEditList)
        recvEditList.adapter = editEntryAdapter

        eventListener = databaseReference!!.addValueEventListener(object : ValueEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onDataChange(snapshot: DataSnapshot) {
                entries.clear()
                for (data in snapshot.children) {
                    val entry = EditEntry(data.child("name").value.toString(), data.key.toString(), data.value)
                    entry.let { entries.add(it) }
                }
                editEntryAdapter.notifyDataSetChanged()
                txtEditLoading.visibility = View.GONE
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "ERRO: ${error.message}")
            }
        })

        findViewById<FloatingActionButton>(R.id.fabAdd).setOnClickListener {
            val editPrompt = "dialog_prompt_" + WHAT_TO_EDIT.lowercase()
            val editPromptLayout = resources.getLayout(resources.getIdentifier(editPrompt, "layout", packageName))

            loadAlertDialog(editPromptLayout, WHAT_TO_EDIT)
        }
    }

    fun loadAlertDialog(layoutToLoad: XmlResourceParser, WHAT_TO_EDIT: String, editMode: Boolean = false, editModePos: Int = 0) {
        val dialogView = layoutInflater.inflate(layoutToLoad, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("OK", null)
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        @Suppress("UNCHECKED_CAST") val editTexts = getViewsByType(dialogView, EditText::class.java) as List<EditText>
        if (editMode) {
            val promptLayout = (dialogView as ViewGroup).children.firstOrNull() as LinearLayout
            val title = promptLayout.children.firstOrNull() as TextView
            title.text = "Editar " + title.text.split(" ")[1]
            val dataMap = entries[editModePos].dataClass as Map<*, *>
            val dataList = dataMap.values.toList()

            for (i in 0 until editTexts.size) {
                editTexts[i].setText(dataList[i].toString())
            }
        }

        dialog.setOnShowListener {
            val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener {
                var inputIsValid = true
                for (edtxt in editTexts) {
                    if (edtxt.text.isBlank()) {
                        Snackbar.make(dialogView, "Um ou mais campos estão vazios.", Snackbar.LENGTH_LONG).show()
                        inputIsValid = false
                        break
                    }
                }

                if (inputIsValid) {
                    var dclass : Any? = 0
                    when (WHAT_TO_EDIT) {
                        "Noticias" -> dclass = Noticia(editTexts[0].text.toString(), editTexts[1].text.toString(), editTexts[2].text.toString())
                    }

                    if (editMode) {
                        databaseReference!!.child(entries[editModePos].id).setValue(dclass)
                        Snackbar.make(window.decorView.rootView, "Editado com sucesso.", Snackbar.LENGTH_LONG).show()
                        dialog.dismiss()
                    } else {
                        val entryRef = databaseReference!!.push()
                        entryRef.setValue(dclass)
                        Snackbar.make(window.decorView.rootView, "Adicionado com sucesso.", Snackbar.LENGTH_LONG).show()
                        dialog.dismiss()
                    }
                }
            }
        }
        dialog.show()
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