package com.example.attendease.teacher.ui.session.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.attendease.databinding.ActivityHistorySubjectBinding
import com.example.attendease.teacher.ui.session.adapter.HistorySubjectAdapter
import com.example.attendease.teacher.ui.session.viewmodel.SessionListViewModel

class HistorySubjectActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistorySubjectBinding
    private val viewModel: SessionListViewModel by viewModels()
    private lateinit var adapter: HistorySubjectAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityHistorySubjectBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        observeViewModel()
        viewModel.loadSessions() // ✅ Fetch sessions from Firebase
    }

    private fun setupRecyclerView() {
        adapter = HistorySubjectAdapter(emptyList()) { selectedSubject ->
            onSubjectClicked(selectedSubject.subject ?: "Unknown Subject")
        }

        binding.recyclerViewSubjects.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewSubjects.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.sessions.observe(this) { sessions ->
            if (sessions.isNullOrEmpty()) {
                Toast.makeText(this, "No subjects found.", Toast.LENGTH_SHORT).show()
                Log.w("HistorySubjectActivity", "⚠️ No session data received.")
            } else {
                Log.d("HistorySubjectActivity", "✅ Loaded ${sessions.size} subjects.")
                adapter = HistorySubjectAdapter(sessions) { selectedSubject ->
                    onSubjectClicked(selectedSubject.subject ?: "Unknown Subject")
                }
                binding.recyclerViewSubjects.adapter = adapter
            }
        }

        // ✅ Observe error messages
        viewModel.error.observe(this) { errorMessage ->
            if (!errorMessage.isNullOrEmpty()) {
                Toast.makeText(this, "Error: $errorMessage", Toast.LENGTH_SHORT).show()
                Log.e("HistorySubjectActivity", "❌ $errorMessage")
            }
        }
    }

    private fun onSubjectClicked(subjectName: String) {
        Toast.makeText(this, "Selected: $subjectName", Toast.LENGTH_SHORT).show()
        Log.d("HistorySubjectActivity", "➡️ Subject clicked: $subjectName")

        val intent = Intent(this, AttendanceReportActivity::class.java).apply {
            putExtra("selectedSubject", subjectName)
        }
        startActivity(intent)
    }

}
