package com.example.attendease.teacher.ui.activity

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.attendease.R
import com.example.attendease.databinding.AttendanceBottomSheetBinding
import com.example.attendease.teacher.data.model.AttendanceRecord
import com.example.attendease.teacher.ui.adapter.AttendanceAdapter
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AttendanceListBottomSheet : BottomSheetDialogFragment() {

    private var _binding: AttendanceBottomSheetBinding? = null
    private val binding get() = _binding!!

    private var statusType: String? = null
    private var attendanceList: ArrayList<AttendanceRecord> = arrayListOf()

    override fun getTheme(): Int = R.style.AppTheme_BottomSheetDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            statusType = it.getString("statusType")
            @Suppress("UNCHECKED_CAST")
            attendanceList = it.getSerializable("attendanceList") as ArrayList<AttendanceRecord>
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            val bottomSheet =
                bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = AttendanceBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val colorResId = when (statusType?.lowercase()) {
            "present" -> R.color.success_color
            "absent" -> R.color.red
            "partial" -> R.color.dark_background
            else -> R.color.black
        }
        binding.tvStatusTitle.text = "$statusType Students"
        binding.tvStatusTitle.setTextColor(
            ContextCompat.getColor(binding.tvStatusTitle.context, colorResId)
        )

        val filtered = attendanceList.filter {
            when (statusType?.lowercase()) {
                "present" -> it.status.equals("present", true) || it.status.equals("late", true)
                else -> it.status.equals(statusType, true)
            }
        }

        val adapter = AttendanceAdapter(filtered)
        binding.rvAttendanceList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAttendanceList.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(statusType: String, attendanceList: ArrayList<AttendanceRecord>): AttendanceListBottomSheet {
            val fragment = AttendanceListBottomSheet()
            val args = Bundle().apply {
                putString("statusType", statusType)
                putSerializable("attendanceList", attendanceList)
            }
            fragment.arguments = args
            return fragment
        }
    }
}