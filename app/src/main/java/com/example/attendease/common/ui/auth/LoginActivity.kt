package com.example.attendease.common.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.attendease.common.firebase.AuthRepository
import com.example.attendease.databinding.LoginScreenBinding
import com.example.attendease.teacher.ui.dashboard.MainNavigationActivity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: LoginScreenBinding
    private lateinit var repository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LoginScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = AuthRepository(FirebaseAuth.getInstance())

        // Handle Google button click
        binding.googleButton.setOnClickListener {
            lifecycleScope.launch {
                val result = repository.signInWithGoogle(this@LoginActivity)
                result.onSuccess {
                    Toast.makeText(this@LoginActivity, "Login Successful", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@LoginActivity, MainNavigationActivity::class.java))
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
        binding.loginButton.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val result = repository.signInWithEmail(email, password)
                result.onSuccess {
                    Toast.makeText(this@LoginActivity, "Login Successful", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@LoginActivity, MainNavigationActivity::class.java))
                    finish()
                }.onFailure { e ->
                    Toast.makeText(
                        this@LoginActivity,
                        "Email login failed: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

//        // Go to signup
//        binding.signUpText.setOnClickListener {
//            startActivity(Intent(this, SignupActivity::class.java))
//        }
    }
}
