package com.example.attendease.teacher.ui.session.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.attendease.databinding.HistorySubjectItemCardBinding
import com.example.attendease.teacher.data.model.ClassSession

class HistorySubjectAdapter(
    private val subjectList: List<ClassSession>,
    private val onSubjectClick: (ClassSession) -> Unit
) : RecyclerView.Adapter<HistorySubjectAdapter.SubjectViewHolder>() {

    inner class SubjectViewHolder(private val binding: HistorySubjectItemCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(subject: ClassSession) {
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
