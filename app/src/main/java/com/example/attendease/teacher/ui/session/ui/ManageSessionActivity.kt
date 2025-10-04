package com.example.attendease.teacher.ui.session.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.attendease.R
import com.example.attendease.teacher.ui.session.adapter.SessionAdapter
import com.example.attendease.databinding.ManageClassScreenBinding
import com.example.attendease.teacher.ui.classschedule.ClassScheduleDialog
import com.example.attendease.teacher.ui.session.viewmodel.SessionListViewModel

class ManageSessionActivity : AppCompatActivity() {
    private lateinit var binding: ManageClassScreenBinding
    private lateinit var adapter: SessionAdapter
    private val sessionListViewModel: SessionListViewModel by viewModels()

    private var dX: Float = 0f
    private var dY: Float = 0f
    private var lastAction: Int = 0

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
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

        // RecyclerView setup
        adapter = SessionAdapter(emptyList()) { session ->
            openSessionActivity(session.roomId, session.sessionId)
        }
        binding.classSessionContainer.layoutManager = LinearLayoutManager(this)
        binding.classSessionContainer.adapter = adapter


        // Observe sessions
        sessionListViewModel.sessions.observe(this) { sessionList ->
            adapter = SessionAdapter(sessionList) { session ->
                openSessionActivity(session.roomId, session.sessionId)
            }
            binding.classSessionContainer.adapter = adapter
            binding.tvClassCount.text = "  "+"${sessionList.size}"
        }

        sessionListViewModel.error.observe(this) { errorMessage ->
            Toast.makeText(this, "Error: $errorMessage", Toast.LENGTH_SHORT).show()
        }

        sessionListViewModel.loadSessions()

        binding.fabAdd.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    dX = view.x - event.rawX
                    dY = view.y - event.rawY
                    lastAction = MotionEvent.ACTION_DOWN
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    view.animate()
                        .x(event.rawX + dX)
                        .y(event.rawY + dY)
                        .setDuration(0)
                        .start()
                    lastAction = MotionEvent.ACTION_MOVE
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (lastAction == MotionEvent.ACTION_DOWN) {
                        // This was a click
                        onFabClicked()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun onFabClicked() {
        val dialog = ClassScheduleDialog()
        dialog.show(supportFragmentManager, "ClassScheduleDialog")
    }

    private fun openSessionActivity(roomId: String?, sessionId: String?) {
        val intent = Intent(this, SessionActivity::class.java).apply {
            putExtra("sessionId", sessionId)
            putExtra("roomId", roomId)
        }
        startActivity(intent)
    }

}