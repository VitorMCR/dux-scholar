package com.example.duxscholar

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.example.duxscholar.databinding.ActivityMainBinding
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var authListener: FirebaseAuth.AuthStateListener

    private lateinit var binding: ActivityMainBinding
    lateinit var btnChatbot: Button
    lateinit var imgbtnUser: ShapeableImageView
    lateinit var txtGreet: TextView
    lateinit var imgbtnEditor: ImageButton
    lateinit var lnlytNews: LinearLayout
    var databaseReference: DatabaseReference? = null
    lateinit var imgbtnCalendario: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        imgbtnUser = findViewById(R.id.imgbtnUser)
        imgbtnCalendario = findViewById(R.id.imgbtnCalendario)

        txtGreet = findViewById(R.id.txtGreet)
        txtGreet.text = getString(R.string.mainactivity_greet, "Usuário")

        imgbtnEditor = findViewById(R.id.imgbtnEditor)
        lnlytNews = findViewById(R.id.lnlytNews)
        auth = Firebase.auth

        // Ação do botão user inferior + verificação
        val imgbtnUsuario = findViewById<ImageButton>(R.id.imgbtnUsuario)

        imgbtnUsuario.setOnClickListener {
            if (auth.currentUser != null) {
                val intent = Intent(this, StudentProfileActivity::class.java)
                startActivity(intent)
            } else {
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
            }
        }

        authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                txtGreet.text = getString(R.string.mainactivity_greet, user.displayName?.split(" ")[0])

                lifecycleScope.launch {
                    if (!checkBasicUser()) {
                        imgbtnEditor.visibility = View.VISIBLE
                    }
                }

//                imgbtnUser.setOnClickListener {
//                    val intent = Intent(this@MainActivity, StudentProfileActivity::class.java)
//                    startActivity(intent)
//                }
            } else {
//                imgbtnUser.setOnClickListener {
//                    val intent = Intent(this@MainActivity, LoginActivity::class.java)
//                    startActivity(intent)
//                }
            }
        }

        imgbtnUser.setOnClickListener {
            val intent = Intent(this@MainActivity, LoginActivity::class.java)
            startActivity(intent)
        }

        imgbtnCalendario.setOnClickListener {
            val intent = Intent(this@MainActivity, CalendarActivity::class.java)
            startActivity(intent)
        }



        btnChatbot = findViewById(R.id.btnChatbot)

        btnChatbot.setOnClickListener {
            val intent = Intent(this@MainActivity, ChatbotActivity::class.java)
            startActivity(intent)
        }

        imgbtnEditor.setOnClickListener {
            val intent = Intent(this@MainActivity, EditActivity::class.java)
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
            startActivity(intent)
        }

        databaseReference = FirebaseDatabase.getInstance().getReference("noticias")

        databaseReference!!.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                lnlytNews.removeAllViews()
                val inflater = LayoutInflater.from(this@MainActivity)
                var limit = if (snapshot.childrenCount <= 4) snapshot.childrenCount else 4
                for (data in snapshot.children) {
                    if (limit > 0) {
                        val newLayoutInstance =
                            inflater.inflate(R.layout.item_main_news, lnlytNews, false)
                        newLayoutInstance.findViewById<TextView>(R.id.txtNewstitle).text =
                            data.child("name").value.toString()
                        newLayoutInstance.findViewById<TextView>(R.id.txtNewsdesc).text =
                            data.child("header").value.toString()
                        if ((limit - 1).toInt() == 0) {
                            newLayoutInstance.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                                marginEnd =
                                    (8 * baseContext.resources.displayMetrics.density).toInt()
                            }
                        }

                        lnlytNews.addView(newLayoutInstance)
                        limit -= 1
                    } else break
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "ERRO: ${error.message}")
            }

        })
    }

    private suspend fun checkBasicUser(): Boolean {
        var isBasicUser = false
        for (userType in listOf("alunos", "professores")) {
            databaseReference = FirebaseDatabase.getInstance().getReference(userType)

            val snapshot = databaseReference!!.child(auth.currentUser!!.uid).get().await()
            if (snapshot.exists()) {
                isBasicUser = true
                val profilePic = snapshot.child("pfp").value as String
                loadProfileImage(profilePic, imgbtnUser)

                break
            }
        }

        return isBasicUser
    }

    private fun loadProfileImage(base64String: String, imageView: ImageView) {
        if (base64String == "none" || base64String.isEmpty()) {
            imageView.load(R.drawable.img_default_user)
            return
        }

        try {
            val imageBytes = Base64.decode(base64String, Base64.DEFAULT)

            imageView.load(imageBytes) {
                error(R.drawable.img_default_user)
            }
        } catch (_: Exception) {
            imageView.load(R.drawable.img_default_user)
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