package com.example.attendease.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.attendease.R
import com.example.attendease.SignupActivity
import com.example.attendease.data.repositories.AuthRepository
import com.example.attendease.databinding.LoginScreenBinding
import com.example.attendease.ui.dashboard.DashboardActivity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: LoginScreenBinding
    private lateinit var repository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LoginScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()

        // Handle window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login_screen)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize repository
        repository = AuthRepository(FirebaseAuth.getInstance())

        // Handle Google button click
        binding.googleButton.setOnClickListener {
            lifecycleScope.launch {
                val result = repository.signInWithGoogle(this@LoginActivity)
                result.onSuccess {
                    Toast.makeText(this@LoginActivity, "Login Successful", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@LoginActivity, DashboardActivity::class.java))
                    finish()
                }.onFailure { e ->
                    Toast.makeText(
                        this@LoginActivity,
                        "Google login failed: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        // Go to signup
        binding.signUpText.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }
}
