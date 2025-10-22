package com.example.attendease.student.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.attendease.R
import com.example.attendease.databinding.StudentDashboardScreenBinding
import com.example.attendease.student.helper.SessionHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.*

class StudentDashboardActivity : AppCompatActivity() {

    private var scheduleFragment: ScheduleFragmentActivity? = null
    private var scanFragment: ScanFragmentActivity? = null
    private var profileFragment: ProfileFragmentActivity? = null
    private var historyFragment: HistoryFragmentActivity? = null
    private var joinClassFragment: JoinClassFragmentActivity? = null

    private lateinit var binding: StudentDashboardScreenBinding
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private var userListener: ValueEventListener? = null
    private lateinit var databaseRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = StudentDashboardScreenBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        // ✅ Initialize Firebase reference for logged-in user
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        databaseRef = FirebaseDatabase.getInstance()
            .getReference("users")
            .child(currentUser.uid)

        // ✅ Listen to user data and update UI
        setupUserListener()

        // ✅ Swipe-to-refresh setup
        binding.swipeRefresh.setOnRefreshListener {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
            val canScrollUp = currentFragment?.view?.canScrollVertically(-1) ?: false
            if (!canScrollUp) {
                refreshDashboard()
            } else {
                binding.swipeRefresh.isRefreshing = false
            }
        }

        // ✅ Load default fragment
        loadFragment("schedule")

        // ✅ Check for live class
        checkForLiveClass()

        // ✅ "Join now" button → Scan fragment
        binding.joinNowBtn.setOnClickListener {
            loadFragment("scan")
        }

        // ✅ Bottom navigation
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_profile -> loadFragment("profile")
                R.id.nav_history -> loadFragment("history")
                R.id.nav_schedule -> loadFragment("schedule")
                R.id.nav_scan -> loadFragment("scan")
            }
            true
        }
    }

    /**
     * 🔹 Listen for changes in user data and update UI
     */
    private fun setupUserListener() {
        userListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (isDestroyed || isFinishing) return

                val currentUser = FirebaseAuth.getInstance().currentUser ?: return

                val fullName = snapshot.child("fullname").getValue(String::class.java)
                val course = snapshot.child("course").getValue(String::class.java)
                var imageUrl = snapshot.child("profileImage").getValue(String::class.java)

                var updated = false // Track if any updates were made

                // ✅ If fullname is missing, set it from Google
                if (fullName.isNullOrEmpty() && !currentUser.displayName.isNullOrEmpty()) {
                    val googleName = currentUser.displayName!!
                    databaseRef.child("fullname").setValue(googleName)
                    updated = true
                }

                // ✅ If profile image is missing, set it from Google photo URL
                if (imageUrl.isNullOrEmpty() && currentUser.photoUrl != null) {
                    val googlePhoto = currentUser.photoUrl.toString()
                    databaseRef.child("profileImage").setValue(googlePhoto)
                    imageUrl = googlePhoto
                    updated = true
                }

                // ✅ Update UI immediately (even if no changes)
                setupUserInfo(
                    name = fullName ?: currentUser.displayName,
                    course = course,
                    imageUrl = imageUrl ?: currentUser.photoUrl?.toString()
                )

                // ✅ Optional: show a one-time toast if we updated something
                if (updated) {
                    Toast.makeText(
                        this@StudentDashboardActivity,
                        "Profile info synced from Google account",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@StudentDashboardActivity,
                    "Failed to load user data: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        databaseRef.addValueEventListener(userListener!!)
    }


    /**
     * 🔹 Display name, course, and image in dashboard header
     */
    private fun setupUserInfo(name: String?, course: String?, imageUrl: String?) = with(binding) {
        userName.text = name ?: "Unknown Student"
        userCourse.text = course ?: "No course assigned"

        if (!this@StudentDashboardActivity.isDestroyed && !this@StudentDashboardActivity.isFinishing) {
            if (!imageUrl.isNullOrEmpty()) {
                Glide.with(this@StudentDashboardActivity)
                    .load(imageUrl)
                    .placeholder(R.drawable.default_avatar)
                    .error(R.drawable.default_avatar)
                    .into(profileImage)
            } else {
                profileImage.setImageResource(R.drawable.default_avatar)
            }
        }
    }

    /**
     * 🔹 Manage fragments
     */
    fun loadFragment(fragmentTag: String) {
        val transaction = supportFragmentManager.beginTransaction()

        // Hide all fragments
        listOf(scheduleFragment, scanFragment, profileFragment, historyFragment, joinClassFragment)
            .forEach { it?.let { transaction.hide(it) } }

        var joinClassFragment = supportFragmentManager.findFragmentByTag("joinClass")
        if (joinClassFragment != null && joinClassFragment.isVisible) {
            transaction.remove(joinClassFragment)
        }

        // Show existing fragment or add new
        when (fragmentTag) {
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
            "joinClass" -> {
                if (joinClassFragment == null) {
                    joinClassFragment = JoinClassFragmentActivity()
                    transaction.add(R.id.fragmentContainer, joinClassFragment!!, "joinClass")
                } else transaction.show(joinClassFragment)
            }
        }

        transaction.commit()
    }

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
        binding.swipeRefresh.isRefreshing = true

        scope.launch {
            try {
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
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        userListener?.let { databaseRef.removeEventListener(it) }
    }
}
