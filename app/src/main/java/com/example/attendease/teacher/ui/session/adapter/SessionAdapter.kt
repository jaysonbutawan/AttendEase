package com.example.attendease.teacher.ui.session.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View // <-- NEW IMPORT for View.GONE/View.VISIBLE
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
                // 1. Existing Data Binding
                tvSubjectName.text = session.subject
                tvRoomCode.text = session.roomName ?: session.roomId
                tvTime.text = "${session.startTime} - ${session.endTime}"

                // 2. Start Class Button Click (Existing Logic)
                startClassButton.setOnClickListener {
                    onStartClassClick(session)
                }
                optionEdit.setOnClickListener {
                    onEditClick(session)
                }
                optionDelete.setOnClickListener {
                    onDeleteClick(session)
                }

                // 3. Long Press Listener: Toggles visibility of the options menu
                // This will show/hide the pop-up card when the main card is long-pressed.
                sessionMainCard.setOnLongClickListener {
                    // Toggle visibility of the menu card
                    if (sessionOptionsPopup.visibility == View.GONE) {
                        sessionOptionsPopup.visibility = View.VISIBLE
                    } else {
                        sessionOptionsPopup.visibility = View.GONE
                    }
                    true // Consume the long-click event
                }

                // 4. Short Click Listener: If the menu is visible, a short click anywhere
                // on the main card will hide it.
                sessionMainCard.setOnClickListener {
                    if (sessionOptionsPopup.visibility == View.VISIBLE) {
                        sessionOptionsPopup.visibility = View.GONE
                    }
                    // Primary action remains the Start button.
                }

                // NOTE: The click listeners for optionEdit and optionDelete are omitted
                // since the adapter constructor no longer provides the required lambda functions.
                // You will need to add these back if you want those buttons to perform actions.
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
