package com.example.attendease.teacher.ui.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.attendease.databinding.ManageClassScreenBinding
import com.example.attendease.teacher.data.model.ClassSession
import com.example.attendease.teacher.ui.adapter.SessionAdapter
import com.example.attendease.teacher.ui.viewmodel.SessionListViewModel
import com.google.firebase.database.FirebaseDatabase

class ManageSessionActivity : AppCompatActivity() {

    private lateinit var binding: ManageClassScreenBinding

    private lateinit var sessionAdapter: SessionAdapter

    private val sessionListViewModel: SessionListViewModel by viewModels()

    private var dX = 0f
    private var dY = 0f
    private var lastAction = 0

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ManageClassScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        observeSessionList()
        setupButtons()
        binding.editSearchSubject.addTextChangedListener { editable ->
            val query = editable?.toString()?.trim() ?: ""
            sessionAdapter.filter(query)
        }
    }

    // ---------------------------
    // UI Setup and Event Handling
    // ---------------------------

    private fun setupRecyclerView() {
        sessionAdapter = SessionAdapter(
            sessionList = emptyList(),
            onStartClassClick = { session ->
                startSession(session.sessionId, session.roomId)
            },
            onEditClick = { session ->
                // Open edit dialog
                Toast.makeText(this, "Edit ${session.subject}", Toast.LENGTH_SHORT).show()
                showEditDialog(session)
            },
            onDeleteClick = { session ->
                deleteSession(session.roomId, session.sessionId)
            }
        )
        sessionAdapter.onEmptyStateChange = { isEmpty ->
            binding.textEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        }

        binding.classSessionContainer.apply {
            layoutManager = LinearLayoutManager(this@ManageSessionActivity)
            adapter = sessionAdapter
        }
    }

    private fun observeSessionList() {
        sessionListViewModel.sessions.observe(this) { sessions ->
            sessionAdapter = SessionAdapter(
                sessionList = sessions,
                onStartClassClick = { session ->
                    startSession(session.sessionId, session.roomId)
                },
                onEditClick = { session ->
                    showEditDialog(session)
                },
                onDeleteClick = { session ->
                    deleteSession(session.roomId, session.sessionId)
                }
            )
            binding.classSessionContainer.adapter = sessionAdapter
            sessionAdapter.updateData(sessions)
        }

        sessionListViewModel.error.observe(this) { error ->
            Toast.makeText(this, "Error: $error", Toast.LENGTH_SHORT).show()
        }

        sessionListViewModel.loadSessions()
    }


    @SuppressLint("ClickableViewAccessibility")
    private fun setupButtons() {
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
                        showAddClassDialog()
                    }
                    true
                }
                else -> false
            }
        }

        binding.backBtn.setOnClickListener {
            startActivity(Intent(this, MainNavigationActivity::class.java))
            finish()
        }
    }

    // ---------------------------
    // Functional Logic
    // ---------------------------

    /** Marks the session as started in Firebase, then opens the session activity. */
    private fun startSession(sessionId: String?, roomId: String?) {
        if (sessionId.isNullOrEmpty() || roomId.isNullOrEmpty()) {
            Toast.makeText(this, "Invalid session or room ID.", Toast.LENGTH_SHORT).show()
            return
        }

        val sessionRef = FirebaseDatabase.getInstance()
            .getReference("rooms")
            .child(roomId)
            .child("sessions")
            .child(sessionId)

        sessionRef.child("sessionStatus").setValue("started")
            .addOnSuccessListener {
                openSessionActivity(roomId, sessionId)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to start session.", Toast.LENGTH_SHORT).show()
            }
    }
    private fun deleteSession(roomId: String?, sessionId: String?) {
        if (roomId.isNullOrEmpty() || sessionId.isNullOrEmpty()) {
            Toast.makeText(this, "Invalid session data.", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Delete Session")
            .setMessage("Are you sure you want to delete this session? This action cannot be undone.")
            .setPositiveButton("Delete") { dialog, _ ->
                val sessionRef = FirebaseDatabase.getInstance()
                    .getReference("rooms")
                    .child(roomId)
                    .child("sessions")
                    .child(sessionId)

                sessionRef.removeValue()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Session deleted successfully.", Toast.LENGTH_SHORT).show()
                        sessionListViewModel.loadSessions()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Failed to delete session.", Toast.LENGTH_SHORT).show()
                    }

                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(true)
            .show()
    }



    /** Opens the SessionActivity and passes session details. */
    private fun openSessionActivity(roomId: String?, sessionId: String?) {
        val intent = Intent(this, SessionActivity::class.java).apply {
            putExtra("sessionId", sessionId)
            putExtra("roomId", roomId)
        }
        startActivity(intent)
    }

    /** Opens the class creation dialog. */
    private fun showAddClassDialog() {
        val dialog = ClassScheduleDialog()
        dialog.show(supportFragmentManager, "ClassScheduleDialog")
    }
    private fun showEditDialog(session: ClassSession) {
        val dialog = ClassScheduleDialog.newInstance(session)
        dialog.show(supportFragmentManager, "EditClassDialog")
    }

}