package com.example.attendease.teacher.ui.activity

import android.content.Intent
import android.os.Bundle
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
        }

    }

    private fun onSessionClicked(session: ClassSession) {
        val intent = Intent(this, AttendanceListActivity::class.java).apply {
            putExtra("subjectName", session.subject)
            putExtra("sessionDate", session.date)
            putExtra("sessionId", session.sessionId)
            putExtra("roomId", session.roomId)
        }
        startActivity(intent)
    }
}