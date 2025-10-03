package com.example.attendease.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.attendease.ui.auth.LoginActivity
import com.example.attendease.ui.dashboard.MainNavigationActivity
import com.example.attendease.ui.onboarding.OnboardingActivity
import com.google.firebase.auth.FirebaseAuth

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            // ✅ User already logged in
            startActivity(Intent(this, MainNavigationActivity::class.java))
        } else {
            startActivity(Intent(this, OnboardingActivity::class.java))
        }

        finish()
    }
}