package com.example.attendease.student.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.attendease.R
import com.example.attendease.databinding.StudentDashboardScreenBinding
import com.example.attendease.student.helper.SessionHelper
import kotlinx.coroutines.*

class StudentDashboardActivity : AppCompatActivity() {

    private var scheduleFragment: ScheduleFragmentActivity? = null
    private var scanFragment: ScanFragmentActivity? = null
    private var profileFragment: ProfileFragmentActivity? = null
    private var historyFragment: HistoryFragmentActivity? = null
    private var joinClassFragment: JoinClassFragmentActivity? = null

    private lateinit var binding: StudentDashboardScreenBinding

    private val scope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = StudentDashboardScreenBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        binding.swipeRefresh.setOnRefreshListener {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)

            val canScrollUp = currentFragment?.view?.canScrollVertically(-1) ?: false
            if (!canScrollUp) {
                // Only refresh if the user is at the top
                refreshDashboard()
            } else {
                binding.swipeRefresh.isRefreshing = false
            }
        }


        // Load default fragment (Schedule)
        loadFragment("schedule")

        // Load active class info
        checkForLiveClass()

        // Join class button navigates to ScanFragment
        binding.joinNowBtn.setOnClickListener {
            loadFragment("scan")
        }

        // BottomNavigationView navigation
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.nav_profile -> loadFragment("profile")
                R.id.nav_history -> loadFragment("history")
                R.id.nav_schedule -> loadFragment("schedule")
                R.id.nav_scan -> loadFragment("scan")
                R.id.nav_join_class -> loadFragment("join_class")
            }
            true
        }
    }

    /**
     * Show/hide fragments to avoid recreating them
     */
    private fun loadFragment(fragmentTag: String) {
        val transaction = supportFragmentManager.beginTransaction()

        // Hide all fragments
        listOf(scheduleFragment, scanFragment, profileFragment, historyFragment, joinClassFragment)
            .forEach { it?.let { transaction.hide(it) } }

        // Show existing fragment or add new
        when(fragmentTag) {
            "schedule" -> {
                if (scheduleFragment == null) {
                    scheduleFragment = ScheduleFragmentActivity()
                    transaction.add(R.id.fragmentContainer, scheduleFragment!!, "schedule")
                } else transaction.show(scheduleFragment!!)
            }
            "scan" -> {
                if (scanFragment == null) {
                    scanFragment = ScanFragmentActivity()
                    transaction.add(R.id.fragmentContainer, scanFragment!!, "scan")
                } else transaction.show(scanFragment!!)
            }
            "profile" -> {
                if (profileFragment == null) {
                    profileFragment = ProfileFragmentActivity()
                    transaction.add(R.id.fragmentContainer, profileFragment!!, "profile")
                } else transaction.show(profileFragment!!)
            }
            "history" -> {
                if (historyFragment == null) {
                    historyFragment = HistoryFragmentActivity()
                    transaction.add(R.id.fragmentContainer, historyFragment!!, "history")
                } else transaction.show(historyFragment!!)
            }
            "join_class" -> {
                if (joinClassFragment == null) {
                    joinClassFragment = JoinClassFragmentActivity()
                    transaction.add(R.id.fragmentContainer, joinClassFragment!!, "join_class")
                } else transaction.show(joinClassFragment!!)
            }
        }

        transaction.commit()
    }

    /**
     * Check Firebase for live (started) sessions that match the student's schedule
     */
    @SuppressLint("SetTextI18n")
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
    private fun refreshDashboard() {
        // Show spinner
        binding.swipeRefresh.isRefreshing = true

        scope.launch {
            try {
                // Reload live session info
                val sessions = SessionHelper.getMatchedSessions()
                val liveClass = sessions.firstOrNull { it.status == "Live" }

                if (liveClass != null) {
                    binding.onClassCard.visibility = View.VISIBLE
                    binding.liveClassHeader.text = "${liveClass.subject}\nis live now in ${liveClass.room}"
                } else {
                    binding.onClassCard.visibility = View.GONE
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                // Stop spinner
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
