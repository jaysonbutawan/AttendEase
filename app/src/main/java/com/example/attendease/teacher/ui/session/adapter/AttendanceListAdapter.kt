package com.example.attendease.teacher.ui.session.adapter


import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.attendease.databinding.AttendanceListItemCardBinding
import com.example.attendease.teacher.data.model.AttendanceRecord

class AttendanceListAdapter(
    private var students: List<AttendanceRecord>
) : RecyclerView.Adapter<AttendanceListAdapter.StudentViewHolder>() {

    inner class StudentViewHolder(private val binding: AttendanceListItemCardBinding)
        : RecyclerView.ViewHolder(binding.root) {

        fun bind(record: AttendanceRecord) {
            binding.textStudentName.text = record.name
            binding.chipAttendanceStatus.text = "${record.status}"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        val binding = AttendanceListItemCardBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return StudentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        holder.bind(students[position])
    }

    override fun getItemCount() = students.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newList: List<AttendanceRecord>) {
        students = newList
        notifyDataSetChanged()
    }

}
