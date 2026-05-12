package com.example.duxscholar

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.XmlResourceParser
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Base64
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import me.angrybyte.numberpicker.view.ActualNumberPicker
import java.io.InputStream
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class Editpt2Activity : AppCompatActivity() {
    lateinit var txtEditTitle: TextView
    lateinit var txtEditLoading: TextView
    lateinit var recvEditList: RecyclerView
    lateinit var entries: ArrayList<EditEntry>
    lateinit var editEntryAdapter: EditEntryAdapter
    var databaseReference: DatabaseReference? = null
    var eventListener: ValueEventListener? = null

    private var lastClickedButton: Button? = null

    private val pickMedia =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                val sizeInBytes = getFileSize(it, this)
                val limitInBytes = 256 * 1024 // 256KB

                if (sizeInBytes > limitInBytes) {
                    Toast.makeText(
                        this,
                        "A imagem selecionada excede o tamanho máximo permitido (256 KB)",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    lastClickedButton?.text = getFileName(it, this)
                    lastClickedButton?.hint = getBase64FromUri(it, this) // Funciona?
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_editpt2)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }

        findViewById<ImageView>(R.id.btnEdit2Close).setOnClickListener {
            finish()
        }

        txtEditTitle = findViewById(R.id.txtEdit2Title)
        txtEditLoading = findViewById(R.id.txtEditLoading)
        val editTarget = intent.getStringExtra("WHAT_TO_EDIT").toString()

        when (editTarget) {
            "Noticias" -> {
                txtEditTitle.text = getString(R.string.editpt2activity_title_editing, "Notícias")
            }

            "InfoAcademicas" -> {
                txtEditTitle.text =
                    getString(R.string.editpt2activity_title_editing, "Informações Acadêmicas")
            }

            else -> {
                txtEditTitle.text = getString(R.string.editpt2activity_title_editing, editTarget)
            }
        }

        databaseReference =
            FirebaseDatabase.getInstance().getReference(editTarget.lowercase())

        entries = ArrayList()
        editEntryAdapter =
            EditEntryAdapter(entries, object : EditEntryAdapter.EntryInteractionListener {
                override fun onEditClick(position: Int) {
                    val editPromptLayout = getTargetedLayout(editTarget)

                    if (editPromptLayout != null) {
                        lifecycleScope.launch {
                            loadAlertDialog(editPromptLayout, editTarget, true, position)
                        }
                    }
                }

                override fun onDeleteClick(position: Int) {
                    val userId = entries[position].id
                    val isUserTarget = editTarget == "Alunos" || editTarget == "Professores"

                    AlertDialog.Builder(this@Editpt2Activity)
                        .setTitle("Confirmar Exclusão")
                        .setMessage("Deseja mesmo excluir esta entrada?${if (isUserTarget) " Isso também removerá permanentemente o acesso à conta." else ""}")
                        .setPositiveButton("Sim") { dialog, _ ->
                            lifecycleScope.launch {
                                try {
                                    if (isUserTarget) {
                                        databaseReference!!.child(userId).child("active").setValue(false)
                                    } else {
                                        databaseReference!!.child(userId).removeValue().await()
                                    }

                                    Snackbar.make(
                                        window.decorView.rootView,
                                        "Excluído com sucesso!",
                                        Snackbar.LENGTH_LONG
                                    ).show()
                                } catch (e: Exception) {
                                    Log.e("Editpt2Activity", "Erro ao deletar: ${e.message}")
                                    Snackbar.make(
                                        window.decorView.rootView,
                                        "Falha ao excluir conta. Tente novamente mais tarde.",
                                        Snackbar.LENGTH_LONG
                                    ).show()
                                }
                            }
                            dialog.dismiss()
                        }
                        .setNegativeButton("Não") { dialog, _ -> dialog.dismiss() }
                        .show()
                }
            })

        recvEditList = findViewById(R.id.recvEditList)
        recvEditList.adapter = editEntryAdapter

        eventListener =
            databaseReference!!.addValueEventListener(object : ValueEventListener {
                @SuppressLint("NotifyDataSetChanged")
                override fun onDataChange(snapshot: DataSnapshot) {
                    entries.clear()
                    for (data in snapshot.children) {
                        if (data.child("active")
                                .exists() && data.child("active").value == false
                        ) continue
                        val entry = EditEntry(
                            data.child("name").value.toString(),
                            data.key.toString()
                        )
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
            val editPromptLayout = getTargetedLayout(editTarget)

            if (editPromptLayout != null) {
                lifecycleScope.launch {
                    loadAlertDialog(editPromptLayout, editTarget)
                }
            }
        }
    }

    val secondaryAuth: FirebaseAuth by lazy {
        val options = FirebaseApp.getInstance().options
        val secondaryApp =
            FirebaseApp.getApps(this).find { it.name == "admin_create" }
                ?: FirebaseApp.initializeApp(this, options, "admin_create")
        FirebaseAuth.getInstance(secondaryApp)
    }

    suspend fun loadAlertDialog(
        layoutToLoad: XmlResourceParser,
        editTarget: String,
        editMode: Boolean = false,
        editModePos: Int = 0,
    ) {
        val dialogView = layoutInflater.inflate(layoutToLoad, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("OK", null)
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        val titleString = when (editTarget) {
            "Noticias" -> "Notícia"
            "InfoAcademicas" -> "Informação Acadêmica"
            else -> editTarget.slice(IntRange(0, editTarget.length - 2))
        }

        val allViews = getAllValidViews(dialogView)
        Log.d("Editpt2Activity", allViews.toString())

        setupImageButtons(allViews)
        setupDropdowns(allViews)

        val promptLayout =
            (dialogView as ViewGroup).children.firstOrNull() as LinearLayout
        val promptTitle = promptLayout.children.firstOrNull() as TextView

        if (editMode) {
            promptTitle.text = getString(R.string.editpt2activity_prompt_edit, titleString)

            when (editTarget) {
                "Alunos" -> {
                    dialogView.findViewById<EditText>(R.id.edtxtStuTempPassword).visibility =
                        View.GONE
                }

                "Professores" -> {
                    dialogView.findViewById<EditText>(R.id.edtxtProfTempPassword).visibility =
                        View.GONE
                }
            }

            fillFieldsForEditMode(dialogView, editTarget, editModePos)
        } else {
            promptTitle.text = getString(R.string.editpt2activity_prompt_insert, titleString)
        }

        dialog.setOnShowListener {
            val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener {
                var inputIsValid = true

                allViews.filter { it is EditText || it is TextInputLayout }.forEach { view ->
                    if (view is EditText && view.tag == "required" && view.text.isBlank()) {
                        inputIsValid = false
                    }
                    if (view is TextInputLayout && view.tag == "required" && view.editText?.text.toString()
                            .isBlank()
                    ) {
                        inputIsValid = false
                    }

                    if (!inputIsValid) {
                        Snackbar.make(
                            dialogView, "Um ou mais campos obrigatórios estão vazios.",
                            Snackbar.LENGTH_LONG
                        ).show()
                        return@setOnClickListener
                    }
                }

                val dclass = when (editTarget) {
                    "Alunos" -> {
                        val acTextView =
                            dialogView.findViewById<AutoCompleteTextView>(R.id.actxtStuCourse)

                        Aluno(
                            dialogView.findViewById<EditText>(R.id.edtxtStuFullName).text.toString(),
                            dialogView.findViewById<Button>(R.id.btnStuProfilePic).hint?.toString()
                                ?: "none",
                            dialogView.findViewById<EditText>(R.id.edtxtStuEmail).text.toString(),
                            dialogView.findViewById<EditText>(R.id.edtxtStuRA).text.toString(),
                            dialogView.findViewById<EditText>(R.id.edtxtStuPhone).text.toString(),
                            if (!editMode) dialogView.findViewById<EditText>(R.id.edtxtStuTempPassword).text.toString() else "",
                            getSelectedDropdownId(acTextView),
                            dialogView.findViewById<ActualNumberPicker>(R.id.npickStuSemester_depends_drpdwnStuCourse).value,
                            dialogView.findViewById<Button>(R.id.btnStuCarteirinha).hint?.toString()
                                ?: "none"
                        )
                    }

                    "Professores" -> Professor(
                        dialogView.findViewById<EditText>(R.id.edtxtProfFullName).text.toString(),
                        dialogView.findViewById<Button>(R.id.btnProfProfilePic).hint?.toString()
                            ?: "none",
                        dialogView.findViewById<EditText>(R.id.edtxtProfEmail).text.toString(),
                        dialogView.findViewById<EditText>(R.id.edtxtProfMatricula).text.toString(),
                        dialogView.findViewById<EditText>(R.id.edtxtProfPhone).text.toString(),
                        if (!editMode) dialogView.findViewById<EditText>(R.id.edtxtProfTempPassword).text.toString() else ""
                    )

                    "Cursos" -> Curso(
                        dialogView.findViewById<EditText>(R.id.edtxtCourName).text.toString(),
                        dialogView.findViewById<ActualNumberPicker>(R.id.npickCourDurationSemester).value,
                        dialogView.findViewById<ActualNumberPicker>(R.id.npickCourCapAlunos).value,
                        dialogView.findViewById<RadioButton>(
                            dialogView.findViewById<RadioGroup>(
                                R.id.rdgrpCourShift
                            ).checkedRadioButtonId
                        )?.text.toString()
                    )

                    "Disciplinas" -> {
                        val acTextView =
                            dialogView.findViewById<AutoCompleteTextView>(R.id.actxtDiscProf)

                        Disciplina(
                            dialogView.findViewById<EditText>(R.id.edtxtDiscName).text.toString(),
                            dialogView.findViewById<RadioButton>(
                                dialogView.findViewById<RadioGroup>(
                                    R.id.rdgrpDiscShift
                                ).checkedRadioButtonId
                            )?.text.toString(),
                            getSelectedDropdownId(acTextView)
                        )
                    }

                    "Noticias" -> Noticia(
                        dialogView.findViewById<EditText>(R.id.edtxtNewTitle).text.toString(),
                        dialogView.findViewById<EditText>(R.id.edtxtNewHeader).text.toString(),
                        dialogView.findViewById<Button>(R.id.btnNewImage).hint?.toString()
                            ?: "none",
                        dialogView.findViewById<EditText>(R.id.edtxtNewContent).text.toString(),
                        ZonedDateTime.now(ZoneId.of("America/Sao_Paulo")).format(
                            DateTimeFormatter.ofPattern("dd/MM/yyyy")
                        )
                    )

                    "InfoAcademicas" -> InfoAcademica(
                        dialogView.findViewById<EditText>(R.id.edtxtServTitle).text.toString(),
                        dialogView.findViewById<Button>(R.id.btnServIcon).hint?.toString()
                            ?: "none",
                        dialogView.findViewById<EditText>(R.id.edtxtServContent).text.toString()
                    )

                    else -> throw IllegalArgumentException("Alvo desconhecido: $editTarget")
                }
                lifecycleScope.launch {
                    if (editMode) {
                        databaseReference!!.child(entries[editModePos].id).setValue(dclass).await()

                        Snackbar.make(
                            window.decorView.rootView,
                            "Editado com sucesso!",
                            Snackbar.LENGTH_LONG
                        ).show()
                    } else {
                        val finalId = when (dclass) {
                            is Aluno -> createAuthAccount(
                                dclass.email,
                                dclass.temppass,
                                dclass.name
                            )

                            is Professor -> createAuthAccount(
                                dclass.email,
                                dclass.temppass,
                                dclass.name
                            )

                            else -> null
                        }

                        if (finalId != null) {
                            databaseReference!!.child(finalId).setValue(dclass).await()
                        } else {
                            databaseReference!!.push().setValue(dclass).await()
                        }
                        Snackbar.make(
                            window.decorView.rootView,
                            "Adicionado com sucesso!",
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private suspend fun createAuthAccount(
        email: String,
        pass: String,
        dName: String
    ): String {
        val result = secondaryAuth.createUserWithEmailAndPassword(email, pass).await()

        secondaryAuth.currentUser!!.updateProfile(userProfileChangeRequest {
            displayName = dName
        }).await()

        secondaryAuth.signOut()
        return result.user?.uid ?: throw Exception("Falha ao obter UID do novo usuário")
    }

    private fun getSelectedDropdownId(acTextView: AutoCompleteTextView): String {
        val adapter = acTextView.adapter ?: return "none"
        val text = acTextView.text.toString()
        for (i in 0 until adapter.count) {
            val item = adapter.getItem(i) as? DropdownItem
            if (item?.name == text) return item.id
        }
        return "none"
    }

    private fun setupImageButtons(allViews: List<View>) {
        val selectButtons = allViews.filterIsInstance<Button>()
            .filter { it !is android.widget.CompoundButton }

        if (selectButtons.isEmpty()) return

        for (button in selectButtons) {
            button.setOnClickListener {
                lastClickedButton = button
                pickMedia.launch("image/*")
            }
        }
    }

    private suspend fun setupDropdowns(allViews: List<View>) {
        val textInputLayouts = allViews.filterIsInstance<TextInputLayout>()
        if (textInputLayouts.isEmpty()) return

        val db = FirebaseDatabase.getInstance()

        for (dropDown in textInputLayouts) {
            val acTextView = dropDown.editText as? AutoCompleteTextView ?: continue
            val refDir = acTextView.tag?.toString() ?: continue

            val snapshot = db.getReference(refDir).get().await()

            val dropDownItems = snapshot.children.mapNotNull { entry ->
                val id = entry.key ?: return@mapNotNull null
                val name = entry.child("name").value?.toString() ?: "Sem nome"

                val displayName = if (refDir == "cursos") {
                    val shift = entry.child("shift").value?.toString() ?: ""
                    "$name ($shift)"
                } else {
                    name
                }

                DropdownItem(id, displayName)
            }

            if (dropDownItems.isEmpty()) {
                showEmptyDropdownWarning(refDir)
                return
            }

            val acAdapter = ArrayAdapter(this, R.layout.item_dropdown, dropDownItems)
            acTextView.setAdapter(acAdapter)

            if (refDir == "cursos") {
                acTextView.setOnItemClickListener { parent, _, position, _ ->
                    val selectedItem = parent.getItemAtPosition(position) as DropdownItem
                    db.getReference(refDir).child(selectedItem.id).child("duration").get()
                        .addOnSuccessListener { durationSnapshot ->
                            val maxDuration = (durationSnapshot.value as? Long)?.toInt()
                                ?: return@addOnSuccessListener
                            updateDependentPickers(allViews, dropDown.id, maxDuration)
                        }
                }
            }
        }
    }

    /**
     * Em caso de number pickers que precisam ser coerentes com o valor de algum outro campo.
     * Exemplo: npickStuSemester_depends_drpdwnStuCourse (Duração do curso)
     * Por enquanto resolve apenas este caso
     */
    private fun updateDependentPickers(allViews: List<View>, triggerViewId: Int, maxValue: Int) {
        val triggerName = resources.getResourceEntryName(triggerViewId)
        allViews.filterIsInstance<ActualNumberPicker>().forEach { picker ->
            val pickerName = resources.getResourceEntryName(picker.id)

            if (pickerName.contains("_depends_$triggerName")) {
                picker.maxValue = maxValue
                if (picker.value > maxValue) {
                    picker.value = maxValue
                }
                picker.invalidate()
            }
        }
    }

    private fun showEmptyDropdownWarning(refDir: String) {
        val refDirFormatted = when (refDir) {
            "infoacademicas" -> "Informações Acadêmicas"
            else -> refDir.replaceFirstChar { it.uppercase() }
        }

        Snackbar.make(
            window.decorView.rootView,
            "Não há nenhuma entrada em $refDirFormatted para usar na inserção!",
            Snackbar.LENGTH_LONG
        ).show()
    }

    private suspend fun fillFieldsForEditMode(
        dialogView: View,
        editTarget: String,
        editModePos: Int
    ) {
        val snapshot = databaseReference!!.child(entries[editModePos].id).get().await()

        if (snapshot.exists()) {
            when (editTarget) {
                "Alunos" -> {
                    val dataMap = snapshot.getValue(Aluno::class.java) as Aluno

                    dialogView.findViewById<EditText>(R.id.edtxtStuFullName).setText(dataMap.name)

                    val btnStuProfilePic = dialogView.findViewById<Button>(R.id.btnStuProfilePic)
                    if (dataMap.pfp != "none") {
                        btnStuProfilePic.hint = dataMap.pfp
                        btnStuProfilePic.text = resources.getString(R.string.hint_btn_change)
                    }

                    dialogView.findViewById<EditText>(R.id.edtxtStuEmail).setText(dataMap.email)
                    dialogView.findViewById<EditText>(R.id.edtxtStuRA).setText(dataMap.ra)
                    dialogView.findViewById<EditText>(R.id.edtxtStuPhone).setText(dataMap.phone)
                    // Password é isento propositalmente

                    val acTextView =
                        dialogView.findViewById<AutoCompleteTextView>(R.id.actxtStuCourse)

                    val snapshotCourse = FirebaseDatabase.getInstance().getReference("cursos")
                        .child(dataMap.curso).get().await()
                    if (snapshotCourse.exists()) {
                        val name = snapshotCourse.child("name").value?.toString() ?: ""
                        val shift = snapshotCourse.child("shift").value?.toString() ?: ""
                        acTextView.setText(getString(R.string.hint_dropdown_course, name, shift), false)
                    }

                    val npickStuSemester =
                        dialogView.findViewById<ActualNumberPicker>(R.id.npickStuSemester_depends_drpdwnStuCourse)
                    npickStuSemester.value = dataMap.semester
                    npickStuSemester.invalidate()

                    val btnStuCarteirinha = dialogView.findViewById<Button>(R.id.btnStuCarteirinha)
                    if (dataMap.carteirinha != "none") {
                        btnStuCarteirinha.hint = dataMap.carteirinha
                        btnStuCarteirinha.text = resources.getString(R.string.hint_btn_change)
                    }
                }

                "Professores" -> {
                    val dataMap = snapshot.getValue(Professor::class.java) as Professor

                    dialogView.findViewById<EditText>(R.id.edtxtProfFullName).setText(dataMap.name)

                    val btnProfProfilePic = dialogView.findViewById<Button>(R.id.btnProfProfilePic)
                    if (dataMap.pfp != "none") {
                        btnProfProfilePic.hint = dataMap.pfp
                        btnProfProfilePic.text = resources.getString(R.string.hint_btn_change)
                    }

                    dialogView.findViewById<EditText>(R.id.edtxtProfEmail).setText(dataMap.email)
                    dialogView.findViewById<EditText>(R.id.edtxtProfMatricula)
                        .setText(dataMap.matricula)
                    dialogView.findViewById<EditText>(R.id.edtxtProfPhone).setText(dataMap.phone)
                    // Password é isento propositalmente
                }

                "Cursos" -> {
                    val dataMap = snapshot.getValue(Curso::class.java) as Curso

                    dialogView.findViewById<EditText>(R.id.edtxtCourName).setText(dataMap.name)

                    val npickCourDurationSemester =
                        dialogView.findViewById<ActualNumberPicker>(R.id.npickCourDurationSemester)
                    npickCourDurationSemester.value = dataMap.duration
                    npickCourDurationSemester.invalidate()

                    val npickCourCapAlunos =
                        dialogView.findViewById<ActualNumberPicker>(R.id.npickCourCapAlunos)
                    npickCourCapAlunos.value = dataMap.capacity
                    npickCourCapAlunos.invalidate()

                    dialogView.findViewById<RadioGroup>(R.id.rdgrpCourShift).check(
                        when (dataMap.shift) {
                            "Matutino" -> R.id.rdbtnMatutino
                            "Vespertino" -> R.id.rdbtnVespertino
                            "Noturno" -> R.id.rdbtnNoturno
                            else -> R.id.rdbtnMatutino
                        }
                    )
                }

                "Disciplinas" -> {
                    val dataMap = snapshot.getValue(Disciplina::class.java) as Disciplina

                    dialogView.findViewById<EditText>(R.id.edtxtDiscName).setText(dataMap.name)
                    dialogView.findViewById<RadioGroup>(R.id.rdgrpDiscShift).check(
                        when (dataMap.shift) {
                            "Matutino" -> R.id.rdbtnMatutino
                            "Vespertino" -> R.id.rdbtnVespertino
                            "Noturno" -> R.id.rdbtnNoturno
                            else -> R.id.rdbtnMatutino
                        }
                    )

                    val acTextView =
                        dialogView.findViewById<AutoCompleteTextView>(R.id.actxtDiscProf)

                    val snapshotProf = FirebaseDatabase.getInstance().getReference("professores")
                        .child(dataMap.professor).get().await()
                    if (snapshotProf.exists()) {
                        acTextView.setText(
                            snapshotProf.child("name").value?.toString() ?: "",
                            false
                        )
                    }
                }

                "Noticias" -> {
                    val dataMap = snapshot.getValue(Noticia::class.java) as Noticia

                    dialogView.findViewById<EditText>(R.id.edtxtNewTitle).setText(dataMap.name)
                    dialogView.findViewById<EditText>(R.id.edtxtNewHeader).setText(dataMap.header)

                    val btnNewImage = dialogView.findViewById<Button>(R.id.btnNewImage)
                    if (dataMap.image != "none") {
                        btnNewImage.hint = dataMap.image
                        btnNewImage.text = resources.getString(R.string.hint_btn_change)
                    }

                    dialogView.findViewById<EditText>(R.id.edtxtNewContent).setText(dataMap.content)
                }

                "InfoAcademicas" -> {
                    val dataMap = snapshot.getValue(InfoAcademica::class.java) as InfoAcademica

                    dialogView.findViewById<EditText>(R.id.edtxtServTitle).setText(dataMap.name)

                    val btnServIcon = dialogView.findViewById<Button>(R.id.btnServIcon)
                    if (dataMap.icon != "none") {
                        btnServIcon.hint = dataMap.icon
                        btnServIcon.text = resources.getString(R.string.hint_btn_change)
                    }

                    dialogView.findViewById<EditText>(R.id.edtxtServContent)
                        .setText(dataMap.content)
                }
            }
        }
    }

    private fun getTargetedLayout(editTarget: String): XmlResourceParser? {
        val layoutId = when (editTarget) {
            "Alunos" -> R.layout.dialog_prompt_alunos
            "Professores" -> R.layout.dialog_prompt_professores
            "Cursos" -> R.layout.dialog_prompt_cursos
            "Disciplinas" -> R.layout.dialog_prompt_disciplinas
            "Noticias" -> R.layout.dialog_prompt_noticias
            "InfoAcademicas" -> R.layout.dialog_prompt_infoacademicas
            else -> null
        } ?: return null

        return resources.getLayout(layoutId)
    }
}


/// HELPER FUNCTIONS

private fun getAllValidViews(view: View): List<View> {
    val result = mutableListOf<View>()

    val isValidType = view is EditText ||
            (view is Button && view !is android.widget.CompoundButton) ||
            view is ActualNumberPicker ||
            view is TextInputLayout ||
            view is RadioGroup

    if (isValidType) {
        result.add(view)
    }

    if (view is ViewGroup) {
        for (i in 0 until view.childCount) {
            result.addAll(getAllValidViews(view.getChildAt(i)))
        }
    }
    return result
}


// TRATAMENTO DE ARQUIVO

private fun getFileName(uri: Uri, context: Context): String? {
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

private fun getFileSize(uri: Uri, context: Context): Long {
    return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
        cursor.moveToFirst()
        cursor.getLong(sizeIndex) // em bytes
    } ?: 0L
}

private fun getBase64FromUri(uri: Uri, context: Context): String? {
    return try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        val bytes = inputStream?.readBytes()

        bytes?.let {
            Base64.encodeToString(it, Base64.NO_WRAP)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}