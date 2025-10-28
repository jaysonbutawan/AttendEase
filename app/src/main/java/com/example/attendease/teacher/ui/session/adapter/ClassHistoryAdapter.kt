package com.example.attendease.teacher.ui.session.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.attendease.databinding.ClassHistoryItemCardBinding
import com.example.attendease.teacher.data.model.ClassSession
import java.text.SimpleDateFormat
import java.util.Locale

class ClassHistoryAdapter(
    private val classHistoryList: List<ClassSession>,
    private val onItemClick: (ClassSession) -> Unit
) : RecyclerView.Adapter<ClassHistoryAdapter.ClassHistoryViewHolder>() {

    inner class ClassHistoryViewHolder(val binding: ClassHistoryItemCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(session: ClassSession) {
            with(binding) {

                val formattedDate = formatDate(session.date)

                val time = "${session.startTime ?: "N/A"} - ${session.endTime ?: "N/A"}"
                textSubjectDetails.text = "$time | $formattedDate"

                root.setOnClickListener {
                    onItemClick(session)
                }
            }
        }

        private fun formatDate(dateString: String?): String {
            if (dateString.isNullOrBlank()) return "No Date"
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val outputFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
                val parsedDate = inputFormat.parse(dateString)
                parsedDate?.let { outputFormat.format(it) } ?: dateString
            } catch (e: Exception) {
                dateString // fallback if format fails
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClassHistoryViewHolder {
        val binding = ClassHistoryItemCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ClassHistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ClassHistoryViewHolder, position: Int) {
        holder.bind(classHistoryList[position])
    }

    override fun getItemCount(): Int = classHistoryList.size
}
