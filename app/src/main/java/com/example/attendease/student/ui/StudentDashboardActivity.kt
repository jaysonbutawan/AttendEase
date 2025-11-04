package com.example.attendease.student.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import com.bumptech.glide.Glide
import com.example.attendease.R
import com.example.attendease.databinding.StudentDashboardScreenBinding
import com.example.attendease.student.helper.SessionHelper
import com.example.attendease.student.helper.StudentValidator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

class StudentDashboardActivity : AppCompatActivity() {

    private var scheduleFragment: ScheduleFragmentActivity? = null
    private var scanFragment: ScanFragmentActivity? = null
    private var profileFragment: ProfileFragmentActivity? = null
    private var historyFragment: HistoryFragmentActivity? = null
    private var joinClassFragment: JoinClassBottomSheet? = null

    private lateinit var binding: StudentDashboardScreenBinding
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    private var userListener: ValueEventListener? = null
    private lateinit var databaseRef: DatabaseReference
    var foundRoomName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = StudentDashboardScreenBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        databaseRef = FirebaseDatabase.getInstance()
            .getReference("users")
            .child(currentUser.uid)

        setupUserListener()

        binding.swipeRefresh.setOnRefreshListener {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
            val canScrollUp = currentFragment?.view?.canScrollVertically(-1) ?: false
            if (!canScrollUp)  {
                refreshDashboard()
                updateLiveSessionStatus()
            } else {
                binding.swipeRefresh.isRefreshing = false
            }
        }

        loadFragment("schedule")

        updateLiveSessionStatus()

        binding.joinNowBtn.setOnClickListener {
            when (binding.joinNowBtn.text.toString()) {
                "Join Now" -> {
                    if (ensureValidStudentName()) {
                        loadFragment("scan")
                    }else{  androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Incomplete Profile")
                        .setMessage("Please complete your profile with your full name before continuing.")
                        .setCancelable(false)
                        .setPositiveButton("OK") { dialog, _ ->
                            dialog.dismiss()

                            val name = binding.userName.text?.toString()
                            val editProfileSheet = EditProfileBottomSheetActivity.newInstance(name)
                            editProfileSheet.show(supportFragmentManager, "EditProfileBottomSheetActivity")
                        }
                        .setNegativeButton("Cancel") { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()
                        return@setOnClickListener

                    }
                }
                "Joined" -> {
                    scope.launch {
                        try {
                            val database = FirebaseDatabase.getInstance().reference
                            val roomsRef = database.child("rooms")
                            val today = java.text.SimpleDateFormat("yyyy-MM-dd").format(java.util.Date())

                            val sessions = SessionHelper.getMatchedSessions()
                            val liveClass = sessions.firstOrNull { it.status == "Live" }

                            if (liveClass == null) {
                                Toast.makeText(this@StudentDashboardActivity, "No active session found.", Toast.LENGTH_SHORT).show()
                                return@launch
                            }

                            val roomSnapshot = roomsRef.get().await()
                            var foundRoomId: String? = null
                            for (room in roomSnapshot.children) {
                                val name1 = room.child("name").getValue(String::class.java)
                                val name2 = room.child("roomName").getValue(String::class.java)
                                if (name1 == liveClass.room || name2 == liveClass.room) {
                                    foundRoomId = room.key
                                    foundRoomName =name1 ?:name2

                                    break
                                }
                            }

                            if (foundRoomId == null) {
                                Toast.makeText(this@StudentDashboardActivity, "Room not found.", Toast.LENGTH_SHORT).show()
                                return@launch
                            }

                            val sessionId = liveClass.sessionId
                            val currentUser = FirebaseAuth.getInstance().currentUser ?: return@launch

                            val attendanceRef = database
                                .child("rooms")
                                .child(foundRoomId)
                                .child("sessions")
                                .child(sessionId)
                                .child("attendance")
                                .child(today)
                                .child(currentUser.uid)

                            val attendanceSnap = attendanceRef.get().await()
                            val timeScanned = attendanceSnap.child("timeScanned").getValue(String::class.java) ?: "N/A"


                            val dataToPass = Bundle().apply {
                                putString("roomId", foundRoomId)
                                putString("sessionId", sessionId)
                                putString("timeScanned", timeScanned)
                                putString("roomName",foundRoomName)
                                putString("dateScanned", today)
                            }

                            loadFragment("joinClass", dataToPass)

                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(this@StudentDashboardActivity, "Failed to load session details.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }



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

                var updated = false

                if (fullName.isNullOrEmpty() && !currentUser.displayName.isNullOrEmpty()) {
                    val googleName = currentUser.displayName!!
                    databaseRef.child("fullname").setValue(googleName)
                    updated = true
                }

                if (imageUrl.isNullOrEmpty() && currentUser.photoUrl != null) {
                    val googlePhoto = currentUser.photoUrl.toString()
                    databaseRef.child("profileImage").setValue(googlePhoto)
                    imageUrl = googlePhoto
                    updated = true
                }
                setupUserInfo(
                    name = fullName ?: currentUser.displayName,
                    course = course,
                    imageUrl = imageUrl ?: currentUser.photoUrl?.toString()
                )
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

    fun loadFragment(fragmentTag: String, args: Bundle? = null) {
        if (fragmentTag.equals("scan", true)
            && !StudentValidator.isValidStudentName(binding.userName.text?.toString())
        ) {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Incomplete Profile")
                .setMessage("Please complete your profile with your full name before continuing.")
                .setCancelable(false)
                .setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()

                    val name = binding.userName.text?.toString()
                    val editProfileSheet = EditProfileBottomSheetActivity.newInstance(name)
                    editProfileSheet.show(supportFragmentManager, "EditProfileBottomSheetActivity")
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
            return
        }
        val transaction = supportFragmentManager.beginTransaction()
        listOf(scheduleFragment, scanFragment, profileFragment, historyFragment, joinClassFragment)
            .forEach { it?.let { transaction.hide(it) } }
        var joinClassFragment = supportFragmentManager.findFragmentByTag("joinClass")
        if (joinClassFragment != null && joinClassFragment.isVisible) {
            transaction.remove(joinClassFragment)
        }

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
                if (joinClassFragment == null || args != null) {
                    joinClassFragment?.let { transaction.remove(it) }

                    joinClassFragment = JoinClassBottomSheet().apply {
                        arguments = args
                    }
                    transaction.add(R.id.fragmentContainer, joinClassFragment, "joinClass")
                } else {
                    transaction.show(joinClassFragment)
                }
            }
        }

        transaction.commit()
    }

    private fun refreshDashboard() {
        updateLiveSessionStatus()
        binding.swipeRefresh.isRefreshing = true

        }


    @SuppressLint("SimpleDateFormat", "SetTextI18n")
    private fun updateLiveSessionStatus() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val database = FirebaseDatabase.getInstance()
        val roomsRef = database.getReference("rooms")
        val today = java.text.SimpleDateFormat("yyyy-MM-dd").format(java.util.Date())

        binding.swipeRefresh.isRefreshing = true

        scope.launch {
            try {
                val liveClassData = withContext(Dispatchers.IO) {
                    val sessions = SessionHelper.getMatchedSessions()
                    val liveClass = sessions.firstOrNull { it.status == "Live" } ?: return@withContext null

                    val roomSnapshot = roomsRef.get().await()
                    val foundRoomId = roomSnapshot.children.firstOrNull { room ->
                        val name1 = room.child("name").getValue(String::class.java)
                        val name2 = room.child("roomName").getValue(String::class.java)
                        name1 == liveClass.room || name2 == liveClass.room
                    }?.key ?: return@withContext null

                    val sessionId = liveClass.sessionId
                    val sessionRef = roomsRef.child(foundRoomId).child("sessions").child(sessionId)
                    val sessionSnap = sessionRef.get().await()
                    val sessionStatus = sessionSnap.child("sessionStatus").getValue(String::class.java)
                        ?: sessionSnap.child("status").getValue(String::class.java)

                    val attendanceRef = sessionRef.child("attendance").child(today).child(currentUser.uid)
                    val attendanceSnap = attendanceRef.get().await()

                    Triple(liveClass, foundRoomId, Pair(sessionStatus, attendanceSnap.exists()))
                }

                withContext(Dispatchers.Main) {
                    if (liveClassData == null) {
                        binding.onClassCard.visibility = View.GONE
                        binding.swipeRefresh.isRefreshing = false
                        return@withContext
                    }

                    val (liveClass, foundRoomId, sessionData) = liveClassData
                    val (sessionStatus, attendanceExists) = sessionData

                    binding.onClassCard.visibility = View.VISIBLE
                    binding.liveClassHeader.text =
                        "${liveClass.subject} is live now in ${liveClass.room}"

                    if (sessionStatus.equals("Live", true) || sessionStatus.equals("Started", true)) {
                        if (attendanceExists) {
                            showJoinedState()
                        } else {
                            checkAttendanceFallback(foundRoomId, liveClass.sessionId, currentUser.uid, today)
                        }
                        binding.joinNowBtn.isEnabled = true
                        binding.joinNowBtn.alpha = 1f
                    } else {
                        binding.onClassCard.visibility = View.GONE
                        binding.joinNowBtn.isEnabled = false
                        binding.joinNowBtn.alpha = 0.5f
                    }

                    binding.swipeRefresh.isRefreshing = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    binding.onClassCard.visibility = View.GONE
                    binding.swipeRefresh.isRefreshing = false
                }
            }
        }
    }
    private fun checkAttendanceFallback(foundRoomId: String, sessionId: String, uid: String, today: String) {
        val attendanceRef = FirebaseDatabase.getInstance()
            .getReference("rooms/$foundRoomId/sessions/$sessionId/attendance/$today/$uid")

        attendanceRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    showJoinedState()
                } else {
                    binding.joinNowBtn.text = "Join Now"
                    binding.joinNowBtn.isEnabled = true
                    binding.joinNowBtn.alpha = 1f
                }
            }

            override fun onCancelled(error: DatabaseError) {
                binding.joinNowBtn.text = "Join Now"
                binding.joinNowBtn.isEnabled = true
                binding.joinNowBtn.alpha = 1f
            }
        })
    }

    private fun showJoinedState() {
        binding.joinNowBtn.text = "Joined"
        binding.joinNowBtn.isEnabled = true
        binding.joinNowBtn.alpha = 0.6f
    }

    private fun ensureValidStudentName(): Boolean {
        val currentName = binding.userName.text?.toString()
        return if (!StudentValidator.isValidStudentName(currentName)) {
            Toast.makeText(
                this,
                "Please complete your profile with your full name before continuing.",
                Toast.LENGTH_LONG
            ).show()
            false
        } else {
            true
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        userListener?.let { databaseRef.removeEventListener(it) }
    }
}
