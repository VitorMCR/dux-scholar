package com.example.duxscholar

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.duxscholar.BuildConfig
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
        addMessage("Olá! Sou o assistente da faculdade. Como posso te ajudar?", false)

        val btnClose = findViewById<TextView>(R.id.btnClose)

        btnClose.setOnClickListener {
            finish()
        }

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

    fun buttonSendChat(view: View) {

        if (isRequestRunning) return

        val userMessage = editTextInput.text.toString()

        if (userMessage.isBlank()) return

        addMessage(userMessage, true)
        editTextInput.setText("")

        isRequestRunning = true
        view.isEnabled = false

        sendMessageToGemini(userMessage) { response ->
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

        val client = OkHttpClient.Builder().readTimeout(30, TimeUnit.SECONDS).writeTimeout(30, TimeUnit.SECONDS).build()

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

                if (body == null) {
                    runOnUiThread {
                        callback("Erro: resposta vazia da API")
                    }
                    return@Thread
                }

                val jsonResponse = JSONObject(body)

                if (jsonResponse.has("error")) {
                    val errorMsg = jsonResponse
                        .getJSONObject("error")
                        .getString("message")

                    runOnUiThread {
                        callback("Erro da API: $errorMsg")
                    }
                    return@Thread
                }

                val candidates = jsonResponse.optJSONArray("candidates")

                if (candidates == null || candidates.length() == 0) {
                    runOnUiThread {
                        callback("Erro: resposta sem conteúdo")
                    }
                    return@Thread
                }

                val text = candidates
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