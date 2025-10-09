package com.example.attendease.common.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.attendease.common.ui.auth.TeacherLoginActivity
import com.example.attendease.common.ui.auth.StudentLoginActivity
import com.example.attendease.databinding.SplashScreenBinding
import com.example.attendease.teacher.ui.dashboard.MainNavigationActivity
import com.google.firebase.auth.FirebaseAuth
// Import MaterialCardView if using the Material components for the stroke (recommended)
import com.google.android.material.card.MaterialCardView

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    private lateinit var binding: SplashScreenBinding

    // Enum to clearly define the roles
    private enum class Role {
        STUDENT, TEACHER
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SplashScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val user = FirebaseAuth.getInstance().currentUser

        if (user != null) {
            // User already logged in → go to dashboard
            startActivity(Intent(this, MainNavigationActivity::class.java))
            finish()
        } else {
            // --- User not logged in → show onboarding options ---

            // 1. Set initial state (both unselected)
            updateRoleSelection(null)

            // 2. Set up Teacher Card click listener
            binding.teacherCardView.setOnClickListener {
                // Apply selection visuals first
                updateRoleSelection(Role.TEACHER)

                // Navigate after a short delay, to let the user see the selection state change
                // Note: Since you use 'finish()' immediately, the visual change might be too quick.
                // However, following your logic, we navigate immediately.
                startActivity(Intent(this, TeacherLoginActivity::class.java))
                finish()
            }

            // 3. Set up Student Card click listener
            binding.studentCardView.setOnClickListener {
                // Apply selection visuals first
                updateRoleSelection(Role.STUDENT)

                // Navigate immediately
                startActivity(Intent(this, StudentLoginActivity::class.java))
                finish()
            }
        }
    }

    /**
     * Updates the UI state of the cards (border color and 'Select' text visibility).
     * This relies on the CardViews being MaterialCardViews with a stroke selector defined in XML.
     * * @param role The role that was just selected (STUDENT or TEACHER), or null to clear selection.
     */
    private fun updateRoleSelection(role: Role?) {
        val isStudentSelected = role == Role.STUDENT
        val isTeacherSelected = role == Role.TEACHER

        // --- Student Card Update ---
        // 1. Set the 'selected' state on the CardView (triggers the stroke selector defined in res/color/card_stroke_selector.xml)
        (binding.studentCardView as? MaterialCardView)?.isSelected = isStudentSelected

        // 2. Control 'Select' badge visibility
        binding.studentSelect.visibility = if (isStudentSelected) View.VISIBLE else View.GONE


        // --- Teacher Card Update ---
        // 1. Set the 'selected' state on the CardView (triggers the stroke selector)
        (binding.teacherCardView as? MaterialCardView)?.isSelected = isTeacherSelected

        // 2. Control 'Select' badge visibility
        binding.teacherSelect.visibility = if (isTeacherSelected) View.VISIBLE else View.GONE
    }
}