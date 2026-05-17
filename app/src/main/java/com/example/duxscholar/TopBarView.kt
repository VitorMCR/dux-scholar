package com.example.duxscholar

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.util.Base64
import android.view.LayoutInflater
import android.widget.ImageView
import androidx.appcompat.app.AppCompatDelegate
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.duxscholar.databinding.LayoutUserTopbarBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class TopBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    private lateinit var authListener: FirebaseAuth.AuthStateListener
    private var databaseReference: DatabaseReference? = null
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val sharedPreferences = context.getSharedPreferences("Mode", Context.MODE_PRIVATE)
    private val preferencesEdit = sharedPreferences.edit()
    private val nightModeState = sharedPreferences.getBoolean("nightmode", false)

    private val binding: LayoutUserTopbarBinding =
        LayoutUserTopbarBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        setupListeners()

        if (nightModeState) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
    }

    private fun setupListeners() {
        binding.imgbtnToggleTheme.setOnClickListener {
            if (nightModeState) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                preferencesEdit.putBoolean("nightmode", false)
                preferencesEdit.apply()
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                preferencesEdit.putBoolean("nightmode", true)
                preferencesEdit.apply()
            }
        }

        binding.txtGreet.text = context.getString(R.string.mainactivity_greet, "Usuário")

        authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                binding.txtGreet.text = context.getString(R.string.mainactivity_greet, user.displayName?.split(" ")[0])

                findViewTreeLifecycleOwner()?.lifecycleScope?.launch {
                    if (!checkBasicUser()) {
                        binding.imgbtnEditor.visibility = VISIBLE
                        binding.imgbtnEditor.setOnClickListener {
                            val intent = Intent(context, EditActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                            }
                            context.startActivity(intent)
                        }
                    }
                }
                binding.imgbtnUser.setOnClickListener {
                    val intent = Intent(context, StudentProfileActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    }
                    context.startActivity(intent)
                }
            } else {
                binding.imgbtnUser.setOnClickListener {
                    val intent = Intent(context, LoginActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    }
                    context.startActivity(intent)
                }
            }
        }
    }

    private suspend fun checkBasicUser(): Boolean {
        var isBasicUser = false
        for (userType in listOf("alunos", "professores")) {
            databaseReference = FirebaseDatabase.getInstance().getReference(userType)

            val snapshot = databaseReference!!.child(auth.currentUser!!.uid).get().await()
            if (snapshot.exists()) {
                isBasicUser = true
                val profilePic = snapshot.child("pfp").value as String
                loadProfileImage(profilePic, binding.imgbtnUser)

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

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        auth.addAuthStateListener(authListener)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        auth.removeAuthStateListener(authListener)
    }
}