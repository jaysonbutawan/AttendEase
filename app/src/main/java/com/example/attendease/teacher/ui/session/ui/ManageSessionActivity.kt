package com.example.attendease.teacher.ui.session.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.attendease.databinding.ManageClassScreenBinding
import com.example.attendease.teacher.data.model.ClassSession
import com.example.attendease.teacher.ui.classschedule.ClassScheduleDialog
import com.example.attendease.teacher.ui.dashboard.MainNavigationActivity
import com.example.attendease.teacher.ui.session.adapter.SessionAdapter
import com.example.attendease.teacher.ui.session.viewmodel.SessionListViewModel
import com.google.firebase.database.FirebaseDatabase

class ManageSessionActivity : AppCompatActivity() {

    // View Binding for XML access
    private lateinit var binding: ManageClassScreenBinding

    // Adapter for session list RecyclerView
    private lateinit var sessionAdapter: SessionAdapter

    // ViewModel to manage session data
    private val sessionListViewModel: SessionListViewModel by viewModels()

    // For draggable floating action button (FAB)
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
            binding.tvClassCount.text = "  ${sessions.size}"
        }

        sessionListViewModel.error.observe(this) { error ->
            Toast.makeText(this, "Error: $error", Toast.LENGTH_SHORT).show()
        }

        sessionListViewModel.loadSessions()
    }


    @SuppressLint("ClickableViewAccessibility")
    private fun setupButtons() {
        // Floating Action Button — draggable + add new class
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
                    // Detect click (not drag)
                    if (lastAction == MotionEvent.ACTION_DOWN) {
                        showAddClassDialog()
                    }
                    true
                }
                else -> false
            }
        }

        // Back button → navigate to dashboard
        binding.backBtn.setOnClickListener {
            startActivity(Intent(this, MainNavigationActivity::class.java))
            finish() // Prevents returning to this screen with back press
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
                Toast.makeText(this, "Class started!", Toast.LENGTH_SHORT).show()
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

        // 🔹 Confirmation dialog before deleting
        androidx.appcompat.app.AlertDialog.Builder(this)
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
                        sessionListViewModel.loadSessions() // Refresh list
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
