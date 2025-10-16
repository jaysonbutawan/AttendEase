package com.example.attendease.student.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.attendease.R
import com.example.attendease.databinding.UpcomingClassCardBinding
import com.example.attendease.student.data.Session

class SessionAdapter(
    private val sessions: List<Session>
) : RecyclerView.Adapter<SessionAdapter.SessionViewHolder>() {

    inner class SessionViewHolder(val binding: UpcomingClassCardBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val binding = UpcomingClassCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SessionViewHolder(binding)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        val session = sessions[position]
        val context: Context = holder.itemView.context
        val binding = holder.binding

        // Set session info
        binding.classTitle.text = session.subject
        binding.instructorName.text = session.instructor
        binding.tvTime.text = "${session.startTime} - ${session.endTime}"
        binding.tvRoom.text = session.room

        // Customize UI based on status
        if (session.status == "Live") {
            binding.statusBadge.text = "Live"
            binding.statusBadge.setBackgroundResource(R.drawable.teacher_rounded_button)
            binding.reminderButton.text = "Join Class Now"
            binding.reminderButton.setBackgroundColor(ContextCompat.getColor(context, R.color.red))
        } else {
            binding.statusBadge.text = "Upcoming"
            binding.statusBadge.setBackgroundResource(R.drawable.student_rounded_button)
            binding.reminderButton.text = "Set Reminder"
            binding.reminderButton.setBackgroundColor(ContextCompat.getColor(context, R.color.blue))
        }

        // Button click
        binding.reminderButton.setOnClickListener {
            if (session.status == "Live") {
                Toast.makeText(context, "Joining ${session.subject}...", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Reminder set for ${session.subject}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun getItemCount() = sessions.size
}
