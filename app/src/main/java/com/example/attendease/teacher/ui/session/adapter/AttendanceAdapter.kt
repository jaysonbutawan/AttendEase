package com.example.attendease.teacher.ui.session.adapter

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.attendease.R
import com.example.attendease.databinding.AttendanceCardBinding
import com.example.attendease.teacher.data.model.AttendanceRecord

class AttendanceAdapter(
    private var attendanceList: List<AttendanceRecord>,
    private val onMarkPresentClick: ((AttendanceRecord) -> Unit)? = null // Make it optional
) : RecyclerView.Adapter<AttendanceAdapter.AttendanceViewHolder>() {

    inner class AttendanceViewHolder(val binding: AttendanceCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(record: AttendanceRecord) = with(binding) {
            tvStudentName.text = record.name ?: "Unknown Student"

            tvStatusText.text = record.timeScanned?.let { "Scanned at $it" } ?: "No scan record"

            tvStatusBadge.text = record.status?.capitalize() ?: "Unknown"
            val badgeColor = when (record.status?.lowercase()) {
                "present"  -> R.color.success_color
                "partial"-> R.color.dark_background
                "absent" -> R.color.red
                else -> R.color.yellow
            }
            tvStatusBadge.setBackgroundColor(ContextCompat.getColor(root.context, badgeColor))

            if (record.status?.lowercase() == "partial") {
                btnConfirmPresent.visibility = View.VISIBLE
                btnConfirmPresent.setOnClickListener {
                    onMarkPresentClick?.invoke(record)
                }
            } else {
                btnConfirmPresent.visibility = View.GONE
            }

            val iconColor = when (record.status?.lowercase()) {
                "present"-> R.color.success_color
                "partial" -> R.color.dark_background
                "absent" -> R.color.red
                else -> R.color.yellow

            }
            ivStatusIcon.setColorFilter(ContextCompat.getColor(root.context, iconColor))

            record.confidence?.let {
                tvStatusText.setTextColor(
                    when {
                        it.contains("QR", ignoreCase = true) -> ContextCompat.getColor(root.context, R.color.green_badge)
                        it.contains("Medium", ignoreCase = true) -> ContextCompat.getColor(root.context, R.color.dark_background)
                        else -> Color.GRAY
                    }
                )
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
