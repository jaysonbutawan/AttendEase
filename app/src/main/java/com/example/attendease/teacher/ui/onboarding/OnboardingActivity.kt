package com.example.attendease.teacher.ui.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.attendease.databinding.SplashScreenBinding
import com.example.attendease.common.ui.auth.LoginActivity

class OnboardingActivity : AppCompatActivity() {
    private lateinit var binding: SplashScreenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SplashScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.loginButton.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
//        binding.createAccountButton.setOnClickListener {
//            startActivity(Intent(this, SignupActivity::class.java))
//        }
    }
}