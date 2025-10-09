package com.example.attendease.teacher.ui.profile

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.attendease.R
import com.example.attendease.common.firebase.AuthRepository
import com.example.attendease.databinding.ProfileScreenBinding
import com.example.attendease.common.ui.auth.TeacherLoginActivity
import com.google.firebase.auth.FirebaseAuth

class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ProfileScreenBinding
    private val authRepository by lazy { AuthRepository(FirebaseAuth.getInstance()) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ProfileScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        with(binding) {
            val name = intent.getStringExtra("name")
            val image = intent.getStringExtra("image")

            tvUserName.text = name
            if (!image.isNullOrEmpty()) {
                Glide.with(this@ProfileActivity)
                    .load(image)
                    .placeholder(R.drawable.default_avatar)
                    .error(R.drawable.default_avatar)
                    .into(profileImage)
            } else {
                profileImage.setImageResource(R.drawable.default_avatar)
            }
            logoutButton.setOnClickListener {
                try {
                    authRepository.signOut()
                    val intent = Intent(this@ProfileActivity, TeacherLoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finish()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

        }
    }
}
