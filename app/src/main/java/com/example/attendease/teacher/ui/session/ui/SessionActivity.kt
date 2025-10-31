package com.example.attendease.teacher.ui.session.ui

import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.attendease.databinding.SessionScreenBinding
import com.example.attendease.teacher.data.model.AttendanceRecord
import com.example.attendease.teacher.data.model.QrUtils
import com.example.attendease.teacher.data.repositories.SessionRepository
import com.example.attendease.teacher.ui.session.adapter.AttendanceAdapter
import com.example.attendease.teacher.ui.session.viewmodel.AttendanceListViewModel
import com.example.attendease.teacher.ui.session.viewmodel.QrSessionViewModel
import com.example.attendease.teacher.ui.session.viewmodel.SessionViewModelFactory
import com.google.android.gms.tasks.Task
import com.google.firebase.database.FirebaseDatabase

class SessionActivity : AppCompatActivity() {

    private lateinit var binding: SessionScreenBinding

    private lateinit var attendanceAdapter: AttendanceAdapter

    private lateinit var qrSessionViewModel: QrSessionViewModel
    private val attendanceListViewModel: AttendanceListViewModel by viewModels()

    private var qrHandler: Handler? = null
    private lateinit var qrRunnable: Runnable

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

        attendanceAdapter = AttendanceAdapter(emptyList()) { record ->
            onConfirmPresentClick(record)
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
        attendanceAdapter = AttendanceAdapter(emptyList()) { record ->
            onConfirmPresentClick(record)
        }
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
            showEndSessionConfirmationDialog()
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

        // Load initial attendance data
        attendanceListViewModel.loadAttendance(room, session)

        // Observe LiveData from ViewModel
        attendanceListViewModel.attendanceList.observe(this) { attendanceList ->
            // Update RecyclerView
            attendanceAdapter.updateData(attendanceList)

            // Update counts in UI
            val presentCount = attendanceList.count { it.status?.lowercase() == "present" || it.status?.lowercase() == "late" }
            val lateCount = attendanceList.count {   it.status?.lowercase() == "partial" }
            val absentCount = attendanceList.count { it.status?.lowercase() == "absent" }

            binding.presentCount.text = "$presentCount"
            binding.tvOutsideCount.text = "$lateCount"
            binding.absentCount.text = "$absentCount"

        }

        // Observe errors separately
        attendanceListViewModel.error.observe(this) { error ->
            Toast.makeText(this, "Error: $error", Toast.LENGTH_SHORT).show()
        }
    }


    // ---------------------------
    // End Session Handling
    // ---------------------------

    private fun showEndSessionConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("End Session")
            .setMessage("Are you sure you want to end this session?")
            .setPositiveButton("Yes") { dialog, _ ->
                dialog.dismiss()
                endSessionConfirmed() // call the actual end session logic here
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun endSessionConfirmed() {
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

        // ✅ Format current date
        val dateFormatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val currentDate = dateFormatter.format(java.util.Date())

        sessionRef.child("sessionStatus").setValue("ended")
            .addOnSuccessListener {
                Toast.makeText(this, "Class ended!", Toast.LENGTH_SHORT).show()
                binding.endClassCard.visibility = View.GONE
                qrHandler?.removeCallbacks(qrRunnable)
                markAbsentForMissingStudents(room, session, session, currentDate)

                Handler(mainLooper).postDelayed({
                }, 2000)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to end session.", Toast.LENGTH_SHORT).show()
            }
    }
    private fun markAbsentForMissingStudents(
        roomId: String,
        previousSessionId: String,
        currentSessionId: String,
        currentDate: String
    ) {
        val database = FirebaseDatabase.getInstance().getReference("rooms").child(roomId)
        val previousAttendanceRef = database.child("sessions").child(previousSessionId).child("attendance")
        val currentAttendanceRef = database.child("sessions").child(currentSessionId).child("attendance").child(currentDate)

        previousAttendanceRef.get().addOnSuccessListener { previousSnapshot ->
            if (!previousSnapshot.exists()) {
                return@addOnSuccessListener
            }

            // ✅ Collect ALL unique student IDs from all previous dates
            val allPreviousStudents = mutableMapOf<String, String>() // studentId -> studentName

            for (dateNode in previousSnapshot.children) {
                for (student in dateNode.children) {
                    val studentId = student.key ?: continue
                    val studentName = student.child("name").getValue(String::class.java) ?: "Unknown Student"
                    allPreviousStudents[studentId] = studentName
                }
            }

            // ✅ Now get current attendance
            currentAttendanceRef.get().addOnSuccessListener { currentSnapshot ->
                val absentTasks = mutableListOf<Task<Void>>()

                for ((studentId, studentName) in allPreviousStudents) {
                    // If student not found today, mark absent
                    if (!currentSnapshot.hasChild(studentId)) {
                        val absentData = mapOf(
                            "name" to studentName,
                            "status" to "absent",
                            "confidence" to "No scan record",
                            "qrValid" to false,
                            "timeScanned" to "N/A",
                            "lateDuration" to 0,
                            "totalOutsideTime" to 0
                        )

                        val task = currentAttendanceRef.child(studentId).setValue(absentData)
                        absentTasks.add(task)
                    }
                }
            }
        }.addOnFailureListener { e ->
        }
    }



    private fun showAttendanceBottomSheet(status: String) {
        val currentList = attendanceListViewModel.attendanceList.value ?: emptyList()
        val bottomSheet = AttendanceListBottomSheet.newInstance(status, ArrayList(currentList))
        bottomSheet.show(supportFragmentManager, "AttendanceListBottomSheet")
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

                // ✅ Get today's date (to match how it’s stored in Firebase)
                val dateFormatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                val currentDate = dateFormatter.format(java.util.Date())

                // ✅ Updated call to include date parameter
                attendanceListViewModel.updateAttendanceStatus(room, session, currentDate, record)

                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    // ---------------------------
    // Lifecycle
    // ---------------------------

    override fun onDestroy() {
        super.onDestroy()
        qrHandler?.removeCallbacksAndMessages(null)
    }
}
