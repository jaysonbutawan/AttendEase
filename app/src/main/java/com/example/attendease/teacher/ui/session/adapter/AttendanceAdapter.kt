package com.example.attendease.teacher.ui.session.adapter

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.attendease.R
import com.example.attendease.databinding.AttendanceCardBinding
import com.example.attendease.teacher.data.model.AttendanceRecord

class AttendanceAdapter(
    private var attendanceList: List<AttendanceRecord>
) : RecyclerView.Adapter<AttendanceAdapter.AttendanceViewHolder>() {

    inner class AttendanceViewHolder(val binding: AttendanceCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(record: AttendanceRecord) = with(binding) {
            // ✅ Student name
            tvStudentName.text = record.name ?: "Unknown Student"

            // ✅ Status text
            tvStatusText.text = when (record.timeScanned) {
                null -> "No scan record"
                else -> "Checked in at ${record.timeScanned}"
            }

            // ✅ Badge (present / absent)
            tvStatusBadge.text = record.status?.capitalize() ?: "Unknown"
            val badgeColor = when (record.status?.lowercase()) {
                "present" -> R.color.success_color
                "late", "partial"-> R.color.dark_background
                "absent" -> R.color.red
                else -> R.color.secondary_text
            }
            tvStatusBadge.setBackgroundColor(
                ContextCompat.getColor(root.context, badgeColor)
            )


            // ✅ Status icon tint
            val iconColor = when (record.status?.lowercase()) {
                "present" -> R.color.success_color
                "late", "partial" -> R.color.dark_background
                "absent" -> R.color.red
                else -> R.color.secondary_text
            }
            ivStatusIcon.setColorFilter(ContextCompat.getColor(root.context, iconColor))

            // ✅ Optional: Use confidence text color
            record.confidence?.let {
                if (it.contains("QR", ignoreCase = true)) {
                    tvStatusText.setTextColor(ContextCompat.getColor(root.context, R.color.green_badge))
                } else if (it.contains("Medium", ignoreCase = true)) {
                    tvStatusText.setTextColor(ContextCompat.getColor(root.context, R.color.dark_background))
                } else {
                    tvStatusText.setTextColor(Color.GRAY)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttendanceViewHolder {
        val binding = AttendanceCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AttendanceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AttendanceViewHolder, position: Int) {
        holder.bind(attendanceList[position])
    }

    override fun getItemCount(): Int = attendanceList.size
    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newList: List<AttendanceRecord>) {
        attendanceList = newList
        notifyDataSetChanged()
    }
}
