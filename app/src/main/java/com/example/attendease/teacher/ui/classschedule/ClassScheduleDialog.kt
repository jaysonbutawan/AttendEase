package com.example.attendease.teacher.ui.classschedule

//noinspection SuspiciousImport
import android.R
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.example.attendease.teacher.data.model.ClassSession
import com.example.attendease.teacher.data.model.Room
import com.example.attendease.teacher.data.repositories.SessionRepository
import com.example.attendease.databinding.ClassSchduleScreenBinding
import com.example.attendease.teacher.ui.classschedule.viewModel.RoomListViewModel
import com.google.firebase.auth.FirebaseAuth
import java.util.Calendar
import androidx.core.graphics.drawable.toDrawable
import com.example.attendease.teacher.data.model.QrUtils


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
        viewModel = ViewModelProvider(requireActivity())[RoomListViewModel::class.java]

        viewModel.rooms.observe(viewLifecycleOwner) { roomList ->
            val adapter = ArrayAdapter(
                requireContext(),
                R.layout.simple_spinner_item,
                roomList
            )
            adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
            binding.spinnerRoom.adapter = adapter
        }

        viewModel.loadRooms()

        // Start time picker
        binding.startTimePicker.setOnClickListener {
            val cal = Calendar.getInstance()
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val minute = cal.get(Calendar.MINUTE)

            TimePickerDialog(
                requireContext(),
                { _, selectedHour, selectedMinute ->
                    val amPm = if (selectedHour < 12) "AM" else "PM"
                    val formattedHour = if (selectedHour % 12 == 0) 12 else selectedHour % 12
                    val formatted = String.format("%02d:%02d %s", formattedHour, selectedMinute, amPm)
                    binding.startTimePicker.setText(formatted)
                    Log.d("ClassScheduleDialog", "Start time picked: $formatted")
                },
                hour,
                minute,
                false
            ).show()
        }

        // End time picker
        binding.endTimePicker.setOnClickListener {
            val cal = Calendar.getInstance()
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val minute = cal.get(Calendar.MINUTE)

            TimePickerDialog(
                requireContext(),
                { _, selectedHour, selectedMinute ->
                    val amPm = if (selectedHour < 12) "AM" else "PM"
                    val formattedHour = if (selectedHour % 12 == 0) 12 else selectedHour % 12
                    val formatted = String.format("%02d:%02d %s", formattedHour, selectedMinute, amPm)
                    binding.endTimePicker.setText(formatted)
                    Log.d("ClassScheduleDialog", "End time picked: $formatted")
                },
                hour,
                minute,
                false
            ).show()
        }


        binding.btnSchedule.setOnClickListener {
            val selectedRoom = binding.spinnerRoom.selectedItem as? Room
            if (selectedRoom != null) {

                val session = ClassSession(
                    roomId = selectedRoom.roomId ?: "",
                    subject = binding.editTextSubject.text.toString(),
                    date = "",
                    startTime = binding.startTimePicker.text.toString(),
                    endTime = binding.endTimePicker.text.toString(),
                    allowanceTime = 10,
                    teacherId = FirebaseAuth.getInstance().currentUser?.uid ?: "",
                    qrCode = ""
                )
                repo.createSession(session) { success, sessionId ->
                    if (success) dismiss()
                    Log.d("ClassScheduleDialog", "Session created with ID: $selectedRoom")
                }
            } else {
                Log.e("ClassScheduleDialog", "No room selected!")
            }
        }


    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        dialog?.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        dialog?.window?.setDimAmount(0.6f)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
