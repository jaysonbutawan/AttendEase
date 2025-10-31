package com.example.attendease.teacher.ui.session.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
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
        viewModel.loadSessions()
        binding.editSearchSubject.addTextChangedListener { editable ->
            val query = editable?.toString()?.trim() ?: ""
            adapter.filter(query)
        }
    }

    private fun setupRecyclerView() {
        adapter = HistorySubjectAdapter(emptyList()) { selectedSubject ->
            onSubjectClicked(selectedSubject.subject ?: "Unknown Subject")

        }
        adapter.onEmptyStateChange = { isEmpty ->
            binding.textEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        }

        binding.recyclerViewSubjects.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewSubjects.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.sessions.observe(this) { sessions ->
            if (sessions.isNullOrEmpty()) {
                binding.textEmptyState.visibility =  View.VISIBLE
                Toast.makeText(this, "No subjects found.", Toast.LENGTH_SHORT).show()
            } else {
                adapter = HistorySubjectAdapter(sessions) { selectedSubject ->
                    onSubjectClicked(selectedSubject.subject ?: "Unknown Subject")
                    binding.textEmptyState.visibility = View.GONE

                }
                binding.recyclerViewSubjects.adapter = adapter
                adapter.updateData(sessions)
            }
        }

        viewModel.error.observe(this) { errorMessage ->
            if (!errorMessage.isNullOrEmpty()) {
                Toast.makeText(this, "Error: $errorMessage", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun onSubjectClicked(subjectName: String) {
        Toast.makeText(this, "Selected: $subjectName", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, AttendanceReportActivity::class.java).apply {
            putExtra("selectedSubject", subjectName)
        }
        startActivity(intent)
    }

}
