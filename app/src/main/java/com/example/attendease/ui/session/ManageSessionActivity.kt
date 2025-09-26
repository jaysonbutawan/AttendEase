package com.example.attendease.ui.session

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.attendease.R
import com.example.attendease.databinding.ManageClassScreenBinding
import com.example.attendease.ui.classschedule.ClassScheduleDialog

class ManageSessionActivity : AppCompatActivity() {
    private lateinit var binding: ManageClassScreenBinding
    private lateinit var adapter: SessionAdapter
    private val sessionListViewModel: SessionList by viewModels()

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ManageClassScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Setup RecyclerView
        adapter = SessionAdapter(emptyList()) { session ->
            Toast.makeText(this, "Start class: ${session.subject}", Toast.LENGTH_SHORT).show()
        }

        binding.classSessionContainer.layoutManager = LinearLayoutManager(this)
        binding.classSessionContainer.adapter = adapter

        // Observe LiveData from ViewModel
        sessionListViewModel.sessions.observe(this) { sessionList ->
            adapter = SessionAdapter(sessionList) { session ->
                Toast.makeText(this, "Start class: ${session.subject}", Toast.LENGTH_SHORT).show()
            }
            binding.classSessionContainer.adapter = adapter
        }

        sessionListViewModel.error.observe(this) { errorMessage ->
            Toast.makeText(this, "Error: $errorMessage", Toast.LENGTH_SHORT).show()
        }

        // Load sessions from Firebase
        sessionListViewModel.loadSessions()

        // Handle new class button
        binding.newClass.setOnClickListener {
            val dialog = ClassScheduleDialog()
            dialog.show(supportFragmentManager, "ClassScheduleDialog")
        }
    }
}
