package com.example.attendease.common.ui.auth

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.attendease.common.firebase.AuthRepository
import com.example.attendease.databinding.SignupScreenBinding
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class SignupActivity : AppCompatActivity() {
    private lateinit var binding: SignupScreenBinding
    private lateinit var repository: AuthRepository

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SignupScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = AuthRepository(FirebaseAuth.getInstance())

        // Sign up button
        binding.signUpButton.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()
            val confirmPassword = binding.confirmPasswordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password!=confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val result = repository.signUpWithEmail(email, password)
                result.onSuccess {
                    Toast.makeText(this@SignupActivity, "Account created successfully", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@SignupActivity, LoginActivity::class.java))
                    finish()
                }.onFailure { e ->
                    Toast.makeText(this@SignupActivity, "Signup failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        binding.LoginText.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }
}