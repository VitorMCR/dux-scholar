package com.example.duxscholar

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityOptionsCompat
import com.example.duxscholar.databinding.LayoutNavbarBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class NavbarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val binding: LayoutNavbarBinding =
        LayoutNavbarBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        setupListeners()
    }

    private fun getIndexForActivity(activityClass: Class<*>): Int {
        return when (activityClass) {
            MainActivity::class.java -> 0
            CalendarActivity::class.java -> 1
            NewsActivity::class.java -> 2
            StudentProfileActivity::class.java, LoginActivity::class.java -> 3
            else -> -1
        }
    }

    private fun navigateTo(targetActivity: Class<*>) {
        val currentActivity = context as? Activity
        val currentIndex = currentActivity?.let { getIndexForActivity(it.javaClass) } ?: -1
        val targetIndex = getIndexForActivity(targetActivity)

        if (currentIndex == targetIndex && currentIndex != -1) return

        val intent = Intent(context, targetActivity).apply {
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }

        if (currentActivity != null && currentIndex != -1 && targetIndex != -1) {
            val (enterAnim, exitAnim) = if (targetIndex > currentIndex) {
                // Indo à esquerda
                Pair(R.anim.slide_in_right, R.anim.slide_out_left)
            } else {
                // Indo à direita
                Pair(R.anim.slide_in_left, R.anim.slide_out_right)
            }

            val options = ActivityOptionsCompat.makeCustomAnimation(context, enterAnim, exitAnim)
            context.startActivity(intent, options.toBundle())
        }
    }

    private fun setupListeners() {
        binding.imgbtnHome.setOnClickListener {
            navigateTo(MainActivity::class.java)
        }

        binding.imgbtnCalendario.setOnClickListener {
            if (auth.currentUser != null) {
                navigateTo(CalendarActivity::class.java)
            } else {
                navigateTo(LoginActivity::class.java)
            }
        }

        binding.imgbtnNoticias.setOnClickListener {
            navigateTo(NewsActivity::class.java)
        }

        binding.imgbtnUsuario.setOnClickListener {
            if (auth.currentUser != null) {
                FirebaseDatabase.getInstance().getReference("alunos").child(auth.currentUser!!.uid).get().addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        navigateTo(StudentProfileActivity::class.java)
                    }
                }
            } else {
                navigateTo(LoginActivity::class.java)
            }
        }
    }
}