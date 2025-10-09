package com.example.attendease.student.ui

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.attendease.R
import com.example.attendease.databinding.StudentDashboardScreenBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class StudentDashboardActivity : AppCompatActivity() {
    private lateinit var binding: StudentDashboardScreenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = StudentDashboardScreenBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        // Load default fragment
        loadFragment(ScheduleFragmentActivity())

        // Handle bottom nav selections
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_profile -> loadFragment(ProfileFragmentActivity())
                R.id.nav_history -> loadFragment(HistoryFragmentActivity())
                R.id.nav_schedule -> loadFragment(ScheduleFragmentActivity())
                R.id.nav_scan -> loadFragment(ScanFragmentActivity())
                R.id.nav_join_class -> loadFragment(JoinClassFragmentActivity())
            }
            true
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
