package com.example.attendease.student.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.attendease.databinding.FragmentAttendanceActivityBinding
import com.example.attendease.student.adapter.AttendanceStatusAdapter
import com.example.attendease.student.viewmodel.AttendanceStatusListViewModel

class AttendanceFragmentActivity : Fragment() {

    private var _binding: FragmentAttendanceActivityBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: AttendanceStatusAdapter
    private val viewModel: AttendanceStatusListViewModel by viewModels()

    private var roomId: String? = null
    private var sessionId: String? = null
    private var selectedSubject: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Get both roomId and sessionId passed from previous screen
        roomId = arguments?.getString("roomId")
        sessionId = arguments?.getString("sessionId")
        Log.d("Attendance","get the room $roomId")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAttendanceActivityBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()

        // ✅ Fetch attendance only if both IDs are provided
        if (!roomId.isNullOrEmpty() && !sessionId.isNullOrEmpty()) {
            viewModel.fetchAttendanceForSession(roomId!!, sessionId!!)
        } else {
            binding.textEmptyState.text = "Missing room or session information."
            binding.textEmptyState.visibility = View.VISIBLE
        }
    }

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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
