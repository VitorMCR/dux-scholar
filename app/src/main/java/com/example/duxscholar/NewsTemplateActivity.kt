package com.example.duxscholar

import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import coil.load
import io.noties.markwon.Markwon

class NewsTemplateActivity : AppCompatActivity() {

    private lateinit var txtTitle: TextView
    private lateinit var txtContent: TextView
    private lateinit var txtPostDate: TextView
    private lateinit var txtPostHeader: TextView
    private lateinit var imgDetail: ImageView
    private lateinit var btnBack: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_news_template)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        txtTitle = findViewById(R.id.txtNewTitle)
        txtContent = findViewById(R.id.txtNewContent)
        txtPostDate = findViewById(R.id.txtNewPostDate)
        txtPostHeader = findViewById(R.id.txtNewPostHeader)
        imgDetail = findViewById(R.id.imgNewsDetail)
        btnBack = findViewById(R.id.btnNewBack)

        val title = intent.getStringExtra("TITLE")
        val header = intent.getStringExtra("HEADER")
        val content = intent.getStringExtra("CONTENT") ?: ""
        val image = intent.getStringExtra("IMAGE")
        val date = intent.getStringExtra("DATE")

        txtTitle.text = title
        txtPostHeader.text = header
        txtPostDate.text = "Postado em $date"

        if (!image.isNullOrEmpty() && image != "none") {
            try {
                val base64String = if (image.contains(",")) {
                    image.substringAfter(",")
                } else {
                    image
                }
                val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
                imgDetail.load(imageBytes)
                imgDetail.visibility = View.VISIBLE
            } catch (_: Exception) {
                imgDetail.visibility = View.GONE
            }
        } else {
            imgDetail.visibility = View.GONE
        }

        val markwon = Markwon.create(this)
        markwon.setMarkdown(txtContent, content)

        btnBack.setOnClickListener {
            finish()
        }
    }
}