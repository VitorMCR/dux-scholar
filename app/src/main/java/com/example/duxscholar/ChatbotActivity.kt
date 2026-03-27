package com.example.duxscholar

import android.os.Bundle
import android.view.View
import android.widget.EditText
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

class ChatbotActivity : AppCompatActivity() {

    lateinit var editTextInput: EditText
    lateinit var recyclerView: RecyclerView
    lateinit var adapter: MessageAdapter

    val messages = mutableListOf<Message>()

    var isRequestRunning = false

    val contextoFaculdade = """
        Você é um assistente virtual da faculdade.

        REGRAS:
        - Responda apenas perguntas relacionadas à faculdade
        - Use apenas as informações fornecidas abaixo
        - Se não souber a resposta, diga: Não tenho essa informação
        - Se a pergunta não for sobre a faculdade, diga: Só posso ajudar com assuntos da faculdade

        INFORMAÇÕES DA FACULDADE:

        Cursos disponíveis:
        - Análise e Desenvolvimento de Sistemas (ADS)
        - Logística
        - Gestão Empresarial

        Biblioteca:
        - Funcionamento: das 8h às 22h
        - Disponível para alunos matriculados

        Serviços acadêmicos:
        - Secretaria acadêmica
        - Atendimento ao aluno
        - Emissão de documentos

        Calendário:
        - Segue os feriados nacionais do Brasil
        - Recesso em julho e dezembro
    """.trimIndent()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chatbot)

        editTextInput = findViewById(R.id.editTextInput)
        recyclerView = findViewById(R.id.recyclerView)

        adapter = MessageAdapter(messages)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        addMessage("Olá! Sou o assistente da faculdade. Como posso te ajudar?", false)

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

    private fun sendMessageToGemini(message: String, callback: (String) -> Unit) {

        val client = OkHttpClient()

        val promptCompleto = """
            $contextoFaculdade

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
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${BuildConfig.API_KEY}")
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