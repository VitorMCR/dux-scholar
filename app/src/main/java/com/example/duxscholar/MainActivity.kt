package com.example.duxscholar

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.util.Base64
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.duxscholar.databinding.ActivityMainBinding
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    lateinit var lnlytNews: LinearLayout
    lateinit var lnyltInfoAcademica: LinearLayout  // ← NOVO
    var databaseReference: DatabaseReference? = null
    lateinit var snapHelper: com.google.android.material.carousel.CarouselSnapHelper
    private var autoScrollJob: Job? = null

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


        lnyltInfoAcademica = findViewById(R.id.lnyltInfoAcademica)

        FirebaseDatabase.getInstance().getReference("infoacademicas")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    lnyltInfoAcademica.removeAllViews()
                    val inflater = LayoutInflater.from(this@MainActivity)

                    for (data in snapshot.children) {
                        val name    = data.child("name").value.toString()
                        val iconB64 = data.child("icon").value.toString()

                        val itemView = inflater.inflate(
                            R.layout.item_service,
                            lnyltInfoAcademica,
                            false
                        )

                        val widthPx  = (107 * resources.displayMetrics.density).toInt()
                        val heightPx = (120 * resources.displayMetrics.density).toInt()
                        val marginPx = (8 * resources.displayMetrics.density).toInt()
                        val params = LinearLayout.LayoutParams(widthPx, heightPx)
                        params.setMargins(marginPx, 0, marginPx, 0)
                        itemView.layoutParams = params

                        itemView.findViewById<TextView>(R.id.txtNome).text = name

                        try {
                            val bytes  = Base64.decode(iconB64, Base64.DEFAULT)
                            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            itemView.findViewById<ImageView>(R.id.imgIcon).setImageBitmap(bitmap)
                        } catch (e: Exception) {
                            Log.e("Firebase", "Erro ao decodificar ícone: ${e.message}")
                        }

                        lnyltInfoAcademica.addView(itemView)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Firebase", "ERRO: ${error.message}")
                }
            })


        val carouselRecyclerView = findViewById<RecyclerView>(R.id.carouselRecyclerView)
        carouselRecyclerView.layoutManager = com.google.android.material.carousel.CarouselLayoutManager(
            com.google.android.material.carousel.FullScreenCarouselStrategy()
        )
        snapHelper = com.google.android.material.carousel.CarouselSnapHelper()
        snapHelper.attachToRecyclerView(carouselRecyclerView)

        val images = mutableListOf<ByteArray>()
        val adapter = CarouselAdapter(images)
        carouselRecyclerView.adapter = adapter

        FirebaseDatabase.getInstance().getReference("slides").addValueEventListener(object : ValueEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onDataChange(snapshot: DataSnapshot) {
                images.clear()
                for (data in snapshot.children) {
                    val imageBase64 = data.child("image").value.toString()

                    try {
                        val imageBytes = Base64.decode(imageBase64, Base64.DEFAULT)
                        images.add(imageBytes)
                    } catch (e: Exception) {
                        Log.e("Firebase", "Erro ao decodificar imagem: ${e.message}")
                    }
                }

                if (images.isEmpty()) {
                    images.add(drawableToByteArray(ResourcesCompat.getDrawable(resources, R.drawable.img_default_mainbanner, null)!!))
                }

                adapter.notifyDataSetChanged()
                startAutoScroll(carouselRecyclerView, images.size)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "ERRO: ${error.message}")
            }
        })

        carouselRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    autoScrollJob?.cancel()
                }
            }
        })
    }

    private fun startAutoScroll(recyclerView: RecyclerView, count: Int) {
        autoScrollJob?.cancel()
        if (count < 2) return

        autoScrollJob = lifecycleScope.launch {
            while (isActive) {
                delay(5_000)
                val layoutManager = recyclerView.layoutManager ?: continue
                val snapView = snapHelper.findSnapView(layoutManager) ?: continue

                val currentPosition = layoutManager.getPosition(snapView)
                val nextPosition = (currentPosition + 1) % count

                recyclerView.smoothScrollToPosition(nextPosition)
            }
        }
    }

    fun drawableToByteArray(drawable: Drawable): ByteArray {
        val bitmap = (drawable as BitmapDrawable).bitmap
        val stream = ByteArrayOutputStream()

        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)

        return stream.toByteArray()
    }
}