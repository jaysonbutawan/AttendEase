package com.example.attendease.student.ui

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.attendease.databinding.FragmentAttendanceActivityBinding
import com.example.attendease.student.adapter.AttendanceStatusAdapter
import com.example.attendease.student.viewmodel.AttendanceStatusListViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AttendanceStatusBottomSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentAttendanceActivityBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: AttendanceStatusAdapter
    private val viewModel: AttendanceStatusListViewModel by viewModels()

    private var roomId: String? = null
    private var sessionId: String? = null
    private var subject: String? =null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        roomId = arguments?.getString("roomId")
        sessionId = arguments?.getString("sessionId")
        subject = arguments?.getString("subject")
        Log.d("Attendance","get the room $roomId")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAttendanceActivityBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()

        if (!roomId.isNullOrEmpty() && !sessionId.isNullOrEmpty()) {
            viewModel.fetchAttendanceForSession(roomId!!, sessionId!!)
        } else {
            binding.textEmptyState.text = "Missing room or session information."
            binding.textEmptyState.visibility = View.VISIBLE
        }
        adapter.onEmptyStateChange = { isEmpty ->
            binding.textEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        }

        binding.textHeader.text ="$subject"}


    private fun setupRecyclerView() {
        adapter = AttendanceStatusAdapter(emptyList())
        binding.recyclerViewSubjects.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewSubjects.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.attendanceList.observe(viewLifecycleOwner) { list ->
            adapter.updateData(list)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                binding.textEmptyState.text = error
                binding.textEmptyState.visibility = View.VISIBLE
            } else {
                binding.textEmptyState.visibility = View.GONE
            }
        }

        viewModel.emptyState.observe(viewLifecycleOwner) { isEmpty ->
            binding.textEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        }

        binding.editSearchSubject.addTextChangedListener { editable ->
            val query = editable?.toString()?.trim() ?: ""
            adapter.filter(query)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}