package com.example.attendease.student.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.attendease.databinding.HistorySubjectItemCardBinding
import com.example.attendease.student.data.Session

class SubjectAdapter (
    private var subjectList: List<Session>,
    private val onSubjectClick: (Session) -> Unit
    ) : RecyclerView.Adapter<SubjectAdapter.SubjectViewHolder>() {
    private var filteredList: List<Session> = subjectList
    var onEmptyStateChange: ((Boolean) -> Unit)? = null

        inner class SubjectViewHolder(private val binding: HistorySubjectItemCardBinding) :
            RecyclerView.ViewHolder(binding.root) {

            fun bind(subject: Session) {
                with(binding) {
                    textSubjectTitle.text = subject.subject
                    textActionDetails.text = "View Attendance Reports"

                    root.setOnClickListener {
                        onSubjectClick(subject)
                    }
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubjectViewHolder {
            val binding = HistorySubjectItemCardBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return SubjectViewHolder(binding)
        }

        override fun onBindViewHolder(holder: SubjectViewHolder, position: Int) {
            holder.bind(filteredList[position]) // ✅ use filtered list
        }

    override fun getItemCount(): Int = filteredList.size // ✅ use filtered list

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newList: List<Session>) {
        subjectList = newList
        filteredList = newList
        notifyDataSetChanged()
        onEmptyStateChange?.invoke(filteredList.isEmpty())
    }
    @SuppressLint("NotifyDataSetChanged")
    fun filter(query: String) {
        filteredList = if (query.isBlank()) {
            subjectList
        } else {
            subjectList.filter { session ->
                session.subject.contains(query, ignoreCase = true) ||
                        session.roomId.contains(query, ignoreCase = true) ||
                        session.sessionId.contains(query, ignoreCase = true)
            }
        }

        notifyDataSetChanged()
        onEmptyStateChange?.invoke(filteredList.isEmpty())
    }
    }
