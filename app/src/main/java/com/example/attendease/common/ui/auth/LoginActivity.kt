package com.example.attendease.common.ui.auth

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.attendease.R // Make sure this is imported for R.id access
import com.example.attendease.common.firebase.AuthRepository
import com.example.attendease.databinding.LoginScreenBinding
import com.example.attendease.teacher.ui.dashboard.MainNavigationActivity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: LoginScreenBinding
    private lateinit var repository: AuthRepository
    private enum class AuthState {  SIGN_IN, SIGN_UP
    }
    private var currentState = AuthState.SIGN_IN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LoginScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = AuthRepository(FirebaseAuth.getInstance())
        setupSegmentedControl()
        binding.loginButton.setOnClickListener {
            handleAuthAction()
        }
        binding.googleButton.setOnClickListener {
            handleGoogleSignIn()
        }
        updateUIForState(AuthState.SIGN_IN)
    }

    private fun setupSegmentedControl() {
        binding.toggleGroup.setOnCheckedChangeListener { group: RadioGroup, checkedId: Int ->
            when (checkedId) {
                R.id.radio_sign_in -> updateUIForState(AuthState.SIGN_IN)
                R.id.radio_sign_up -> updateUIForState(AuthState.SIGN_UP)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateUIForState(state: AuthState) {
        currentState = state
        binding.credentialView.apply {
            when (state) {
                AuthState.SIGN_IN -> {
                    // Sign In State 🔐
                    binding.cardTitle.text = "Welcome Back!"
                    binding.forgotPasswordText.visibility = View.VISIBLE
                    binding.loginButton.text = getString(R.string.log_in)
                }
                AuthState.SIGN_UP -> {
                    // Sign Up State 📝
                    binding.cardTitle.text = "Create Account"
                    binding.forgotPasswordText.visibility = View.GONE
                    binding.loginButton.text = "SIGN UP" // Set button text to "SIGN UP"

                }
            }
        }
    }

    private fun handleAuthAction() {
        val email = binding.emailEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            return
        }


        when (currentState) {
            AuthState.SIGN_IN -> signIn(email, password)
            AuthState.SIGN_UP -> {
                signUp(email, password)
            }
        }
    }

    private fun signIn(email: String, password: String) {
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

    private fun signUp(email: String, password: String) {
        lifecycleScope.launch {
            val result = repository.signUpWithEmail(email, password)
            result.onSuccess {
                Toast.makeText(this@LoginActivity, "Account created successfully. Please sign in.", Toast.LENGTH_LONG).show()
                binding.toggleGroup.check(R.id.radio_sign_in)
                updateUIForState(AuthState.SIGN_IN)
                binding.passwordEditText.text.clear()
            }.onFailure { e ->
                Toast.makeText(this@LoginActivity, "Signup failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun handleGoogleSignIn() {
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
}