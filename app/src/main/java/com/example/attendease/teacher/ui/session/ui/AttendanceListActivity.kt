package com.example.attendease.teacher.ui.session.ui

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.attendease.databinding.ActivityAttendanceListBinding
import com.example.attendease.teacher.data.model.AttendanceRecord
import com.example.attendease.teacher.ui.session.adapter.AttendanceAdapter
import com.example.attendease.teacher.ui.session.viewmodel.AttendanceListViewModel

class AttendanceListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAttendanceListBinding
    private lateinit var adapter: AttendanceAdapter
    private var roomId: String? = null
    private var sessionId: String? = null
    private val viewModel: AttendanceListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAttendanceListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val subjectName = intent.getStringExtra("subjectName")
        val sessionDate = intent.getStringExtra("sessionDate")
        val sessionId = intent.getStringExtra("sessionId")
        val roomId = intent.getStringExtra("roomId")

        Log.d("AttendanceList", "🎯 Subject=$subjectName | Session=$sessionId | Room=$roomId")
        setupUI(subjectName, sessionDate)
        setupRecyclerView()
        observeViewModel()

        if (sessionId != null && sessionDate != null) {
            viewModel.fetchAttendanceList(roomId, sessionId,sessionDate)
        } else {
            Toast.makeText(this, "Missing session ID and date", Toast.LENGTH_SHORT).show()
        }
        adapter = AttendanceAdapter(emptyList()) { record ->
            onConfirmPresentClick(record)
        }

    }

    private fun setupUI(subject: String?, date: String?) {
        binding.textClassTitle.text = subject ?: "Unknown Subject"
        binding.textClassDetails.text = "Date: $date"
    }

    private fun setupRecyclerView() {
        adapter = AttendanceAdapter(emptyList())
        binding.recyclerStudentList.layoutManager = LinearLayoutManager(this)
        binding.recyclerStudentList.adapter = adapter
        adapter = AttendanceAdapter(emptyList()) { record ->
            onConfirmPresentClick(record)
        }

    }

    private fun observeViewModel() {
        viewModel.attendanceList.observe(this) { students ->
            Log.d("AttendanceList", "✅ Loaded ${students.size} students")

            if (students.isEmpty()) {
                Toast.makeText(this, "No attendance records found.", Toast.LENGTH_SHORT).show()
            }

            adapter.updateData(students)
        }

        viewModel.error.observe(this) { error ->
            error?.let {
                Log.e("AttendanceList", "❌ $it")
                Toast.makeText(this, "Error: $it", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun onConfirmPresentClick(record: AttendanceRecord) {
        showConfirmPresentDialog(record)
    }

    private fun showConfirmPresentDialog(record: AttendanceRecord) {
        AlertDialog.Builder(this)
            .setTitle("Confirm Attendance")
            .setMessage("Are you sure you want to mark ${record.name} as Present?")
            .setPositiveButton("Confirm") { dialog, _ ->
                val room = roomId ?: return@setPositiveButton
                val session = sessionId ?: return@setPositiveButton

                val dateFormatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                val currentDate = dateFormatter.format(java.util.Date())

                viewModel.updateAttendanceStatus(room, session, currentDate, record)

                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

}
