package com.example.duxscholar

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var authListener: FirebaseAuth.AuthStateListener
    lateinit var btnChatbot: Button
    lateinit var imgbtnUser: ShapeableImageView
    lateinit var txtGreet: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        imgbtnUser = findViewById(R.id.imgbtnUser)
        txtGreet = findViewById(R.id.txtGreet)
        auth = Firebase.auth

        authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                txtGreet.text = "Olá, ${auth.currentUser?.displayName}"
                imgbtnUser.setOnClickListener {
                    val intent = Intent(this@MainActivity, StudentProfileActivity::class.java)
                    startActivity(intent)
                }
            } else {
                imgbtnUser.setOnClickListener {
                    val intent = Intent(this@MainActivity, LoginActivity::class.java)
                    startActivity(intent)
                }
            }
        }

        btnChatbot = findViewById(R.id.btnChatbot)

        btnChatbot.setOnClickListener {
            val intent = Intent(this@MainActivity, ChatbotActivity::class.java)
            startActivity(intent)
        }

        val servicesText = findViewById<TextView>(R.id.txtMaisServ)
        servicesText.setOnClickListener {
            val intent = Intent(this, ServicesActivity::class.java)
            startActivity(intent)
        }

        val newsText = findViewById<TextView>(R.id.txtMaisNews)
        newsText.setOnClickListener {
            val intent = Intent(this, NewsActivity::class.java)
        }
    }

    override fun onStart() {
        super.onStart()
        auth.addAuthStateListener(authListener)
    }

    override fun onStop() {
        super.onStop()
        auth.removeAuthStateListener(authListener)
    }
}