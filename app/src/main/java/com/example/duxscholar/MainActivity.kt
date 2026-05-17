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
    private lateinit var binding: ActivityMainBinding
    lateinit var lnlytNews: LinearLayout
    var databaseReference: DatabaseReference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<Button>(R.id.btnChatbot).setOnClickListener {
            val intent = Intent(this@MainActivity, ChatbotActivity::class.java)
            startActivity(intent)
        }

        findViewById<TextView>(R.id.txtMaisServ).setOnClickListener {
            val intent = Intent(this, ServicesActivity::class.java)
            startActivity(intent)
        }

        findViewById<TextView>(R.id.txtMaisNews).setOnClickListener {
            val intent = Intent(this, NewsActivity::class.java)
            startActivity(intent)
        }

        databaseReference = FirebaseDatabase.getInstance().getReference("noticias")
        lnlytNews = findViewById(R.id.lnlytNews)

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
}