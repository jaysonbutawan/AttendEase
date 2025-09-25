package com.example.attendease.ui.session

import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.example.attendease.databinding.ClassSchduleScreenBinding
import java.util.Calendar

class ClassScheduleDialog : DialogFragment() {

    private var _binding: ClassSchduleScreenBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ClassSchduleScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.startTimePicker.setOnClickListener {
            val cal = Calendar.getInstance()
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val minute = cal.get(Calendar.MINUTE)

            TimePickerDialog(
                requireContext(),
                { _, selectedHour, selectedMinute ->
                    val amPm = if (selectedHour < 12) "AM" else "PM"
                    val formattedHour = if (selectedHour % 12 == 0) 12 else selectedHour % 12
                    binding.startTimePicker.setText(
                        String.format("%02d:%02d %s", formattedHour, selectedMinute, amPm)
                    )
                },
                hour,
                minute,
                false
            ).show()
        }

        binding.endTimePicker.setOnClickListener {
            val cal = Calendar.getInstance()
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val minute = cal.get(Calendar.MINUTE)

            TimePickerDialog(
                requireContext(),
                { _, selectedHour, selectedMinute ->
                    val amPm = if (selectedHour < 12) "AM" else "PM"
                    val formattedHour = if (selectedHour % 12 == 0) 12 else selectedHour % 12
                    binding.endTimePicker.setText(
                        String.format("%02d:%02d %s", formattedHour, selectedMinute, amPm)
                    )
                },
                hour,
                minute,
                false
            ).show()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
