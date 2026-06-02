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
import java.io.BufferedReader
import java.io.InputStreamReader
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

    private var noticiasContext: String = ""
    private var infoacademicasContext: String = ""

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
        // Controla se o splash já foi exibido nessa sessão do app
        // Reseta quando o processo do app é encerrado (app fechado de verdade)
        var splashAlreadyShown = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chatbot)

        editTextInput = findViewById(R.id.editTextInput)
        recyclerView = findViewById(R.id.recvChat)
        lnlytChips = findViewById(R.id.lnlytChips)
        scrollChips = findViewById(R.id.scrollChips)
        buttonSend = findViewById(R.id.buttonSend)
        layoutChat = findViewById(R.id.layoutChat)
        layoutSplash = findViewById(R.id.layoutSplash)
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

        FirebaseDatabase.getInstance().getReference("noticias")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val sb = StringBuilder()
                    sb.append("Notícias atuais da instituição:\n")
                    for (data in snapshot.children) {
                        val titulo = data.child("name").value.toString()
                        val resumo = data.child("header").value.toString()
                        sb.append("- Título: $titulo\n")
                        sb.append("  Resumo: $resumo\n")
                    }
                    noticiasContext = sb.toString()
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("Firebase", "Erro ao carregar notícias: ${error.message}")
                }
            })

        FirebaseDatabase.getInstance().getReference("infoacademicas")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val sb = StringBuilder()
                    sb.append("Informações acadêmicas disponíveis no app:\n")
                    for (data in snapshot.children) {
                        val nome = data.child("name").value.toString()
                        sb.append("- $nome\n")
                    }
                    infoacademicasContext = sb.toString()
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("Firebase", "Erro ao carregar infoacademicas: ${error.message}")
                }
            })

        // Se o splash já foi exibido nessa sessão, vai direto pro chat
        if (splashAlreadyShown) {
            layoutSplash.visibility = View.GONE
            layoutChat.visibility = View.VISIBLE
            addMessage(
                "Olá! Meu nome é Duque, seu assistente pessoal!\n\nComo posso te ajudar?",
                false
            )
        } else {
            showSplashThenChat()
        }
    }

    private fun setupButtons() {
        findViewById<ImageView>(R.id.btnClose).setOnClickListener { finish() }

        findViewById<TextView>(R.id.btnNewChat).setOnClickListener {
            showNewChatModal()
        }

        layoutModalOverlay.setOnClickListener {
            hideNewChatModal()
        }

        findViewById<TextView>(R.id.btnModalCancel).setOnClickListener {
            hideNewChatModal()
        }

        findViewById<TextView>(R.id.btnModalConfirm).setOnClickListener {
            hideNewChatModal()
            clearConversation()
        }

        findViewById<LinearLayout>(R.id.layoutModal).setOnClickListener { }
    }

    private fun showNewChatModal() {
        layoutModalOverlay.visibility = View.VISIBLE
        layoutModalOverlay.alpha = 0f
        layoutModalOverlay.animate()
            .alpha(1f)
            .setDuration(200)
            .start()
    }

    private fun hideNewChatModal() {
        layoutModalOverlay.animate()
            .alpha(0f)
            .setDuration(180)
            .withEndAction {
                layoutModalOverlay.visibility = View.GONE
            }.start()
    }

    private fun showSplashThenChat() {
        val imgDuque = findViewById<ImageView>(R.id.imgSplashDuque)
        val txtName = findViewById<TextView>(R.id.txtSplashName)
        val txtSub = findViewById<TextView>(R.id.txtSplashSub)

        imgDuque.scaleX = 0.5f
        imgDuque.scaleY = 0.5f
        imgDuque.animate()
            .alpha(1f).scaleX(1f).scaleY(1f)
            .setDuration(500)
            .withEndAction {
                txtName.animate().alpha(1f).setDuration(350).withEndAction {
                    txtSub.animate().alpha(0.6f).setDuration(300).withEndAction {
                        handler.postDelayed({ transitionToChat() }, 1200)
                    }.start()
                }.start()
            }.start()
    }

    private fun transitionToChat() {
        layoutSplash.animate()
            .alpha(0f)
            .setDuration(400)
            .withEndAction {
                layoutSplash.visibility = View.GONE
                layoutChat.visibility = View.VISIBLE
                layoutChat.alpha = 0f
                layoutChat.animate()
                    .alpha(1f)
                    .setDuration(350)
                    .withEndAction {
                        // Marca que o splash já foi exibido nessa sessão
                        splashAlreadyShown = true
                        addMessage(
                            "Olá! Meu nome é Duque, seu assistente pessoal!\n\nComo posso te ajudar?",
                            false
                        )
                    }.start()
            }.start()
    }

    private fun clearConversation() {
        conversationHistory = JSONArray()
        messages.clear()
        adapter.notifyDataSetChanged()
        setChipsEnabled(true)
        setSendButtonEnabled(true)
        addMessage(
            "Olá! Meu nome é Duque, seu assistente pessoal!\n\nComo posso te ajudar?",
            false
        )
        Toast.makeText(this, "Nova conversa iniciada!", Toast.LENGTH_SHORT).show()
    }

    private fun setupChips() {
        val densityScale = resources.displayMetrics.density

        for (suggestion in chipSuggestions) {
            val chip = TextView(this).apply {
                text = suggestion
                textSize = 13f
                setTextColor(context.getColor(R.color.textColor))
                background = context.getDrawable(R.drawable.bg_chip)
                setPadding(
                    (14 * densityScale).toInt(),
                    (8 * densityScale).toInt(),
                    (14 * densityScale).toInt(),
                    (8 * densityScale).toInt()
                )
                isSingleLine = true
                isClickable = true
                isFocusable = true

                val params = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                params.marginEnd = (8 * densityScale).toInt()
                layoutParams = params

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
        buttonSend.animate()
            .alpha(if (enabled) 1f else 0.4f)
            .setDuration(150)
            .start()
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
        recyclerView.post {
            recyclerView.smoothScrollToPosition(position)
        }
    }

    private fun addLoading() {
        messages.add(Message("", false, true, ""))
        loadingIndex = messages.size - 1
        adapter.notifyItemInserted(loadingIndex)
        recyclerView.post {
            recyclerView.smoothScrollToPosition(loadingIndex)
        }
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
                val text = "Pensando" + ".".repeat(dots)
                messages[loadingIndex] = Message(text, false, true, "")
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

    private fun loadContextFromAssets(): String {
        val builder = StringBuilder()
        try {
            val inputStream = assets.open("faculdade.txt")
            val reader = BufferedReader(InputStreamReader(inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                builder.append(line).append("\n")
            }
            reader.close()
        } catch (e: Exception) {
            return "Erro ao carregar contexto"
        }
        return builder.toString()
    }

    private fun buildContext(): String {
        val base = loadContextFromAssets()
        val sb = StringBuilder()
        sb.append(base)
        sb.append("\n\n")
        if (noticiasContext.isNotEmpty()) {
            sb.append(noticiasContext)
            sb.append("\n")
        }
        if (infoacademicasContext.isNotEmpty()) {
            sb.append(infoacademicasContext)
        }
        return sb.toString()
    }

    private fun sendMessageToGemini(message: String, callback: (String) -> Unit) {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val userText = if (conversationHistory.length() == 0) {
            val contexto = buildContext()
            """
                $contexto
                
                Pergunta do aluno:
                $message
            """.trimIndent()
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
                val body = response.body?.string()

                val jsonResponse = JSONObject(body!!)
                val text = jsonResponse
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
                // Remove a última mensagem do usuário do histórico se a requisição falhou
                if (conversationHistory.length() > 0) {
                    val cleaned = JSONArray()
                    for (i in 0 until conversationHistory.length() - 1) {
                        cleaned.put(conversationHistory.get(i))
                    }
                    conversationHistory.put(cleaned)
                }

                // erros
                val errorMessage = when (e) {
                    is java.net.UnknownHostException -> "Sem conexão com a internet. Verifique sua rede."
                    is java.net.SocketTimeoutException -> "Tempo de resposta esgotado. Tente novamente."
                    is java.io.IOException -> "Erro de conexão: ${e.message}"
                    else -> "Erro: ${e.javaClass.simpleName} — ${e.message}"
                }

                Log.e("ChatbotActivity", "Erro ao chamar Gemini: ${e.javaClass.name} — ${e.message}")

                runOnUiThread { callback(errorMessage) }
            }
        }.start()
    }
}