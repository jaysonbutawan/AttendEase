package com.example.attendease.teacher.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.attendease.R
import com.example.attendease.common.firebase.AuthRepository
import com.example.attendease.common.ui.auth.TeacherLoginActivity
import com.example.attendease.databinding.ProfileScreenBinding
import com.google.firebase.auth.FirebaseAuth

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ProfileScreenBinding
    private val authRepository by lazy { AuthRepository(FirebaseAuth.getInstance()) }

    // Declare these as nullable vars and initialize in onCreate()
    private var name: String? = null
    private var email: String? = null
    private var image: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ProfileScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        name = intent.getStringExtra("name")
        email = intent.getStringExtra("email")
        image = intent.getStringExtra("image")

        setupProfileUI()
        setupListeners()
    }

    private fun setupProfileUI() = with(binding) {
        tvUserName.text = name ?: "No Name"

        if (!image.isNullOrEmpty()) {
            Glide.with(this@ProfileActivity)
                .load(image)
                .placeholder(R.drawable.default_avatar)
                .error(R.drawable.default_avatar)
                .into(profileImage)
        } else {
            profileImage.setImageResource(R.drawable.default_avatar)
        }
    }

    private fun setupListeners() = with(binding) {

        generalSection.findViewById<View>(R.id.edit_profile).setOnClickListener {
            showEditProfileSheet()
        }

        logoutButton.setOnClickListener {
            showLogoutConfirmationDialog()
        }
    }

    private fun showEditProfileSheet() {
        val editProfileSheet = EditProfileBottomSheet.newInstance(name, email, image)
        editProfileSheet.show(supportFragmentManager, editProfileSheet.tag)
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Confirm Logout")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Logout") { _, _ ->
                try {
                    authRepository.signOut()
                    val intent = Intent(this, TeacherLoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finish()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(true)
            .show()
    }

}