package com.example.attendease.student.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.attendease.R
import com.example.attendease.databinding.StudentAttendanceStatusCardBinding
import com.example.attendease.student.data.AttendanceStatus

class AttendanceStatusAdapter(
    private var attendanceList: List<AttendanceStatus>
) : RecyclerView.Adapter<AttendanceStatusAdapter.AttendanceViewHolder>() {

    inner class AttendanceViewHolder(private val binding: StudentAttendanceStatusCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(status: AttendanceStatus) {
            binding.tvStatusText.text = status.timeText
            binding.tvStatusBadge.text = status.statusText

            val colorRes = when (status.statusText?.lowercase()) {
                "present" -> R.color.green_badge  // Green
                "absent" -> R.color.red    // Red
                "late" -> R.color.yellow     // Yellow/Orange
                "partial" -> R.color.dark_background // Dark Orange
                else -> android.R.color.darker_gray             // Default gray
            }

            binding.tvStatusBadge.setBackgroundColor(
                binding.tvStatusBadge.context.getColor(colorRes)
            )
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttendanceViewHolder {
        val binding = StudentAttendanceStatusCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return AttendanceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AttendanceViewHolder, position: Int) {
        holder.bind(attendanceList[position])
    }

    override fun getItemCount(): Int = attendanceList.size

    fun updateData(newList: List<AttendanceStatus>) {
        attendanceList = newList
        notifyDataSetChanged()
    }
}
