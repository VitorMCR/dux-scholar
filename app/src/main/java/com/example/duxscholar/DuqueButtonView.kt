package com.example.duxscholar

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.duxscholar.databinding.LayoutDuqueButtonBinding

class DuqueButtonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    private val binding: LayoutDuqueButtonBinding =
        LayoutDuqueButtonBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        setupListener()
    }

    private fun setupListener() {
        binding.imgbtnDuque.setOnClickListener {
            val intent = Intent(context, ChatbotActivity::class.java)
            context.startActivity(intent)
        }
    }
}