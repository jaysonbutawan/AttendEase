package com.example.attendease.teacher.ui.adapter

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
    private val onMarkPresentClick: ((AttendanceRecord) -> Unit)? = null
) : RecyclerView.Adapter<AttendanceAdapter.AttendanceViewHolder>() {
    private var filteredList: List<AttendanceRecord> = attendanceList
    var onEmptyStateChange: ((Boolean) -> Unit)? = null

    inner class AttendanceViewHolder(val binding: AttendanceCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(record: AttendanceRecord) = with(binding) {

            tvStudentName.text = record.name ?: "Unknown Student"
            tvStatusText.text = record.timeScanned?.let { "Scanned at $it" } ?: "No scan record"

            tvStatusBadge.text = record.status?.capitalize() ?: "Unknown"
            val badgeColor = when (record.status?.lowercase()) {
                "present" -> R.color.success_color
                "partial" -> R.color.dark_background
                "absent" -> R.color.red
                else -> R.color.yellow
            }
            tvStatusBadge.setBackgroundColor(ContextCompat.getColor(root.context, badgeColor))

            val iconColor = when (record.status?.lowercase()) {
                "present" -> R.color.success_color
                "partial" -> R.color.dark_background
                "absent" -> R.color.red
                else -> R.color.yellow
            }
            ivStatusIcon.setColorFilter(ContextCompat.getColor(root.context, iconColor))

            if (
                record.status.equals("partial", true) ||
                record.status.equals("present", true) ||
                record.status.equals("late", true)
            ) {

                btnConfirmPresent.apply {
                    visibility = View.VISIBLE
                    text = "Confirm Present"
                    setBackgroundColor(Color.RED)
                    setOnClickListener { onMarkPresentClick?.invoke(record) }
                }

                when {
                    record.confidence?.contains("Low GPS", ignoreCase = true) == true -> {
                        tvStatusText.text = "Partial — Low GPS accuracy detected"
                        tvStatusText.setTextColor(Color.parseColor("#F4A261"))
                    }

                    record.confidence?.contains("Left geofence", ignoreCase = true) == true -> {
                        val outsideInfo = record.outsideTimeDisplay
                            ?: "${record.totalOutsideTime ?: 0} min outside"
                        tvStatusText.text = "Partial — Left geofence area ($outsideInfo)"
                        tvStatusText.setTextColor(Color.parseColor("#E76F51"))
                    }

                    else -> {
                        val outsideInfo = record.outsideTimeDisplay
                            ?: "${record.totalOutsideTime ?: 0} min outside"
                        tvStatusText.text = "Partial — Left geofence area ($outsideInfo)"
                        tvStatusText.setTextColor(Color.GRAY)
                    }
                }

            } else {
                btnConfirmPresent.visibility = View.GONE
            }
            record.confidence?.let {
                tvStatusText.setTextColor(
                    when {
                        it.contains("QR", ignoreCase = true) ->
                            ContextCompat.getColor(root.context, R.color.green_badge)

                        it.contains("Medium", ignoreCase = true) ->
                            ContextCompat.getColor(root.context, R.color.dark_background)

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
        holder.bind(filteredList[position])
    }

    override fun getItemCount(): Int = filteredList.size
    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newList: List<AttendanceRecord>) {
        attendanceList = newList
        filteredList = newList
        notifyDataSetChanged()
        onEmptyStateChange?.invoke(filteredList.isEmpty())
    }

    @SuppressLint("NotifyDataSetChanged")
    fun filter(query: String) {
        filteredList = if (query.isBlank()) {
            attendanceList
        } else {
            attendanceList.filter { record ->
                record.name?.contains(query, ignoreCase = true) == true ||
                        record.status?.contains(query, ignoreCase = true) == true ||
                        record.timeScanned?.contains(query, ignoreCase = true) == true
            }
        }
        notifyDataSetChanged()
        onEmptyStateChange?.invoke(filteredList.isEmpty())
    }

}
