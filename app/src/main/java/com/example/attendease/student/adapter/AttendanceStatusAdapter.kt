package com.example.attendease.student.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.attendease.R
import com.example.attendease.databinding.StudentAttendanceStatusCardBinding
import com.example.attendease.student.data.AttendanceStatus

class AttendanceStatusAdapter(
    private var attendanceList: List<AttendanceStatus>
) : RecyclerView.Adapter<AttendanceStatusAdapter.AttendanceViewHolder>() {
    private var filteredList: List<AttendanceStatus> = attendanceList
    var onEmptyStateChange: ((Boolean) -> Unit)? = null
    inner class AttendanceViewHolder(private val binding: StudentAttendanceStatusCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(status: AttendanceStatus) {
            binding.tvStatusText.text = status.timeText
            binding.tvStatusBadge.text = status.statusText

            val colorRes = when (status.statusText?.lowercase()) {
                "present" -> R.color.green_badge
                "absent" -> R.color.red
                "late" -> R.color.yellow
                "partial" -> R.color.dark_background
                else -> android.R.color.darker_gray
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
        holder.bind(filteredList[position])     }

    override fun getItemCount(): Int = filteredList.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newList: List<AttendanceStatus>) {
        attendanceList = newList
        filteredList = newList
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun filter(query: String) {
        filteredList = if (query.isBlank()) {
            attendanceList
        } else {
            attendanceList.filter { item ->
                item.statusText?.contains(query, ignoreCase = true) == true ||
                        item.timeText?.contains(query, ignoreCase = true) == true
            }
        }

        notifyDataSetChanged()
        onEmptyStateChange?.invoke(filteredList.isEmpty())
    }
}
