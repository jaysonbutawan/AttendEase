package com.example.attendease.student.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.attendease.databinding.HistorySubjectItemCardBinding
import com.example.attendease.student.data.Session

class SubjectAdapter (
    private val subjectList: List<Session>,
    private val onSubjectClick: (Session) -> Unit
    ) : RecyclerView.Adapter<SubjectAdapter.SubjectViewHolder>() {

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
            holder.bind(subjectList[position])
        }

        override fun getItemCount(): Int = subjectList.size
    }
