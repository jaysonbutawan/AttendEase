package com.example.attendease.student.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.attendease.databinding.FragmentHistoryScreenBinding
import com.example.attendease.student.adapter.SubjectAdapter
import com.example.attendease.student.data.Session
import com.example.attendease.student.viewmodel.SubjectListViewModel

class HistoryFragmentActivity : Fragment() {

    private var _binding: FragmentHistoryScreenBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: SubjectAdapter
    private val viewModel: SubjectListViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()

        viewModel.fetchMatchedSubjects()
        binding.editSearchSubject.addTextChangedListener { editable ->
            val query = editable?.toString()?.trim() ?: ""
            adapter.filter(query)
        }

        adapter.onEmptyStateChange = { isEmpty ->
            binding.textEmptyState.isVisible = isEmpty
        }
    }

    private fun setupRecyclerView() {
        adapter = SubjectAdapter(emptyList()) { selectedSubject ->
            onSubjectClicked(selectedSubject)
        }

        binding.recyclerViewSubjects.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewSubjects.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.isVisible = loading
        }

        viewModel.subjects.observe(viewLifecycleOwner) { subjectList ->
            if (subjectList.isEmpty()) {
                binding.textEmptyState.isVisible = true
                binding.recyclerViewSubjects.isVisible = false
            } else {
                binding.textEmptyState.isVisible = false
                binding.recyclerViewSubjects.isVisible = true
                adapter = SubjectAdapter(subjectList) { onSubjectClicked(it) }
                binding.recyclerViewSubjects.adapter = adapter
                adapter.updateData(subjectList)
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            if (!error.isNullOrEmpty()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun onSubjectClicked(session: Session) {
        val attendanceFragment = AttendanceStatusBottomSheet().apply {
            arguments = Bundle().apply {
                putString("roomId", session.roomId)
                putString("sessionId", session.sessionId)
                putString("subject",session.subject)
                Log.d("History", "passs the ${session.room}")
                Log.d("History","passs the ${session.sessionId}")
                Log.d("History","pass the ${session.subject}")

            }
        }

        parentFragmentManager.beginTransaction()
            .replace(id, attendanceFragment)
            .addToBackStack(null)
            .commit()
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
