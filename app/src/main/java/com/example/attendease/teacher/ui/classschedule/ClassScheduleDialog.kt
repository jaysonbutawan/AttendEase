package com.example.attendease.teacher.ui.classschedule

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.TimePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.example.attendease.databinding.ClassSchduleScreenBinding
import com.example.attendease.teacher.data.model.ClassSession
import com.example.attendease.teacher.data.model.Room
import com.example.attendease.teacher.data.repositories.SessionRepository
import com.example.attendease.teacher.ui.classschedule.viewModel.RoomListViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
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

        // Initialize ViewModel
        viewModel = ViewModelProvider(requireActivity())[RoomListViewModel::class.java]

        // Observe room list
        viewModel.rooms.observe(viewLifecycleOwner) { roomList ->
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, roomList)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerRoom.adapter = adapter

            // 🟢 Preselect the old room if editing an existing session
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
                Log.d("ClassScheduleDialog", "$label time picked: $formatted")
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
            // 🆕 CREATE NEW SESSION
            createNewSession(subject, startTime, endTime, newRoomId)
        } else {
            // 🟢 UPDATE EXISTING SESSION
            if (oldRoomId == newRoomId) {
                updateExistingSession(oldRoomId!!, sessionId, subject, startTime, endTime)
            } else {
                confirmRoomChange(oldRoomId!!, newRoomId, sessionId, subject, startTime, endTime)
            }
        }
    }

    private fun createNewSession(subject: String, startTime: String, endTime: String, roomId: String) {
        val dbRef = FirebaseDatabase.getInstance().getReference("rooms")

        dbRef.get().addOnSuccessListener { roomsSnapshot ->
            val newStart = parseTimeToMinutes(startTime)
            val newEnd = parseTimeToMinutes(endTime)

            var hasConflict = false

            roomsSnapshot.children.forEach { roomSnap ->
                roomSnap.child("sessions").children.forEach { sessionSnap ->
                    val existingSubject = sessionSnap.child("subject").getValue(String::class.java) ?: ""
                    val existingStart = parseTimeToMinutes(sessionSnap.child("startTime").getValue(String::class.java) ?: "")
                    val existingEnd = parseTimeToMinutes(sessionSnap.child("endTime").getValue(String::class.java) ?: "")

                    val overlaps = (newStart < existingEnd && newEnd > existingStart)

                    if (existingSubject.equals(subject, ignoreCase = true) && overlaps) {
                        hasConflict = true
                        return@forEach
                    }
                }
                if (hasConflict) return@forEach
            }

            if (hasConflict) {
                Toast.makeText(
                    requireContext(),
                    "❌ This subject overlaps with an existing session in another room.",
                    Toast.LENGTH_LONG
                ).show()
                return@addOnSuccessListener
            }

            // ✅ No conflict → proceed to create
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
                    Toast.makeText(requireContext(), "✅ New session created.", Toast.LENGTH_SHORT).show()
                    Log.d("ClassScheduleDialog", "✅ Created session ID: $sessionKey")
                    dismiss()
                } else {
                    Log.e("ClassScheduleDialog", "❌ Failed to create new session")
                }
            }

        }.addOnFailureListener {
            Log.e("ClassScheduleDialog", "❌ Failed to fetch rooms: ${it.message}")
        }
    }





    private fun parseTimeToMinutes(time: String): Int {
        return try {
            val format = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
            val date = format.parse(time)
            val cal = Calendar.getInstance()
            cal.time = date!!  // ✅ Correct way to set the time
            cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        } catch (e: Exception) {
            Log.e("ClassScheduleDialog", "❌ Failed to parse time: $time, ${e.message}")
            0
        }
    }




    private fun updateExistingSession(roomId: String, sessionId: String, subject: String, start: String, end: String) {
        val ref = FirebaseDatabase.getInstance()
            .getReference("rooms")
            .child(roomId)
            .child("sessions")
            .child(sessionId)

        val updates = mapOf("subject" to subject, "startTime" to start, "endTime" to end)

        ref.updateChildren(updates).addOnSuccessListener {
            Toast.makeText(requireContext(), "Session updated successfully.", Toast.LENGTH_SHORT).show()
            dismiss()
        }.addOnFailureListener {
            Log.e("ClassScheduleDialog", "❌ Failed to update session: ${it.message}")
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
                Log.e("ClassScheduleDialog", "⚠️ Old session does not exist!")
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
                        Toast.makeText(requireContext(), "Session moved successfully.", Toast.LENGTH_SHORT).show()
                        dismiss()
                    }
                }
            }.addOnFailureListener {
                Log.e("ClassScheduleDialog", "❌ Failed to migrate session: ${it.message}")
            }
        }.addOnFailureListener {
            Log.e("ClassScheduleDialog", "❌ Failed to get old session: ${it.message}")
        }
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
