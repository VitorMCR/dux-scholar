package com.example.duxscholar

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.XmlResourceParser
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import me.angrybyte.numberpicker.view.ActualNumberPicker
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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
                        databaseReference!!.child(entries[position].id).removeValue().addOnSuccessListener {
                            Snackbar.make(window.decorView.rootView, "Entrada removida com sucesso.", Snackbar.LENGTH_LONG).show()
                        }.addOnFailureListener {
                            Snackbar.make(window.decorView.rootView, "Algo de errado ocorreu. Tente novamente mais tarde.", Snackbar.LENGTH_LONG).show()
                        }
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

        val allViews = getAllValidViews(dialogView)

        // Setup de campos para carregar imagem
        if (allViews.any { it is Button }) {
            val selectButtons: List<Button> = allViews.filterIsInstance<Button>()

            for (button in selectButtons) {
                button.setOnClickListener {
                    val pickMedia = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                        uri?.let {
                            val sizeInBytes = getFileSize(it, this)
                            val limitInBytes = 256 * 1024 // 256KB

                            if (sizeInBytes > limitInBytes) {
                                Snackbar.make(dialogView, "A imagem selecionada excede o tamanho máximo permitido (256 KB)",
                                    Snackbar.LENGTH_LONG).show()
                            } else {
                                button.text = getFileName(it, this)
                                button.hint = it.toString() // Funciona?
                            }
                        }
                    }

                    pickMedia.launch("image/*")
                }
            }
        }

        // Setup de dropdowns
        if (allViews.any { it is TextInputLayout }) {
            val dropDowns: List<TextInputLayout> = allViews.filterIsInstance<TextInputLayout>()

            for (dropDown in dropDowns) {
                val acTextView = dropDown.getChildAt(1) as AutoCompleteTextView
                var referenceDir = acTextView.hint.split(" ")[0].lowercase()

                when (referenceDir) {
                    "professor" -> {
                        referenceDir = "professores"
                    }
                    else -> {
                        referenceDir += "s"
                    }
                }

                val quickDatabaseReference = FirebaseDatabase.getInstance().getReference(referenceDir)
                val dropDownItems = mutableMapOf<String, String>()

                quickDatabaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        dropDownItems[snapshot.key.toString()] = snapshot.child("name").value.toString()
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.w("Firebase", "loadPost:onCancelled", error.toException())
                    }
                })

                val acAdapter = ArrayAdapter(this, R.layout.item_dropdown, dropDownItems.values.toList())
                acTextView.setAdapter(acAdapter)
            }
        }


        if (editMode) {
            val promptLayout = (dialogView as ViewGroup).children.firstOrNull() as LinearLayout
            val title = promptLayout.children.firstOrNull() as TextView
            title.text = "Editar " + title.text.split(" ").drop(1).joinToString(" ")

            TODO("Refazer lógica - '.dataClass' desnecessário?")
//            val dataMap = entries[editModePos].dataClass as Map<*, *>
//            val dataList = dataMap.values.toList()
//
//            val inputableViews = allViews.filterNot{ it is Button }
        }

        dialog.setOnShowListener {
            val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener {
                TODO("Refazer lógica")
//                var inputIsValid = true
//                for (edtxt in editTexts) {
//                    if (edtxt.text.isBlank()) {
//                        Snackbar.make(dialogView, "Um ou mais campos estão vazios.", Snackbar.LENGTH_LONG).show()
//                        inputIsValid = false
//                        break
//                    }
//                }
//
//                if (inputIsValid) {
//                    var dclass : Any? = 0
//                    when (WHAT_TO_EDIT) {
//                        "Noticias" -> dclass = Noticia(editTexts[0].text.toString(), editTexts[1].text.toString(), editTexts[2].text.toString(),
//                            LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
//                    }
//
//                    if (editMode) {
//                        databaseReference!!.child(entries[editModePos].id).setValue(dclass).addOnSuccessListener {
//                            Snackbar.make(window.decorView.rootView, "Editado com sucesso.", Snackbar.LENGTH_LONG).show()
//                        }.addOnFailureListener {
//                            Snackbar.make(window.decorView.rootView, "Algo de errado ocorreu. Tente novamente mais tarde.", Snackbar.LENGTH_LONG).show()
//                        }
//                        dialog.dismiss()
//                    } else {
//                        val entryRef = databaseReference!!.push()
//                        entryRef.setValue(dclass).addOnSuccessListener {
//                            Snackbar.make(window.decorView.rootView, "Adicionado com sucesso.", Snackbar.LENGTH_LONG).show()
//                        }.addOnFailureListener {
//                            Snackbar.make(window.decorView.rootView, "Algo de errado ocorreu. Tente novamente mais tarde.", Snackbar.LENGTH_LONG).show()
//                        }
//
//                        dialog.dismiss()
//                    }
//                }
            }
        }
        dialog.show()
    }

    fun getAllValidViews(view: View): List<View> {
        val result = mutableListOf<View>()
        val types = listOf<Class<*>>(EditText::class.java, Button::class.java, ActualNumberPicker::class.java,
            TextInputLayout::class.java)
        for (type in types) {
            if (type.isInstance(view)) {
                result.add(view)
            }
        }

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                result.addAll(getAllValidViews(view.getChildAt(i)))
            }
        }
        return result
    }

    fun getFileName(uri: Uri, context: Context): String? {
        var fileName: String? = null

        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        fileName = it.getString(nameIndex)
                    }
                }
            }
        }

        if (fileName == null) {
            fileName = uri.path
            val cut = fileName?.lastIndexOf('/')
            if (cut != -1) {
                fileName = fileName?.substring(cut!! + 1)
            }
        }
        return fileName
    }

    fun getFileSize(uri: Uri, context: Context): Long {
        return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            cursor.moveToFirst()
            cursor.getLong(sizeIndex) // em bytes
        } ?: 0L
    }
}