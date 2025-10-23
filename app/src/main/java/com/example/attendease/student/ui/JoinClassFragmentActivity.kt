package com.example.attendease.student.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.attendease.databinding.FragmentJoinClassScreenBinding
import com.example.attendease.student.helper.SessionHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

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
        fetchAndDisplayLatestAttendance()
    }

    private fun fetchAndDisplayLatestAttendance() {
        val studentId = currentUser?.uid ?: return

        // Show loading
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val report = SessionHelper.getLatestAttendanceReport(studentId)
                binding.progressBar.visibility = View.GONE

                if (report == null) {
                    Toast.makeText(requireContext(), "No attendance record found.", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // ✅ Extract fields
                val subject = report["subject"] as? String ?: "Unknown Subject"
                val room = report["roomName"] as? String ?: "Unknown Room"
                val sessionStatus = report["sessionStatus"] as? String ?: "N/A"
                val startTime = report["startTime"] as? String ?: "N/A"
                val endTime = report["endTime"] as? String ?: "N/A"
                val timeScanned = report["timeScanned"] as? String ?: "N/A"
                val status = report["status"] as? String ?: "N/A"
                val lateDuration = (report["lateDuration"] as? Int) ?: 0
                val teacherId = report["teacherId"] as? String ?: ""

                // ✅ Fetch instructor name
                val teacherSnap = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(teacherId)
                    .get()
                    .await()
                val instructorName = teacherSnap.child("fullname").getValue(String::class.java) ?: "Unknown Instructor"

                // ✅ Update header card based on status
                when (status.lowercase()) {
                    "present" -> {
                        binding.statusCard.setCardBackgroundColor(resources.getColor(android.R.color.holo_green_dark, null))
                        binding.lateTitle.text = "Present"
                        binding.lateSubtitle.text = "You arrived on time!"
                    }
                    "late" -> {
                        binding.statusCard.setCardBackgroundColor(resources.getColor(android.R.color.holo_orange_dark, null))
                        binding.lateTitle.text = "Late Arrival"
                        binding.lateSubtitle.text = "You are late by ${lateDuration} min(s)"
                    }
                    else -> {
                        binding.statusCard.setCardBackgroundColor(resources.getColor(android.R.color.holo_red_dark, null))
                        binding.lateTitle.text = "Absent"
                        binding.lateSubtitle.text = "You missed the class."
                    }
                }

                // ✅ Update session details
                binding.subjectValue.text = subject
                binding.roomValue.text = room
                binding.statusValue.text = sessionStatus.replaceFirstChar { it.uppercase() }
                binding.instructorValue.text = instructorName

                // ✅ Show room & scanned time under card
                binding.roomTimeText.text = "$room   $timeScanned"

            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
