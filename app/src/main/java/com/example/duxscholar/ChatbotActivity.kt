package com.example.duxscholar

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class ChatbotActivity : AppCompatActivity() {

    lateinit var editTextInput: EditText
    lateinit var recyclerView: RecyclerView
    lateinit var adapter: MessageAdapter
    lateinit var lnlytChips: LinearLayout
    lateinit var scrollChips: HorizontalScrollView
    lateinit var buttonSend: View
    lateinit var layoutChat: LinearLayout
    lateinit var layoutSplash: LinearLayout
    lateinit var layoutModalOverlay: FrameLayout

    val messages = mutableListOf<Message>()

    var isRequestRunning = false
    var loadingIndex = -1

    private val handler = Handler(Looper.getMainLooper())
    private var typingRunnable: Runnable? = null

    // Contextos simples
    private var noticiasContext: String = ""
    private var infoacademicasContext: String = ""
    private var professoresContext: String = ""
    private var cursosContext: String = ""
    private var disciplinasContext: String = ""
    private var horarioContext: String = ""

    // Contexto do aluno logado
    private var alunoContext: String = ""
    private var alunoCarregado = false

    // Dados do aluno logado
    private var alunoNome: String = ""
    private var alunoRA: String = ""
    private var alunoSemestre: String = ""
    private var alunoCursoId: String = ""
    private var alunoCursoNome: String = ""

    // Mapas para resolver relacionamentos entre tabelas
    private val cursosMap = mutableMapOf<String, String>()
    private val professoresMap = mutableMapOf<String, String>()
    private val disciplinasMap = mutableMapOf<String, Map<String, String>>()

    // Controle de carregamento
    private var professoresCarregados = false
    private var disciplinasCarregadas = false
    private var cursosCarregados = false

    private var disciplinasSnap: DataSnapshot? = null
    private var horariosPendentes: DataSnapshot? = null

    private var conversationHistory = JSONArray()

    private val INPUT_MAX_CHARS = 500

    private val chipSuggestions = listOf(
        "Quais são os cursos?",
        "Ver notícias",
        "Informações de estágio",
        "Calendário acadêmico",
        "Contato da faculdade",
        "Transporte escolar"
    )

    companion object {
        var splashAlreadyShown = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chatbot)

        editTextInput      = findViewById(R.id.editTextInput)
        recyclerView       = findViewById(R.id.recvChat)
        lnlytChips         = findViewById(R.id.lnlytChips)
        scrollChips        = findViewById(R.id.scrollChips)
        buttonSend         = findViewById(R.id.buttonSend)
        layoutChat         = findViewById(R.id.layoutChat)
        layoutSplash       = findViewById(R.id.layoutSplash)
        layoutModalOverlay = findViewById(R.id.layoutModalOverlay)

        adapter = MessageAdapter(messages)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        setupChips()
        setupButtons()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        editTextInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s != null && s.length > INPUT_MAX_CHARS) {
                    editTextInput.removeTextChangedListener(this)
                    editTextInput.setText(s.substring(0, INPUT_MAX_CHARS))
                    editTextInput.setSelection(INPUT_MAX_CHARS)
                    editTextInput.addTextChangedListener(this)
                }
            }
        })

        // Carrega dados do Firebase em paralelo
        loadAlunoLogado()
        loadFirebaseData()

        if (splashAlreadyShown) {
            layoutSplash.visibility = View.GONE
            layoutChat.visibility   = View.VISIBLE
            addWelcomeMessage()
        } else {
            showSplashThenChat()
        }
    }

    // ─────────────────────────────────────────────
    // CARREGAMENTO DO ALUNO LOGADO
    // ─────────────────────────────────────────────

    private fun loadAlunoLogado() {
        val uid = Firebase.auth.currentUser?.uid

        // Se não há usuário logado, marca como carregado sem dados (acesso anônimo)
        if (uid == null) {
            alunoCarregado = true
            Log.d("Firebase", "Nenhum usuário logado — chatbot sem contexto de aluno")
            return
        }

        FirebaseDatabase.getInstance().getReference("alunos").child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        alunoCarregado = true
                        Log.d("Firebase", "Aluno não encontrado no banco")
                        return
                    }

                    val aluno = snapshot.getValue(Aluno::class.java) ?: run {
                        alunoCarregado = true
                        return
                    }

                    alunoNome     = aluno.name
                    alunoRA       = aluno.ra
                    alunoSemestre = aluno.semester.toString()
                    alunoCursoId  = aluno.curso

                    // JOIN: busca o nome do curso pelo ID
                    FirebaseDatabase.getInstance().getReference("cursos").child(alunoCursoId)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(cursoSnapshot: DataSnapshot) {
                                if (cursoSnapshot.exists()) {
                                    val curso = cursoSnapshot.getValue(Curso::class.java)
                                    alunoCursoNome = curso?.name ?: ""
                                }

                                // Monta o contexto do aluno com os dados já resolvidos
                                montarContextoAluno()
                                alunoCarregado = true
                                Log.d("Firebase", "Aluno logado carregado: $alunoNome — $alunoCursoNome — ${alunoSemestre}º sem")
                            }

                            override fun onCancelled(error: DatabaseError) {
                                alunoCarregado = true
                                Log.e("Firebase", "Erro ao buscar curso do aluno: ${error.message}")
                            }
                        })
                }

                override fun onCancelled(error: DatabaseError) {
                    alunoCarregado = true
                    Log.e("Firebase", "Erro ao buscar aluno logado: ${error.message}")
                }
            })
    }

    private fun montarContextoAluno() {
        if (alunoNome.isBlank()) {
            alunoContext = ""
            return
        }

        val sb = StringBuilder()
        sb.append("Dados do aluno que está conversando agora:\n")
        sb.append("- Nome: $alunoNome\n")
        if (alunoRA.isNotBlank())       sb.append("- Registro Acadêmico (RA): $alunoRA\n")
        if (alunoCursoNome.isNotBlank()) sb.append("- Curso: $alunoCursoNome\n")
        if (alunoSemestre.isNotBlank()) sb.append("- Semestre atual: ${alunoSemestre}º semestre\n")

        sb.append("\n")
        sb.append("Instruções especiais para o aluno logado:\n")
        sb.append("- Quando o aluno perguntar sobre 'minha grade', 'minhas aulas', 'que aulas tenho', use o curso e semestre acima para filtrar os dados da grade de horários.\n")
        sb.append("- Quando o aluno perguntar 'que aula tenho na segunda' ou qualquer dia da semana, filtre a grade pelo curso e semestre do aluno e mostre as disciplinas do dia solicitado.\n")
        sb.append("- Você pode chamar o aluno pelo primeiro nome ao responder, para tornar a conversa mais pessoal.\n")

        alunoContext = sb.toString()
    }

    // ─────────────────────────────────────────────
    // CARREGAMENTO DO FIREBASE E CRUZAMENTO DE DADOS
    // ─────────────────────────────────────────────

    private fun loadFirebaseData() {

        FirebaseDatabase.getInstance().getReference("noticias")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val sb = StringBuilder("Notícias atuais da instituição:\n")
                    for (data in snapshot.children) {
                        sb.append("- Título: ${data.child("name").value}\n")
                        sb.append("  Resumo: ${data.child("header").value}\n")
                    }
                    noticiasContext = sb.toString()
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("Firebase", "Erro noticias: ${error.message}")
                }
            })

        FirebaseDatabase.getInstance().getReference("infoacademicas")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val sb = StringBuilder("Informações acadêmicas disponíveis no app:\n")
                    for (data in snapshot.children) {
                        sb.append("- ${data.child("name").value}\n")
                    }
                    infoacademicasContext = sb.toString()
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("Firebase", "Erro infoacademicas: ${error.message}")
                }
            })

        FirebaseDatabase.getInstance().getReference("professores")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val sb = StringBuilder("Professores da instituição:\n")
                    for (data in snapshot.children) {
                        val id   = data.key ?: continue
                        val nome = data.child("name").value.toString()
                        val email = data.child("email").value.toString()
                        professoresMap[id] = nome
                        sb.append("- Nome: $nome\n")
                        sb.append("  Email: $email\n")
                    }
                    professoresContext  = sb.toString()
                    professoresCarregados = true
                    tentarMontarDisciplinas()
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("Firebase", "Erro professores: ${error.message}")
                }
            })

        FirebaseDatabase.getInstance().getReference("cursos")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val sb = StringBuilder("Cursos da instituição:\n")
                    for (data in snapshot.children) {
                        val id      = data.key ?: continue
                        val nome    = data.child("name").value.toString()
                        val periodo = data.child("shift").value.toString()
                        val duracao = data.child("duration").value.toString()
                        val vagas   = data.child("capacity").value.toString()
                        cursosMap[id] = nome
                        sb.append("- Curso: $nome\n")
                        sb.append("  Período: $periodo\n")
                        sb.append("  Duração: $duracao semestres\n")
                        sb.append("  Vagas: $vagas\n")
                    }
                    cursosContext    = sb.toString()
                    cursosCarregados = true
                    tentarMontarHorario()
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("Firebase", "Erro cursos: ${error.message}")
                }
            })

        FirebaseDatabase.getInstance().getReference("disciplinas")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    disciplinasSnap = snapshot
                    tentarMontarDisciplinas()
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("Firebase", "Erro disciplinas: ${error.message}")
                }
            })

        FirebaseDatabase.getInstance().getReference("horarios_aula")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    horariosPendentes = snapshot
                    tentarMontarHorario()
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("Firebase", "Erro horarios: ${error.message}")
                }
            })
    }

    private fun tentarMontarDisciplinas() {
        val snapshot = disciplinasSnap ?: return
        if (!professoresCarregados) return

        val sb = StringBuilder("Disciplinas da instituição:\n")
        for (data in snapshot.children) {
            val id       = data.key ?: continue
            val nome     = data.child("name").value.toString()
            val periodo  = data.child("shift").value.toString()
            val profId   = data.child("professor").value.toString()
            val semestre = data.child("semester").value?.toString() ?: ""
            val cursoId  = data.child("course").value?.toString() ?: ""
            val profNome = professoresMap[profId] ?: "Professor não alocado"

            disciplinasMap[id] = mapOf(
                "nome"      to nome,
                "professor" to profNome,
                "periodo"   to periodo,
                "semestre"  to semestre,
                "cursoId"   to cursoId
            )

            sb.append("- Disciplina: $nome\n")
            sb.append("  Professor: $profNome\n")
            sb.append("  Período: $periodo\n")
            if (semestre.isNotEmpty()) sb.append("  Semestre: $semestre\n")
        }
        disciplinasContext    = sb.toString()
        disciplinasCarregadas = true
        tentarMontarHorario()
    }

    private fun tentarMontarHorario() {
        val snapshot = horariosPendentes ?: return
        if (!cursosCarregados || !disciplinasCarregadas) return

        val sb = StringBuilder("Grade de Horários por curso e semestre:\n")

        for (cursoSnap in snapshot.children) {
            val cursoId   = cursoSnap.key ?: continue
            val cursoNome = cursosMap[cursoId] ?: "Curso $cursoId"
            sb.append("\nCurso: $cursoNome\n")

            for (semestreSnap in cursoSnap.children) {
                val semestre = semestreSnap.key ?: continue
                sb.append("  Semestre $semestre:\n")

                for (diaSnap in semestreSnap.children) {
                    val dia = diaSnap.key ?: continue
                    val disciplinasNoDia = mutableListOf<String>()

                    for (aulaSnap in diaSnap.children) {
                        val discId = aulaSnap.value?.toString() ?: continue
                        if (discId == "none") continue
                        val disc = disciplinasMap[discId]
                        if (disc != null) {
                            val nomeDisc = disc["nome"] ?: discId
                            val prof     = disc["professor"] ?: ""
                            disciplinasNoDia.add(
                                if (prof.isNotEmpty()) "$nomeDisc (Prof: $prof)"
                                else nomeDisc
                            )
                        }
                    }

                    if (disciplinasNoDia.isNotEmpty()) {
                        sb.append("    $dia: ${disciplinasNoDia.joinToString(", ")}\n")
                    }
                }
            }
        }

        horarioContext = sb.toString()
        Log.d("Firebase", "Horários montados com sucesso")
    }

    // ─────────────────────────────────────────────
    // CONTEXTO PARA O GEMINI
    // ─────────────────────────────────────────────

    private fun buildContext(): String {
        val base = loadContextFromAssets()
        val sb   = StringBuilder()
        sb.append(base).append("\n\n")

        // Injeta o contexto do aluno logo após as instruções base,
        // para que o Gemini saiba quem está conversando desde o início
        if (alunoContext.isNotEmpty()) {
            sb.append(alunoContext).append("\n")
        }

        sb.append("""
            Instruções de relacionamento entre dados:
            - Cada disciplina pertence a um curso e a um semestre específico.
            - A grade de horários mostra quais disciplinas ocorrem em cada dia da semana, por curso e semestre.
            - Se o aluno informar seu curso e semestre, use a grade de horários para mostrar as disciplinas e os dias.
            - Se perguntarem sobre um professor, use as disciplinas para mostrar em quais cursos e semestres ele leciona.
            - Se perguntarem sobre disciplinas de um curso, filtre pelo nome do curso nos dados abaixo.
            
        """.trimIndent())
        sb.append("\n")

        if (noticiasContext.isNotEmpty())       sb.append(noticiasContext).append("\n")
        if (infoacademicasContext.isNotEmpty()) sb.append(infoacademicasContext).append("\n")
        if (professoresContext.isNotEmpty())    sb.append(professoresContext).append("\n")
        if (cursosContext.isNotEmpty())         sb.append(cursosContext).append("\n")
        if (disciplinasContext.isNotEmpty())    sb.append(disciplinasContext).append("\n")
        if (horarioContext.isNotEmpty())        sb.append(horarioContext).append("\n")

        return sb.toString()
    }

    // ─────────────────────────────────────────────
    // UI
    // ─────────────────────────────────────────────

    private fun addWelcomeMessage() {
        val greeting = if (alunoNome.isNotBlank()) {
            val firstName = alunoNome.split(" ").firstOrNull() ?: alunoNome
            "Olá, $firstName! Meu nome é Duque, seu assistente pessoal!\n\nComo posso te ajudar?"
        } else {
            "Olá! Meu nome é Duque, seu assistente pessoal!\n\nComo posso te ajudar?"
        }
        addMessage(greeting, false)
    }

    private fun setupButtons() {
        findViewById<ImageView>(R.id.btnClose).setOnClickListener { finish() }

        findViewById<TextView>(R.id.btnNewChat).setOnClickListener { showNewChatModal() }

        layoutModalOverlay.setOnClickListener { hideNewChatModal() }

        findViewById<TextView>(R.id.btnModalCancel).setOnClickListener { hideNewChatModal() }

        findViewById<TextView>(R.id.btnModalConfirm).setOnClickListener {
            hideNewChatModal()
            clearConversation()
        }

        findViewById<LinearLayout>(R.id.layoutModal).setOnClickListener { }
    }

    private fun showNewChatModal() {
        layoutModalOverlay.visibility = View.VISIBLE
        layoutModalOverlay.alpha = 0f
        layoutModalOverlay.animate().alpha(1f).setDuration(200).start()
    }

    private fun hideNewChatModal() {
        layoutModalOverlay.animate().alpha(0f).setDuration(180)
            .withEndAction { layoutModalOverlay.visibility = View.GONE }.start()
    }

    private fun showSplashThenChat() {
        val imgDuque = findViewById<ImageView>(R.id.imgSplashDuque)
        val txtName  = findViewById<TextView>(R.id.txtSplashName)
        val txtSub   = findViewById<TextView>(R.id.txtSplashSub)

        imgDuque.scaleX = 0.5f
        imgDuque.scaleY = 0.5f
        imgDuque.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(500)
            .withEndAction {
                txtName.animate().alpha(1f).setDuration(350)
                    .withEndAction {
                        txtSub.animate().alpha(0.6f).setDuration(300)
                            .withEndAction {
                                handler.postDelayed({ transitionToChat() }, 1200)
                            }.start()
                    }.start()
            }.start()
    }

    private fun transitionToChat() {
        layoutSplash.animate().alpha(0f).setDuration(400)
            .withEndAction {
                layoutSplash.visibility = View.GONE
                layoutChat.visibility   = View.VISIBLE
                layoutChat.alpha = 0f
                layoutChat.animate().alpha(1f).setDuration(350)
                    .withEndAction {
                        splashAlreadyShown = true
                        addWelcomeMessage()
                    }.start()
            }.start()
    }

    private fun clearConversation() {
        conversationHistory = JSONArray()
        messages.clear()
        adapter.notifyDataSetChanged()
        setChipsEnabled(true)
        setSendButtonEnabled(true)
        addWelcomeMessage()
        Toast.makeText(this, "Nova conversa iniciada!", Toast.LENGTH_SHORT).show()
    }

    private fun setupChips() {
        val dp = resources.displayMetrics.density
        for (suggestion in chipSuggestions) {
            val chip = TextView(this).apply {
                text = suggestion
                textSize = 13f
                setTextColor(context.getColor(R.color.textColor))
                background = context.getDrawable(R.drawable.bg_chip)
                setPadding((14*dp).toInt(), (8*dp).toInt(), (14*dp).toInt(), (8*dp).toInt())
                isSingleLine = true
                isClickable  = true
                isFocusable  = true
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).also { it.marginEnd = (8*dp).toInt() }
                setOnClickListener { sendChipMessage(suggestion) }
            }
            lnlytChips.addView(chip)
        }
    }

    private fun setChipsEnabled(enabled: Boolean) {
        scrollChips.visibility = if (enabled) View.VISIBLE else View.GONE
    }

    private fun setSendButtonEnabled(enabled: Boolean) {
        buttonSend.isEnabled = enabled
        buttonSend.animate().alpha(if (enabled) 1f else 0.4f).setDuration(150).start()
    }

    private fun sendChipMessage(text: String) {
        if (isRequestRunning) return
        setChipsEnabled(false)
        setSendButtonEnabled(false)
        addMessage(text, true)
        isRequestRunning = true
        addLoading()
        sendMessageToGemini(text) { response ->
            removeLoading()
            addMessage(response, false)
            isRequestRunning = false
            setChipsEnabled(true)
            setSendButtonEnabled(true)
        }
    }

    private fun getCurrentTime(): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("America/Sao_Paulo")
        return sdf.format(Date())
    }

    private fun addMessage(text: String, isUser: Boolean) {
        messages.add(Message(text, isUser, false, getCurrentTime()))
        val position = messages.size - 1
        adapter.notifyItemInserted(position)
        recyclerView.post { recyclerView.smoothScrollToPosition(position) }
    }

    private fun addLoading() {
        messages.add(Message("", false, true, ""))
        loadingIndex = messages.size - 1
        adapter.notifyItemInserted(loadingIndex)
        recyclerView.post { recyclerView.smoothScrollToPosition(loadingIndex) }
        startTypingAnimation()
    }

    private fun removeLoading() {
        typingRunnable?.let { handler.removeCallbacks(it) }
        if (loadingIndex != -1) {
            messages.removeAt(loadingIndex)
            adapter.notifyItemRemoved(loadingIndex)
            loadingIndex = -1
        }
    }

    private fun startTypingAnimation() {
        var dots = 0
        typingRunnable = object : Runnable {
            override fun run() {
                if (loadingIndex == -1) return
                dots = (dots % 3) + 1
                messages[loadingIndex] = Message("Pensando" + ".".repeat(dots), false, true, "")
                adapter.notifyItemChanged(loadingIndex)
                handler.postDelayed(this, 500)
            }
        }
        handler.post(typingRunnable!!)
    }

    fun buttonSendChat(view: View) {
        if (isRequestRunning) return
        val userMessage = editTextInput.text.toString()
        if (userMessage.isBlank()) return

        setChipsEnabled(false)
        setSendButtonEnabled(false)
        addMessage(userMessage, true)
        editTextInput.setText("")
        isRequestRunning = true
        addLoading()

        sendMessageToGemini(userMessage) { response ->
            removeLoading()
            addMessage(response, false)
            isRequestRunning = false
            setSendButtonEnabled(true)
            setChipsEnabled(true)
        }
    }

    fun copyMessageToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Mensagem Duque", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Mensagem copiada!", Toast.LENGTH_SHORT).show()
    }

    // ─────────────────────────────────────────────
    // ASSETS E API
    // ─────────────────────────────────────────────

    private fun loadContextFromAssets(): String {
        return try {
            assets.open("faculdade.txt")
                .bufferedReader()
                .use { it.readText() }
        } catch (e: Exception) {
            ""
        }
    }

    private fun sendMessageToGemini(message: String, callback: (String) -> Unit) {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val userText = if (conversationHistory.length() == 0) {
            val contexto = buildContext()
            "$contexto\n\nPergunta do aluno:\n$message"
        } else {
            message
        }

        val userTurn = JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().put(JSONObject().put("text", userText)))
        }
        conversationHistory.put(userTurn)

        val requestBody = JSONObject()
            .put("contents", conversationHistory)
            .toString()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite:generateContent?key=${BuildConfig.API_KEY}")
            .post(requestBody)
            .build()

        Thread {
            try {
                val response = client.newCall(request).execute()
                val body     = response.body?.string()

                val text = JSONObject(body!!)
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")

                val modelTurn = JSONObject().apply {
                    put("role", "model")
                    put("parts", JSONArray().put(JSONObject().put("text", text)))
                }
                conversationHistory.put(modelTurn)

                runOnUiThread { callback(text) }

            } catch (e: Exception) {
                if (conversationHistory.length() > 0) {
                    val cleaned = JSONArray()
                    for (i in 0 until conversationHistory.length() - 1) {
                        cleaned.put(conversationHistory.get(i))
                    }
                    conversationHistory = cleaned
                }

                val errorMessage = when (e) {
                    is java.net.UnknownHostException   -> "Sem conexão com a internet. Verifique sua rede."
                    is java.net.SocketTimeoutException -> "Tempo de resposta esgotado. Tente novamente."
                    is java.io.IOException             -> "Erro de conexão: ${e.message}"
                    else -> "Erro: ${e.javaClass.simpleName} — ${e.message}"
                }

                Log.e("ChatbotActivity", "Erro Gemini: ${e.javaClass.name} — ${e.message}")
                runOnUiThread { callback(errorMessage) }
            }
        }.start()
    }
}