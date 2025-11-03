package com.example.attendease.teacher.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.attendease.databinding.ClassHistoryItemCardBinding
import com.example.attendease.teacher.data.model.ClassSession
import java.text.SimpleDateFormat
import java.util.Locale

class ClassHistoryAdapter(
    private var classHistoryList: List<ClassSession>,
    private val onItemClick: (ClassSession) -> Unit
) : RecyclerView.Adapter<ClassHistoryAdapter.ClassHistoryViewHolder>() {
    private var filteredList: List<ClassSession> = classHistoryList
    var onEmptyStateChange: ((Boolean) -> Unit)? = null

    inner class ClassHistoryViewHolder(val binding: ClassHistoryItemCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(session: ClassSession) {
            with(binding) {

                val formattedDate = formatDate(session.date)

                val time = "${session.startTime ?: "N/A"} - ${session.endTime ?: "N/A"}"
                textSubjectDetails.text = "$formattedDate |$time"

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
                dateString
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
        holder.bind(filteredList[position])
    }

    override fun getItemCount(): Int = filteredList.size


    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newList: List<ClassSession>) {
        classHistoryList = newList
        filteredList = newList
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun filter(query: String) {
        filteredList = if (query.isBlank()) {
            classHistoryList
        } else {
            classHistoryList.filter { session ->
                val rawDate = session.date ?: ""
                val formattedDate = try {
                    val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val outputFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
                    val parsedDate = inputFormat.parse(rawDate)
                    parsedDate?.let { outputFormat.format(it) } ?: rawDate
                } catch (e: Exception) {
                    rawDate
                }

                val subjectMatch = session.subject?.contains(query, ignoreCase = true) == true
                val dateMatch =
                    rawDate.contains(query, ignoreCase = true) || formattedDate.contains(
                        query,
                        ignoreCase = true
                    )
                val startTimeMatch = session.startTime?.contains(query, ignoreCase = true) == true
                val endTimeMatch = session.endTime?.contains(query, ignoreCase = true) == true

                subjectMatch || dateMatch || startTimeMatch || endTimeMatch
            }
        }

        notifyDataSetChanged()
        onEmptyStateChange?.invoke(filteredList.isEmpty())
    }
}
