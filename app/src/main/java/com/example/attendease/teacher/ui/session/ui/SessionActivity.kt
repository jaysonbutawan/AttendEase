package com.example.attendease.teacher.ui.session.ui

import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.attendease.databinding.SessionScreenBinding
import com.example.attendease.teacher.data.model.QrUtils
import com.example.attendease.teacher.data.repositories.SessionRepository
import com.example.attendease.teacher.ui.session.adapter.AttendanceAdapter
import com.example.attendease.teacher.ui.session.viewmodel.AttendanceListViewModel
import com.example.attendease.teacher.ui.session.viewmodel.QrSessionViewModel
import com.example.attendease.teacher.ui.session.viewmodel.SessionViewModelFactory
import com.google.firebase.database.FirebaseDatabase

class SessionActivity : AppCompatActivity() {

    // View Binding for XML access
    private lateinit var binding: SessionScreenBinding

    // RecyclerView Adapter for attendance list
    private lateinit var attendanceAdapter: AttendanceAdapter

    // ViewModels
    private lateinit var qrSessionViewModel: QrSessionViewModel
    private val attendanceListViewModel: AttendanceListViewModel by viewModels()

    // Handler for auto QR refresh
    private var qrHandler: Handler? = null
    private lateinit var qrRunnable: Runnable

    // Session details
    private var roomId: String? = null
    private var sessionId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SessionScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.presentCount.text = "${attendanceListViewModel.attendanceList.value?.size ?: 0} Present"

        binding.tvPresentCount.setOnClickListener {
            showAttendanceBottomSheet("Present")
        }

        binding.tvAbsentCount.setOnClickListener {
            showAttendanceBottomSheet("Absent")
        }

        binding.outsideCard.setOnClickListener {
            showAttendanceBottomSheet("Partial")
        }


        // Get intent data
        sessionId = intent.getStringExtra("sessionId")
        roomId = intent.getStringExtra("roomId")

        // Setup UI components
        setupRecyclerView()
        setupViewModels()
        setupButtons()

        // Start QR updates and observe attendance
        startQrCodeGeneration()
        observeAttendanceList()
    }

    // ---------------------------
    // UI Setup and Initialization
    // ---------------------------

    private fun setupRecyclerView() {
        attendanceAdapter = AttendanceAdapter(emptyList())
        binding.studentAttendanceRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@SessionActivity)
            adapter = attendanceAdapter
        }
    }

    private fun setupViewModels() {
        val repository = SessionRepository()
        val factory = SessionViewModelFactory(repository)
        qrSessionViewModel = factory.create(QrSessionViewModel::class.java)
    }

    private fun setupButtons() {
        // End Class Button
        binding.btnEndClass.setOnClickListener {
            endSession()
        }
    }

    // ---------------------------
    // QR Code Handling
    // ---------------------------

    private fun startQrCodeGeneration() {
        qrRunnable = object : Runnable {
            override fun run() {
                val sessionIdValue = sessionId ?: return
                val roomIdValue = roomId ?: return

                val qrCode = QrUtils.generateQrCode(sessionIdValue)
                qrSessionViewModel.updateQr(roomIdValue, sessionIdValue, qrCode)

                val qrBitmap: Bitmap = QrUtils.generateQrBitmap(qrCode)
                binding.qrImageView.setImageBitmap(qrBitmap)

                qrHandler?.postDelayed(this, 30_000) // regenerate every 30s
            }
        }

        qrHandler = Handler(mainLooper)
        qrHandler?.post(qrRunnable)
    }

    // ---------------------------
    // Attendance Observation
    // ---------------------------

    private fun observeAttendanceList() {
        val session = sessionId
        val room = roomId

        if (session.isNullOrEmpty() || room.isNullOrEmpty()) {
            Toast.makeText(this, "Invalid session or room ID.", Toast.LENGTH_SHORT).show()
            return
        }

        attendanceListViewModel.loadAttendance(room, session)

        attendanceListViewModel.attendanceList.observe(this) { attendanceList ->
            attendanceAdapter.updateData(attendanceList)

            val presentCount = attendanceList.count { it.status?.lowercase() == "present" }
            val lateCount =
                attendanceList.count { it.status?.lowercase() == "late" || it.status?.lowercase() == "partial" }
            val absentCount = attendanceList.count { it.status?.lowercase() == "absent" }

            // ✅ Update UI text views
            binding.presentCount.text = "$presentCount"
            binding.tvOutsideCount.text = "$lateCount"
            binding.absentCount.text = "$absentCount"

            attendanceListViewModel.attendanceList.observe(this) { attendanceList ->
                attendanceAdapter.updateData(attendanceList)
            }

            attendanceListViewModel.error.observe(this) { error ->
                Toast.makeText(this, "Error: $error", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ---------------------------
    // End Session Handling
    // ---------------------------

    private fun endSession() {
        val session = sessionId
        val room = roomId

        if (session.isNullOrEmpty() || room.isNullOrEmpty()) {
            Toast.makeText(this, "Invalid session or room ID.", Toast.LENGTH_SHORT).show()
            return
        }

        val sessionRef = FirebaseDatabase.getInstance()
            .getReference("rooms")
            .child(room)
            .child("sessions")
            .child(session)

        sessionRef.child("sessionStatus").setValue("ended")
            .addOnSuccessListener {
                Toast.makeText(this, "Class ended!", Toast.LENGTH_SHORT).show()
                qrHandler?.removeCallbacks(qrRunnable)
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to end session.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showAttendanceBottomSheet(status: String) {
        val currentList = attendanceListViewModel.attendanceList.value ?: emptyList()
        val bottomSheet = AttendanceListBottomSheet.newInstance(status, ArrayList(currentList))
        bottomSheet.show(supportFragmentManager, "AttendanceListBottomSheet")
    }


    // ---------------------------
    // Lifecycle
    // ---------------------------

    override fun onDestroy() {
        super.onDestroy()
        qrHandler?.removeCallbacksAndMessages(null)
    }
}
