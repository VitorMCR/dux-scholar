package com.example.duxscholar

import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.ai.client.generativeai.Chat
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class ChatbotActivity : AppCompatActivity() {
    lateinit var editTextInput: EditText
    lateinit var editTextOutput: EditText
    lateinit var chat: Chat

    var stringBuilder: StringBuilder = java.lang.StringBuilder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chatbot)

        editTextInput = findViewById(R.id.editTextInput)
        editTextOutput = findViewById(R.id.editTextOutput)

        val generativeModel = GenerativeModel (
            modelName = "gemini-pro",
            apiKey = BuildConfig.API_KEY
        )

        chat = generativeModel.startChat(
            history = listOf(
                content(role = "user") { text("Hello, I have 2 dogs in my house.")},
                content(role = "model") { text("Great to meet you. What would you like to know?")}
            )
        )

        stringBuilder.append("Hello, I have 2 dogs in my house.\n")
        stringBuilder.append("Great to meet you. What would you like to know?\n")

        editTextOutput.setText(stringBuilder.toString())

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    fun buttonSendChat(view: View){
        stringBuilder.append(editTextInput.text.toString())
        MainScope().launch {
            val result = chat.sendMessage(editTextInput.text.toString())
            stringBuilder.append(result.text + "\n\n")

            editTextOutput.setText(stringBuilder.toString())
            editTextInput.setText("")
        }
    }
}