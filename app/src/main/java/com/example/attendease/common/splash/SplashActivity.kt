package com.example.attendease.common.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope // Required for Coroutines in Activity
import com.example.attendease.common.ui.auth.TeacherLoginActivity
import com.example.attendease.common.ui.auth.StudentLoginActivity
import com.example.attendease.databinding.SplashScreenBinding
import com.example.attendease.student.ui.StudentDashboardActivity
import com.example.attendease.teacher.ui.dashboard.MainNavigationActivity // Teacher Dashboard
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase // Required to read role
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    private lateinit var binding: SplashScreenBinding
    private val database = FirebaseDatabase.getInstance().reference

    private enum class Role {
        STUDENT, TEACHER
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SplashScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val user = FirebaseAuth.getInstance().currentUser

        if (user != null) {
            // User already logged in → Check Role for secure routing
            checkUserRoleAndNavigate(user.uid)
        } else {
            // --- User not logged in → show role selection options ---
            setupRoleSelection()
        }
    }

    /**
     * Fetches the user's role from Firebase and routes them to the correct dashboard.
     */
    private fun checkUserRoleAndNavigate(userId: String) {
        // Use a coroutine for the asynchronous Firebase read
        lifecycleScope.launch {
            try {
                // Read the 'role' field under the user's ID
                val snapshot = database.child("users").child(userId).child("role").get().await()
                val role = snapshot.getValue(String::class.java)

                if (role.equals("teacher", ignoreCase = true)) {
                    startActivity(Intent(this@SplashActivity, MainNavigationActivity::class.java))
                } else if (role.equals("student", ignoreCase = true)) {
                    startActivity(Intent(this@SplashActivity, StudentDashboardActivity::class.java))
                } else {
                    // Fallback if role is missing or invalid
                    FirebaseAuth.getInstance().signOut()
                    setupRoleSelection()
                }
            } catch (e: Exception) {
                // Handle network error, database error, etc.
                FirebaseAuth.getInstance().signOut()
                setupRoleSelection()
            }
            finish()
        }
    }

    /**
     * Sets up the role selection UI and listeners.
     */
    private fun setupRoleSelection() {
        binding.onboardingOptionLayout.visibility = View.VISIBLE
        updateRoleSelection(null)

        binding.teacherCardView.setOnClickListener {
            updateRoleSelection(Role.TEACHER)
            startActivity(Intent(this, TeacherLoginActivity::class.java))
            finish()
        }

        binding.studentCardView.setOnClickListener {
            updateRoleSelection(Role.STUDENT)
            startActivity(Intent(this, StudentLoginActivity::class.java))
            finish()
        }
    }

    /**
     * Updates the UI state of the cards (border color and 'Select' text visibility).
     */
    private fun updateRoleSelection(role: Role?) {
        val isStudentSelected = role == Role.STUDENT
        val isTeacherSelected = role == Role.TEACHER

        (binding.studentCardView as? MaterialCardView)?.isSelected = isStudentSelected
        binding.studentSelect.visibility = if (isStudentSelected) View.VISIBLE else View.GONE

        (binding.teacherCardView as? MaterialCardView)?.isSelected = isTeacherSelected
        binding.teacherSelect.visibility = if (isTeacherSelected) View.VISIBLE else View.GONE
    }
}