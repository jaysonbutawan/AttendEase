package com.example.attendease.teacher.ui.session.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.attendease.teacher.data.model.ClassSession
import com.example.attendease.databinding.SessionItemCardBinding

class SessionAdapter(
    private val sessionList: List<ClassSession>,
    private val onStartClassClick: (ClassSession) -> Unit,
    private val onEditClick: (ClassSession) -> Unit,
    private val onDeleteClick: (ClassSession) -> Unit
) : RecyclerView.Adapter<SessionAdapter.SessionViewHolder>() {

    inner class SessionViewHolder(val binding: SessionItemCardBinding) :
        RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(session: ClassSession) {
            with(binding) {
                // Existing bindings
                tvSubjectName.text = session.subject
                tvRoomCode.text = session.roomName ?: session.roomId
                tvTime.text = "${session.startTime} - ${session.endTime}"

                //  Set button text based on session status
                if (session.sessionStatus == "started") {
                    startClassButton.text = "Started"
                    startClassButton.isEnabled = true
                    startClassButton.alpha = 0.8f
                } else {
                    startClassButton.text = "Start Class"
                    startClassButton.isEnabled = true
                    startClassButton.alpha = 1.0f
                }

                // Start Class button click
                startClassButton.setOnClickListener {
                    onStartClassClick(session)
                }

                // Edit and delete buttons
                optionEdit.setOnClickListener { onEditClick(session) }
                optionDelete.setOnClickListener { onDeleteClick(session) }

                // Long-click to toggle options menu
                sessionMainCard.setOnLongClickListener {
                    sessionOptionsPopup.visibility =
                        if (sessionOptionsPopup.visibility == View.GONE) View.VISIBLE else View.GONE
                    true
                }

                // Short-click to hide menu if visible
                sessionMainCard.setOnClickListener {
                    if (sessionOptionsPopup.visibility == View.VISIBLE) {
                        sessionOptionsPopup.visibility = View.GONE
                    }
                }
            }
        }

    }

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
        holder.bind(sessionList[position])
    }

    override fun getItemCount(): Int = sessionList.size
}
