package com.example.attendease.ui.session

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.attendease.data.model.ClassSession
import com.example.attendease.databinding.SessionItemCardBinding
class SessionAdapter(
    private val sessionList: List<ClassSession>,
    private val onStartClassClick: (ClassSession) -> Unit) : RecyclerView.Adapter<SessionAdapter.SessionViewHolder>() {

    inner class SessionViewHolder(val binding: SessionItemCardBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val binding = SessionItemCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SessionViewHolder(binding)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        val session = sessionList[position]
        with(holder.binding) {
            tvSubjectName.text = session.subject
            tvRoomCode.text = session.roomName?: session.roomId
            tvTime.text = session.startTime + " - " + session.endTime
            tvDate.text = session.date

            startClassButton.setOnClickListener {
                Log.d("Session", "Start button clicked for session: ${session.sessionId}")
                onStartClassClick(session)}
        }
    }
    override fun getItemCount(): Int = sessionList.size
}
