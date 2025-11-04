package com.example.attendease.common.ui.auth

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.attendease.R
import com.example.attendease.common.firebase.AuthRepository
import com.example.attendease.common.splash.SplashActivity
import com.example.attendease.databinding.StudentLoginScreenBinding
import com.example.attendease.student.ui.StudentDashboardActivity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class StudentLoginActivity : AppCompatActivity() {

    private lateinit var binding: StudentLoginScreenBinding
    private lateinit var repository: AuthRepository

    private enum class AuthState { SIGN_IN, SIGN_UP }
    private var currentState = AuthState.SIGN_IN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = StudentLoginScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = AuthRepository(FirebaseAuth.getInstance())

        setupSegmentedControl()

        binding.loginButton.setOnClickListener { handleAuthAction() }
        binding.googleButton.setOnClickListener { handleGoogleSignIn() }
        binding.tvChangeRole.setOnClickListener { toggleRole() }

        updateUIForState(AuthState.SIGN_IN)
    }

    private fun setupSegmentedControl() {
        binding.toggleGroup.setOnCheckedChangeListener { _: RadioGroup, checkedId: Int ->
            when (checkedId) {
                R.id.radio_sign_in -> updateUIForState(AuthState.SIGN_IN)
                R.id.radio_sign_up -> updateUIForState(AuthState.SIGN_UP)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateUIForState(state: AuthState) {
        currentState = state
        when (state) {
            AuthState.SIGN_IN -> {
                binding.cardTitle.text = "Welcome Back!"
                binding.forgotPasswordText.visibility = View.VISIBLE
                binding.loginButton.text = getString(R.string.log_in)
            }
            AuthState.SIGN_UP -> {
                binding.cardTitle.text = "Create Account"
                binding.forgotPasswordText.visibility = View.GONE
                binding.loginButton.text = "Sign Up"
            }
        }
    }

    private fun handleAuthAction() {
        val email = binding.emailEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            AlertDialog.Builder(this@StudentLoginActivity)
                .setTitle("Login Failed")
                .setMessage("Please enter email and password")
                .setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                }
                .setCancelable(true)
                .show()
            return
        }

        when (currentState) {
            AuthState.SIGN_IN -> signIn(email, password)
            AuthState.SIGN_UP -> signUp(email, password)
        }
    }

    private fun signIn(email: String, password: String) {
        binding.progressBar.visibility = View.GONE
        binding.loginButton.isEnabled = true
        lifecycleScope.launch {
            val result = repository.signInWithEmail(email, password, "student")
            binding.progressBar.visibility = View.GONE
            binding.loginButton.isEnabled = true
            result.onSuccess {
                Toast.makeText(this@StudentLoginActivity, "Login Successful", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@StudentLoginActivity, StudentDashboardActivity::class.java))
                finish()
            }.onFailure { e ->
                AlertDialog.Builder(this@StudentLoginActivity)
                    .setTitle("Login Failed")
                    .setMessage("Your credentials is Invalid")
                    .setPositiveButton("OK") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setCancelable(true)
                    .show()
            }
        }
    }

    private fun signUp(email: String, password: String) {
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showInvalidEmailDialog()
            return
        }
        binding.progressBar.visibility = View.GONE
        binding.loginButton.isEnabled = true
        lifecycleScope.launch {
            val result = repository.signUpWithEmail(email, password, "student")
            binding.progressBar.visibility = View.GONE
            binding.loginButton.isEnabled = true
            result.onSuccess {
                AlertDialog.Builder(this@StudentLoginActivity)
                    .setTitle("Success")
                    .setMessage("Your account successfully created")
                    .setPositiveButton("OK") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setCancelable(true)
                    .show()
                binding.toggleGroup.check(R.id.radio_sign_in)
                updateUIForState(AuthState.SIGN_IN)
                binding.passwordEditText.text?.clear()
            }.onFailure { e ->
                Toast.makeText(
                    this@StudentLoginActivity,
                    "Signup failed: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    private fun showInvalidEmailDialog() {
        AlertDialog.Builder(this)
            .setTitle("Invalid Email")
            .setMessage("Please enter a valid email address before signing up.")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(true)
            .show()
    }


    private fun handleGoogleSignIn() {
        binding.progressBar.visibility = View.GONE
        binding.loginButton.isEnabled = true
        lifecycleScope.launch {
            val result = repository.signInWithGoogle(this@StudentLoginActivity, "student")
            binding.progressBar.visibility = View.GONE
            binding.loginButton.isEnabled = true
            result.onSuccess {
                Toast.makeText(this@StudentLoginActivity, "Login Successful", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@StudentLoginActivity, StudentDashboardActivity::class.java))
                finish()
            }.onFailure { e ->
                Toast.makeText(
                    this@StudentLoginActivity,
                    "Google login failed: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    private fun toggleRole() {
        startActivity(Intent(this, SplashActivity::class.java))
    }
}
