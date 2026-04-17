package com.example.duxscholar

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class ChatbotActivity : AppCompatActivity() {

    lateinit var editTextInput: EditText
    lateinit var recyclerView: RecyclerView
    lateinit var adapter: MessageAdapter

    val messages = mutableListOf<Message>()

    var isRequestRunning = false
    var loadingIndex = -1

    private val handler = Handler(Looper.getMainLooper())
    private var typingRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chatbot)

        editTextInput = findViewById(R.id.editTextInput)
        recyclerView = findViewById(R.id.recyclerView)

        adapter = MessageAdapter(messages)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Mensagem inicial
        addMessage("Olá! Meu nome é Duque, seu assistente pessoal!\n\nComo posso te ajudar?", false)

        val btnClose = findViewById<ImageView>(R.id.btnClose)
        btnClose.setOnClickListener { finish() }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun addMessage(text: String, isUser: Boolean) {
        messages.add(Message(text, isUser))
        adapter.notifyItemInserted(messages.size - 1)
        recyclerView.scrollToPosition(messages.size - 1)
    }

    private fun addLoading() {
        messages.add(Message("", false, true))
        loadingIndex = messages.size - 1
        adapter.notifyItemInserted(loadingIndex)
        recyclerView.scrollToPosition(loadingIndex)

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

                messages[loadingIndex] = Message(text, false, true)
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

        addMessage(userMessage, true)
        editTextInput.setText("")

        isRequestRunning = true
        view.isEnabled = false

        addLoading()

        sendMessageToGemini(userMessage) { response ->

            removeLoading()
            addMessage(response, false)

            isRequestRunning = false
            view.isEnabled = true
        }
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

    private fun sendMessageToGemini(message: String, callback: (String) -> Unit) {

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val contexto = loadContextFromAssets()

        val promptCompleto = """
            $contexto
            
            Pergunta do aluno:
            $message
        """.trimIndent()

        val jsonObject = JSONObject()
        val contentsArray = JSONArray()
        val contentObject = JSONObject()
        val partsArray = JSONArray()
        val textObject = JSONObject()

        textObject.put("text", promptCompleto)
        partsArray.put(textObject)
        contentObject.put("parts", partsArray)
        contentsArray.put(contentObject)
        jsonObject.put("contents", contentsArray)

        val requestBody = jsonObject
            .toString()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-lite-preview:generateContent?key=${BuildConfig.API_KEY}")
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

                runOnUiThread {
                    callback(text)
                }

            } catch (e: Exception) {
                runOnUiThread {
                    callback("Erro: ${e.message}")
                }
            }
        }.start()
    }
}