package com.example.attendease.teacher.ui.dashboard

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.attendease.R
import com.example.attendease.databinding.MainNavScreenBinding
import com.example.attendease.teacher.ui.profile.ProfileActivity
import com.example.attendease.teacher.ui.session.ui.ManageSessionActivity
import com.google.firebase.auth.FirebaseAuth

class MainNavigationActivity : AppCompatActivity() {

    private lateinit var binding: MainNavScreenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainNavScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val user = FirebaseAuth.getInstance().currentUser

        setupUserInfo(user?.displayName, user?.email, user?.photoUrl?.toString())
        setupClickListeners(user?.displayName, user?.email, user?.photoUrl?.toString())
    }

    private fun setupUserInfo(name: String?, email: String?, imageUrl: String?) = with(binding) {
        tvUserName.text = name
        if (!imageUrl.isNullOrEmpty()) {
            Glide.with(this@MainNavigationActivity)
                .load(imageUrl)
                .placeholder(R.drawable.default_avatar)
                .error(R.drawable.default_avatar)
                .into(profileImage)
        } else {
            profileImage.setImageResource(R.drawable.default_avatar)
        }
    }

    private fun setupClickListeners(name: String?, email: String?, imageUrl: String?) = with(binding) {
        cvManageClasses.setOnClickListener {
            startActivity(Intent(this@MainNavigationActivity, ManageSessionActivity::class.java))
        }

        cvAttendanceReport.setOnClickListener {
            startActivity(Intent(this@MainNavigationActivity, AttendanceReportActivity::class.java))
        }

        profileImage.setOnClickListener {
            val intent = Intent(this@MainNavigationActivity, ProfileActivity::class.java).apply {
                putExtra("name", name)
                putExtra("email", email)
                putExtra("image", imageUrl)
            }
            startActivity(intent)
        }
    }
}
