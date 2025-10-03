package com.example.attendease.ui.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.attendease.R
import com.example.attendease.databinding.OnboardingOptionScreenBinding
import com.example.attendease.ui.auth.LoginActivity
import com.example.attendease.ui.auth.SignupActivity

class OnboardingActivity : AppCompatActivity() {
    private lateinit var binding: OnboardingOptionScreenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = OnboardingOptionScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.loginButton.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
        binding.createAccountButton.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }
}