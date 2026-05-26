package com.example.duxscholar

import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import io.noties.markwon.Markwon

class ServicesTemplateActivity : AppCompatActivity() {

    lateinit var txtTitle: TextView
    lateinit var txtContent: TextView
    lateinit var btnBack: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_services_template)

        txtTitle = findViewById(R.id.txtServiceTitle)
        txtContent = findViewById(R.id.txtServiceContent)
        btnBack = findViewById(R.id.btnBack)

        val title = intent.getStringExtra("TITLE")
        val content = intent.getStringExtra("CONTENT") ?: ""

        txtTitle.text = title

        val markwon = Markwon.create(this)

        markwon.setMarkdown(txtContent, content)

        btnBack.setOnClickListener {
            finish()
        }
    }
}