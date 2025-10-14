package com.example.attendease.student.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
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
        with(holder.binding) {
            classTitle.text = session.subject
            instructorName.text = session.instructor
            statusBadge.text = session.status
            tvTime.text = "${session.startTime} - ${session.endTime}"
            tvRoom.text = session.room
            reminderButton.setOnClickListener {
                Toast.makeText(it.context, "Reminder set for ${session.subject}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun getItemCount() = sessions.size
}
