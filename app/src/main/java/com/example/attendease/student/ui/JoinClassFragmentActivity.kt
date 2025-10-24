package com.example.attendease.student.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.attendease.databinding.FragmentJoinClassScreenBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class JoinClassFragmentActivity : Fragment() {

    private var _binding: FragmentJoinClassScreenBinding? = null
    private val binding get() = _binding!!
    private val currentUser = FirebaseAuth.getInstance().currentUser

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentJoinClassScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("JOIN_CLASS_DEBUG", "Fragment created with arguments: ${arguments?.keySet()}")


        // ✅ Retrieve passed arguments
        val roomId = arguments?.getString("roomId")
        val sessionId = arguments?.getString("sessionId")
        val timeScanned = arguments?.getString("timeScanned")
        val scannedDate = arguments?.getString("dateScanned")
        val today = scannedDate ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        Log.d("JOIN_CLASS_ARGS", "Room ID: $roomId | Session ID: $sessionId | Time Scanned: $timeScanned | Date: $today")

        if (roomId.isNullOrEmpty() || sessionId.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Missing session information.", Toast.LENGTH_SHORT).show()
            Log.e("JOIN_CLASS_ERROR", "Missing required session parameters.")
            return
        }

        fetchAndDisplayAttendanceFromParams(roomId, sessionId, timeScanned, today)
    }

    /**
     * ✅ Fetch attendance data based on passed roomId/sessionId/date
     */
    private fun fetchAndDisplayAttendanceFromParams(
        roomId: String,
        sessionId: String,
        timeScanned: String?,
        today: String
    ) {
        val studentId = currentUser?.uid ?: return
        showLoading(true)

        lifecycleScope.launch {
            try {
                val database = FirebaseDatabase.getInstance().reference

                Log.d("JOIN_CLASS_FLOW", "Fetching session data for Room: $roomId | Session: $sessionId")

                val sessionSnap = database
                    .child("rooms")
                    .child(roomId)
                    .child("sessions")
                    .child(sessionId)
                    .get()
                    .await()

                if (!sessionSnap.exists()) {
                    Log.e("JOIN_CLASS_FETCH", "Session not found in database.")
                    Toast.makeText(requireContext(), "Session not found.", Toast.LENGTH_SHORT).show()
                    showLoading(false)
                    return@launch
                }

                // ⏳ Give Firebase some time to sync the just-written attendance data
                delay(800)

                val attendancePath = "rooms/$roomId/sessions/$sessionId/attendance/$today/$studentId"
                val attendanceSnap = database
                    .child("rooms")
                    .child(roomId)
                    .child("sessions")
                    .child(sessionId)
                    .child("attendance")
                    .child(today)
                    .child(studentId)
                    .get()
                    .await()

                if (!attendanceSnap.exists()) {
                    Log.w("JOIN_CLASS_FETCH", "No attendance found yet for path: $attendancePath")
                    Toast.makeText(requireContext(), "Attendance record not found yet.", Toast.LENGTH_SHORT).show()
                    showLoading(false)
                    return@launch
                }

                Log.d("JOIN_CLASS_FETCH", "✅ Attendance found for $studentId in $attendancePath")

                // Extract values safely
                val status = attendanceSnap.child("status").getValue(String::class.java) ?: "absent"
                val lateDuration = attendanceSnap.child("lateDuration").getValue(Int::class.java) ?: 0
                val scanTime = timeScanned
                    ?: attendanceSnap.child("timeScanned").getValue(String::class.java)
                    ?: "N/A"

                val subject = sessionSnap.child("subject").getValue(String::class.java) ?: "Unknown Subject"
                val roomName = sessionSnap.child("roomName").getValue(String::class.java)
                    ?: sessionSnap.child("name").getValue(String::class.java)
                    ?: "Unknown Room"
                val teacherId = sessionSnap.child("teacherId").getValue(String::class.java) ?: ""
                val sessionStatus = sessionSnap.child("sessionStatus").getValue(String::class.java) ?: "N/A"

                Log.d(
                    "JOIN_CLASS_VALUES",
                    "Status: $status | Late: $lateDuration | Subject: $subject | Room: $roomName | ScanTime: $scanTime"
                )

                // Fetch teacher’s name
                val teacherSnap = database.child("users").child(teacherId).get().await()
                val instructorName =
                    teacherSnap.child("fullname").getValue(String::class.java) ?: "Unknown Instructor"

                // ✅ Update UI
                updateConfirmationUI(
                    status = status,
                    lateDuration = lateDuration,
                    timeScanned = scanTime,
                    subject = subject,
                    room = roomName,
                    instructorName = instructorName,
                    sessionStatus = sessionStatus
                )

            } catch (e: Exception) {
                Log.e("JOIN_CLASS_FETCH", "❌ Error fetching attendance: ${e.message}", e)
                Toast.makeText(requireContext(), "Error fetching attendance details.", Toast.LENGTH_SHORT).show()
            } finally {
                showLoading(false)
            }
        }
    }

    /**
     * ✅ Update the confirmation UI based on attendance data
     */
    private fun updateConfirmationUI(
        status: String,
        lateDuration: Int,
        timeScanned: String,
        subject: String,
        room: String,
        instructorName: String,
        sessionStatus: String
    ) {
        val context = requireContext()

        Log.d("JOIN_CLASS_UI", "Updating UI — Status: $status | Time: $timeScanned")

        when (status.lowercase()) {
            "present" -> {
                binding.statusCard.setCardBackgroundColor(
                    ContextCompat.getColor(context, android.R.color.holo_green_dark)
                )
                binding.lateTitle.text = "Present"
                binding.lateSubtitle.text = "You arrived on time!"
            }
            "late" -> {
                binding.statusCard.setCardBackgroundColor(
                    ContextCompat.getColor(context, android.R.color.holo_orange_dark)
                )
                binding.lateTitle.text = "Late Arrival"
                binding.lateSubtitle.text = "You are late by $lateDuration minute(s)"
            }
            "partial" -> {
                binding.statusCard.setCardBackgroundColor(
                    ContextCompat.getColor(context, android.R.color.holo_blue_dark)
                )
                binding.lateTitle.text = "Partial Attendance"
                binding.lateSubtitle.text = "Attendance requires review (low GPS confidence)."
            }
            else -> {
                binding.statusCard.setCardBackgroundColor(
                    ContextCompat.getColor(context, android.R.color.holo_red_dark)
                )
                binding.lateTitle.text = "Absent"
                binding.lateSubtitle.text = "Attendance not recorded or missing."
            }
        }

        binding.subjectValue.text = subject
        binding.roomValue.text = room
        binding.instructorValue.text = instructorName
        binding.statusValue.text = sessionStatus.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
        binding.roomTimeText.text = "$room   $timeScanned"
    }

    private fun showLoading(isLoading: Boolean) {
        if (_binding == null) return
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
