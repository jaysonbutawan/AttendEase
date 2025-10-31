package com.example.attendease.student.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.attendease.R // Ensure you import your R file if you use colors/drawables
import com.example.attendease.databinding.FragmentJoinClassScreenBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

// 💡 Change the base class to BottomSheetDialogFragment
class JoinClassBottomSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentJoinClassScreenBinding? = null
    private val binding get() = _binding!!
    private val currentUser = FirebaseAuth.getInstance().currentUser
   private var roomName: String? =null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentJoinClassScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val roomId = arguments?.getString("roomId")
        val sessionId = arguments?.getString("sessionId")
        val timeScanned = arguments?.getString("timeScanned")
        val scannedDate = arguments?.getString("dateScanned")
         roomName = arguments?.getString("roomName")
        val today = scannedDate ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        if (roomId.isNullOrEmpty() || sessionId.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Missing session information.", Toast.LENGTH_SHORT)
                .show()
            dismiss()
            return
        }

        fetchAndDisplayAttendanceFromParams(roomId, sessionId, timeScanned, today)
    }

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

                // 1. Fetch Session Details
                val sessionSnap = database
                    .child("rooms").child(roomId).child("sessions").child(sessionId)
                    .get().await()

                if (!sessionSnap.exists()) {
                    Toast.makeText(requireContext(), "Session not found.", Toast.LENGTH_SHORT)
                        .show()
                    showLoading(false)
                    return@launch
                }

                // 2. Delay for Firebase sync
                delay(800)

                // 3. Fetch Attendance Record
                val attendanceSnap = database
                    .child("rooms").child(roomId).child("sessions").child(sessionId)
                    .child("attendance").child(today).child(studentId)
                    .get().await()

                if (!attendanceSnap.exists()) {
                    Toast.makeText(
                        requireContext(),
                        "Attendance record not found yet.",
                        Toast.LENGTH_SHORT
                    ).show()
                    showLoading(false)
                    return@launch
                }

                // Extract values safely
                val status = attendanceSnap.child("status").getValue(String::class.java) ?: "absent"
                val lateDuration =
                    attendanceSnap.child("lateDuration").getValue(Int::class.java) ?: 0
                val scanTime = timeScanned
                    ?: attendanceSnap.child("timeScanned").getValue(String::class.java)
                    ?: "N/A"

                val subject =
                    sessionSnap.child("subject").getValue(String::class.java) ?: "Unknown Subject"
                val teacherId = sessionSnap.child("teacherId").getValue(String::class.java) ?: ""
                val sessionStatus =
                    sessionSnap.child("sessionStatus").getValue(String::class.java) ?: "N/A"

                // 4. Fetch teacher’s name
                val teacherSnap = database.child("users").child(teacherId).get().await()
                val instructorName =
                    teacherSnap.child("fullname").getValue(String::class.java)
                        ?: "Unknown Instructor"

                // 5. Update UI
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
                Log.e("JOIN_CLASS_FETCH", " Error fetching attendance: ${e.message}", e)
                Toast.makeText(
                    requireContext(),
                    "Error fetching attendance details.",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private fun updateConfirmationUI(
        status: String,
        lateDuration: Int,
        timeScanned: String,
        subject: String,
        room: String?,
        instructorName: String,
        sessionStatus: String
    ) {
        val context = requireContext()
        // ... (The rest of the logic remains the same)
        when (status.lowercase()) {
            "present" -> {
                binding.statusCard.setCardBackgroundColor(
                    ContextCompat.getColor(context, android.R.color.holo_green_dark)
                )
                binding.lateTitle.text = "Present"
                binding.lateSubtitle.text = "You arrived on time!"
                binding.clockIconLarge.setImageResource(R.drawable.clock)
            }

            "late" -> {
                binding.statusCard.setCardBackgroundColor(
                    ContextCompat.getColor(context, android.R.color.holo_orange_dark)
                )
                binding.lateTitle.text = "Late Arrival"
                binding.lateSubtitle.text = "You are late by $lateDuration minute(s)"
                binding.clockIconLarge.setImageResource(R.drawable.clock)
            }

            "partial" -> {
                binding.statusCard.setCardBackgroundColor(
                    ContextCompat.getColor(context, android.R.color.holo_blue_dark)
                )
                binding.lateTitle.text = "Partial Attendance"
                binding.lateSubtitle.text = "Attendance requires review (low GPS confidence)."
                binding.clockIconLarge.setImageResource(R.drawable.ic_warning)
            }

            else -> {
                binding.statusCard.setCardBackgroundColor(
                    ContextCompat.getColor(context, android.R.color.holo_red_dark)
                )
                binding.lateTitle.text = "Absent"
                binding.lateSubtitle.text = "Attendance not recorded or missing."
                binding.clockIconLarge.setImageResource(R.drawable.ic_close)
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