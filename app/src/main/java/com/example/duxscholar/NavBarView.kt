package com.example.duxscholar

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.duxscholar.databinding.LayoutNavbarBinding
import com.google.firebase.auth.FirebaseAuth

class NavbarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private lateinit var auth: FirebaseAuth
    private val binding: LayoutNavbarBinding =
        LayoutNavbarBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        setupListeners()
    }

    private fun setupListeners() {
        binding.imgbtnHome.setOnClickListener {
            val intent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            }
            context.startActivity(intent)
        }

        binding.imgbtnCalendario.setOnClickListener {
            val intent = Intent(context, CalendarActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            }
            context.startActivity(intent)
        }

        binding.imgbtnNoticias.setOnClickListener {
            val intent = Intent(context, NewsActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            }
            context.startActivity(intent)
        }

        binding.imgbtnUsuario.setOnClickListener {
            if (auth.currentUser != null) {
                val intent = Intent(context, StudentProfileActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                }
                context.startActivity(intent)
            } else {
                val intent = Intent(context, LoginActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                }
                context.startActivity(intent)
            }
        }
    }
}