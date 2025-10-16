package com.example.attendease.student.ui

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.attendease.R
import com.example.attendease.databinding.StudentDashboardScreenBinding
import com.example.attendease.student.helper.SessionHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class StudentDashboardActivity : AppCompatActivity() {
    private lateinit var binding: StudentDashboardScreenBinding
    private val database = FirebaseDatabase.getInstance().reference
    private val currentUser = FirebaseAuth.getInstance().currentUser
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = StudentDashboardScreenBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        // Load default fragment
        loadFragment(ScheduleFragmentActivity())

        // Load active class info
        checkForLiveClass()

        // Join class button
        binding.joinNowBtn.setOnClickListener {
            // Navigate to ScanFragmentActivity
            loadFragment(ScanFragmentActivity())
        }

        // Handle bottom navigation
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

    /**
     * Check Firebase for live (started) sessions that match the student's uploaded schedule.
     */
    private fun checkForLiveClass() {
        binding.onClassCard.visibility = View.GONE

        scope.launch {
            val sessions = SessionHelper.getMatchedSessions()
            val liveClass = sessions.firstOrNull { it.status == "Live" }

            if (liveClass != null) {
                binding.onClassCard.visibility = View.VISIBLE
                binding.liveClassHeader.text = "${liveClass.subject}\nis live now in ${liveClass.room}"
            } else {
                binding.onClassCard.visibility = View.GONE
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
