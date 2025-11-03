package com.example.attendease.teacher.ui.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.attendease.databinding.AttendanceReportScreenBinding
import com.example.attendease.teacher.data.model.ClassSession
import com.example.attendease.teacher.ui.adapter.ClassHistoryAdapter
import com.example.attendease.teacher.ui.viewmodel.ClassHistoryViewModel

class AttendanceReportActivity : AppCompatActivity() {

    private lateinit var binding: AttendanceReportScreenBinding
    private lateinit var adapter: ClassHistoryAdapter
    private val viewModel: ClassHistoryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AttendanceReportScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        val selectedSubject = intent.getStringExtra("selectedSubject")

        if (!selectedSubject.isNullOrEmpty()) {
            binding.tvClassName.text = selectedSubject
        } else {
            binding.tvClassName.text = "No subject selected"
        }

        observeViewModel(selectedSubject)
        viewModel.fetchClassHistory()

        binding.editSearchStudent.addTextChangedListener { editable ->
            val query = editable?.toString()?.trim() ?: ""
            adapter.filter(query)
        }
    }

    private fun setupRecyclerView() {
        adapter = ClassHistoryAdapter(emptyList()) { session ->
            onSessionClicked(session)
            binding.editSearchStudent.setText("")

        }
        adapter.onEmptyStateChange = { isEmpty ->
            binding.textEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        }


        binding.classHistoryContainer.layoutManager = LinearLayoutManager(this)
        binding.classHistoryContainer.adapter = adapter
    }

    private fun observeViewModel(selectedSubject: String?) {
        viewModel.classHistoryList.observe(this) { sessions ->
            val filteredSessions = if (selectedSubject != null) {
                sessions.filter { it.subject.equals(selectedSubject, ignoreCase = true) }
            } else {
                sessions
            }

            Log.d(
                "AttendanceReport",
                "📚 Filtered ${filteredSessions.size} sessions for subject=$selectedSubject"
            )

            if (filteredSessions.isEmpty()) {
                Toast.makeText(this, "No attendance history for $selectedSubject", Toast.LENGTH_SHORT).show()
            }

            adapter = ClassHistoryAdapter(filteredSessions) { session ->
                onSessionClicked(session)
            }
            binding.classHistoryContainer.adapter = adapter
            adapter.updateData(filteredSessions)
        }

        viewModel.isLoading.observe(this) { isLoading ->
            Log.d("AttendanceReport", "⏳ Loading state: $isLoading")
        }

        viewModel.errorMessage.observe(this) { message ->
            message?.let {
                Log.e("AttendanceReport", "❌ Error fetching class history: $it")
                Toast.makeText(this, "Error: $it", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun onSessionClicked(session: ClassSession) {
        Log.d("AttendanceReport", "🖱️ Session clicked: ${session.subject} | ${session.date}")

        Toast.makeText(
            this,
            "Selected: ${session.subject} on ${session.date}",
            Toast.LENGTH_SHORT
        ).show()

        // 🔹 Navigate to AttendanceListActivity
        val intent = Intent(this, AttendanceListActivity::class.java).apply {
            putExtra("subjectName", session.subject)
            putExtra("sessionDate", session.date)
            putExtra("sessionId", session.sessionId)
            putExtra("roomId", session.roomId)
        }
        startActivity(intent)
    }
}