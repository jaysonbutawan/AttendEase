package com.example.attendease.student.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
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
        val binding = holder.binding

        binding.classTitle.text = session.subject
        binding.instructorName.text = session.instructor
        binding.tvTime.text = "${session.startTime} - ${session.endTime}"
        binding.tvRoom.text = session.room

        if (session.status == "Live") {
            binding.statusBadge.text = "Live"
            binding.statusBadge.setBackgroundResource(R.drawable.teacher_rounded_button)
        } else {
            binding.statusBadge.text = "Upcoming"
            binding.statusBadge.setBackgroundResource(R.drawable.student_rounded_button)
        }
    }

    override fun getItemCount() = sessions.size
}
