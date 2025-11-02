package com.example.attendease.teacher.ui.classschedule

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.TimePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.attendease.databinding.ClassSchduleScreenBinding
import com.example.attendease.teacher.data.model.ClassSession
import com.example.attendease.teacher.data.model.Room
import com.example.attendease.teacher.data.repositories.SessionRepository
import com.example.attendease.teacher.ui.classschedule.viewModel.RoomListViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import java.util.Calendar

class ClassScheduleDialog : DialogFragment() {

    private var _binding: ClassSchduleScreenBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: RoomListViewModel
    private val repo = SessionRepository()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ClassSchduleScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("DefaultLocale")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val oldRoomId = arguments?.getString("roomId")
        val sessionId = arguments?.getString("sessionId")

        // Prefill fields for editing
        arguments?.let { args ->
            binding.editTextSubject.setText(args.getString("subject"))
            binding.startTimePicker.setText(args.getString("startTime"))
            binding.endTimePicker.setText(args.getString("endTime"))
            binding.btnSchedule.text = if (sessionId != null) "Update Session" else "Create Session"
        }
        binding.btnClose.setOnClickListener {
            dismiss()
        }

        // Initialize ViewModel
        viewModel = ViewModelProvider(requireActivity())[RoomListViewModel::class.java]

        // Observe room list
        viewModel.rooms.observe(viewLifecycleOwner) { roomList ->
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, roomList)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerRoom.adapter = adapter

            // Preselect the old room if editing an existing session
            oldRoomId?.let { id ->
                val index = roomList.indexOfFirst { it.roomId == id }
                if (index != -1) binding.spinnerRoom.setSelection(index)
            }
        }
        viewModel.loadRooms()

        // Time pickers
        setupTimePicker(binding.startTimePicker, "Start")
        setupTimePicker(binding.endTimePicker, "End")

        // Handle create or update
        binding.btnSchedule.setOnClickListener {
            handleScheduleAction(sessionId, oldRoomId)
        }
    }

    private fun setupTimePicker(view: View, label: String) {
        view.setOnClickListener {
            val cal = Calendar.getInstance()
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val minute = cal.get(Calendar.MINUTE)
            TimePickerDialog(requireContext(), { _, h, m ->
                val amPm = if (h < 12) "AM" else "PM"
                val formattedHour = if (h % 12 == 0) 12 else h % 12
                val formatted = String.format("%02d:%02d %s", formattedHour, m, amPm)
                when (label) {
                    "Start" -> binding.startTimePicker.setText(formatted)
                    "End" -> binding.endTimePicker.setText(formatted)
                }
            }, hour, minute, false).show()
        }
    }

    private fun handleScheduleAction(sessionId: String?, oldRoomId: String?) {
        val selectedRoom = binding.spinnerRoom.selectedItem as? Room
        val subject = binding.editTextSubject.text.toString().trim()
        val startTime = binding.startTimePicker.text.toString()
        val endTime = binding.endTimePicker.text.toString()

        if (selectedRoom == null) {
            Toast.makeText(requireContext(), "Please select a room.", Toast.LENGTH_SHORT).show()
            return
        }

        val newRoomId = selectedRoom.roomId ?: ""
        if (subject.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a subject.", Toast.LENGTH_SHORT).show()
            return
        }

        if (sessionId.isNullOrEmpty()) {
            // CREATE NEW SESSION
            createNewSession(subject, startTime, endTime, newRoomId)
        } else {
            // UPDATE EXISTING SESSION
            if (oldRoomId == newRoomId) {
                updateExistingSession(oldRoomId, sessionId, subject, startTime, endTime)
            } else {
                confirmRoomChange(oldRoomId!!, newRoomId, sessionId, subject, startTime, endTime)
            }
        }
    }

    private fun createNewSession(subject: String, startTime: String, endTime: String, roomId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
        if (startTime.isEmpty() || endTime.isEmpty()) {
            showAlertDialog("Invalid Time", "Please select both start and end times.")
            return@launch
        }

        val newStart = parseTimeToMinutes(startTime)
        val newEnd = parseTimeToMinutes(endTime)

        // Validate logical time order
        if (newStart == newEnd || newStart > newEnd) {
            showAlertDialog("Invalid Schedule", "End time must be greater than start time.")
            return@launch
        }

        // Prevent weird raw inputs like "0000" or "1234" (missing colon or AM/PM)
        val timeFormatRegex = Regex("""^(0[1-9]|1[0-2]):[0-5][0-9]\s?(AM|PM)$""")
        if (!timeFormatRegex.matches(startTime.uppercase()) || !timeFormatRegex.matches(endTime.uppercase())) {
            showAlertDialog("Invalid Time Format", "Please use valid time format (e.g. 12:00 AM, 01:30 PM).")
            return@launch
        }

        val dbRef = FirebaseDatabase.getInstance().getReference("rooms")

        dbRef.get().addOnSuccessListener { roomsSnapshot ->
            var hasConflict = false
            var conflictDetails = ""

            // Loop through all rooms and their sessions
            roomsSnapshot.children.forEach { roomSnap ->
                val currentTeacherId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

                roomSnap.child("sessions").children.forEach { sessionSnap ->
                    val existingSubject = sessionSnap.child("subject").getValue(String::class.java) ?: ""
                    val existingStart = parseTimeToMinutes(sessionSnap.child("startTime").getValue(String::class.java) ?: "")
                    val existingEnd = parseTimeToMinutes(sessionSnap.child("endTime").getValue(String::class.java) ?: "")
                    val existingRoomId = roomSnap.key ?: ""
                    val existingTeacherId = sessionSnap.child("teacherId").getValue(String::class.java) ?: ""

                    val overlaps = (newStart < existingEnd && newEnd > existingStart)

                    // Check if subject overlaps for same teacher
                    if (existingSubject.equals(subject, ignoreCase = true) && overlaps) {
                        hasConflict = true
                        conflictDetails = "The subject time overlaps with another schedule in the room selected."
                        return@forEach
                    }

                    // Check if the same room is in use by any class at the same time
                    if (existingRoomId == roomId && overlaps) {
                        hasConflict = true
                        conflictDetails = "Room selected already have session between ${sessionSnap.child("startTime").value} and ${sessionSnap.child("endTime").value}."
                        return@forEach
                    }
                    if (existingTeacherId == currentTeacherId && overlaps) {
                        hasConflict = true
                        conflictDetails = "You already have another session to this time you selected."
                        return@forEach
                    }
                }

                if (hasConflict) return@forEach
            }

            if (hasConflict) {
                showAlertDialog("Schedule Conflict", conflictDetails)
                return@addOnSuccessListener
            }

            val session = ClassSession(
                roomId = roomId,
                subject = subject,
                date = "",
                startTime = startTime,
                endTime = endTime,
                allowanceTime = 10,
                teacherId = FirebaseAuth.getInstance().currentUser?.uid ?: "",
                qrCode = ""
            )

            repo.createSession(session) { success, sessionKey ->
                if (success) {
                    showAlertDialog("Success", "New session created successfully.")
                    dismiss()
                } else {
                    showAlertDialog("Error", "Failed to create new session. Please try again.")
                }
            }

        }.addOnFailureListener {
            showAlertDialog("Error", "Failed to fetch room data: ${it.message}")
        }

        } catch (e: Exception) {
            showAlertDialog("Error", "Unexpected error: ${e.message}")
        }
    }
}

    private fun parseTimeToMinutes(time: String): Int {
        return try {
            val format = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
            val date = format.parse(time)
            val cal = Calendar.getInstance()
            cal.time = date!!  // Correct way to set the time
            cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        } catch (e: Exception) {
            0
        }
    }

    private fun updateExistingSession(
        roomId: String,
        sessionId: String,
        subject: String,
        startTime: String,
        endTime: String
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                if (startTime.isEmpty() || endTime.isEmpty()) {
                    showAlertDialog("Invalid Time", "Please select both start and end times.")
                    return@launch
                }

                val newStart = parseTimeToMinutes(startTime)
                val newEnd = parseTimeToMinutes(endTime)

                // Validate logical order
                if (newStart == newEnd || newStart > newEnd) {
                    showAlertDialog("Invalid Schedule", "End time must be greater than start time.")
                    return@launch
                }

                // Validate time format (e.g. 12:00 PM)
                val timeFormatRegex = Regex("""^(0[1-9]|1[0-2]):[0-5][0-9]\s?(AM|PM)$""")
                if (!timeFormatRegex.matches(startTime.uppercase()) || !timeFormatRegex.matches(endTime.uppercase())) {
                    showAlertDialog("Invalid Time Format", "Please use valid time format (e.g. 12:00 AM, 01:30 PM).")
                    return@launch
                }

                val dbRef = FirebaseDatabase.getInstance().getReference("rooms")

                dbRef.get().addOnSuccessListener { roomsSnapshot ->
                    var hasConflict = false
                    var conflictDetails = ""

                    val currentTeacherId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

                    // Loop through all rooms and sessions to check for conflicts
                    roomsSnapshot.children.forEach { roomSnap ->
                        roomSnap.child("sessions").children.forEach { sessionSnap ->
                            val existingId = sessionSnap.key ?: ""
                            if (existingId == sessionId) return@forEach // Skip the current session

                            val existingSubject = sessionSnap.child("subject").getValue(String::class.java) ?: ""
                            val existingStart = parseTimeToMinutes(sessionSnap.child("startTime").getValue(String::class.java) ?: "")
                            val existingEnd = parseTimeToMinutes(sessionSnap.child("endTime").getValue(String::class.java) ?: "")
                            val existingRoomId = roomSnap.key ?: ""
                            val existingTeacherId = sessionSnap.child("teacherId").getValue(String::class.java) ?: ""

                            val overlaps = (newStart < existingEnd && newEnd > existingStart)

                            // Same room conflict
                            if (existingRoomId == roomId && overlaps) {
                                hasConflict = true
                                conflictDetails = "Room selected already has a session between ${sessionSnap.child("startTime").value} and ${sessionSnap.child("endTime").value}."
                                return@forEach
                            }

                            // Teacher double-booking
                            if (existingTeacherId == currentTeacherId && overlaps) {
                                hasConflict = true
                                conflictDetails = "You already have another session during the selected time."
                                return@forEach
                            }

                            // Same subject overlap
                            if (existingSubject.equals(subject, ignoreCase = true) && overlaps) {
                                hasConflict = true
                                conflictDetails = "This subject overlaps with another schedule."
                                return@forEach
                            }
                        }
                        if (hasConflict) return@forEach
                    }

                    if (hasConflict) {
                        showAlertDialog("Schedule Conflict", conflictDetails)
                        return@addOnSuccessListener
                    }

                    // No conflict — proceed with update
                    val updates = mapOf(
                        "subject" to subject,
                        "startTime" to startTime,
                        "endTime" to endTime
                    )

                    dbRef.child(roomId)
                        .child("sessions")
                        .child(sessionId)
                        .updateChildren(updates)
                        .addOnSuccessListener {
                            showAlertDialog("Success", "Session updated successfully")
                            dismiss()
                        }
                        .addOnFailureListener {
                            showAlertDialog("Error", "Failed to update session: ${it.message}")
                        }

                }.addOnFailureListener {
                    showAlertDialog("Error", "Failed to fetch room data: ${it.message}")
                }

            } catch (e: Exception) {
                showAlertDialog("Error", "Unexpected error: ${e.message}")
            }
        }
    }


    private fun confirmRoomChange(oldRoomId: String, newRoomId: String, sessionId: String, subject: String, start: String, end: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Move session to a new room?")
            .setMessage("You are moving this session from $oldRoomId to $newRoomId. Continue?")
            .setPositiveButton("Yes") { _, _ ->
                moveSession(oldRoomId, newRoomId, sessionId, subject, start, end)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun moveSession(oldRoomId: String, newRoomId: String, sessionId: String, subject: String, start: String, end: String) {
        val oldRef = FirebaseDatabase.getInstance()
            .getReference("rooms/$oldRoomId/sessions/$sessionId")

        oldRef.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                return@addOnSuccessListener
            }

            val sessionData = snapshot.value
            val newRef = FirebaseDatabase.getInstance()
                .getReference("rooms/$newRoomId/sessions/$sessionId")

            newRef.setValue(sessionData).addOnSuccessListener {
                newRef.updateChildren(
                    mapOf(
                        "subject" to subject,
                        "startTime" to start,
                        "endTime" to end,
                        "roomId" to newRoomId
                    )
                ).addOnSuccessListener {
                    oldRef.removeValue().addOnSuccessListener {
                        showAlertDialog("Success", "Session moved successfully")
                        dismiss()
                    }
                }
            }
        }
    }
    private fun showAlertDialog(title: String, message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    companion object {
        fun newInstance(session: ClassSession): ClassScheduleDialog {
            return ClassScheduleDialog().apply {
                arguments = Bundle().apply {
                    putString("sessionId", session.sessionId)
                    putString("roomId", session.roomId)
                    putString("subject", session.subject)
                    putString("startTime", session.startTime)
                    putString("endTime", session.endTime)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            setDimAmount(0.6f)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
