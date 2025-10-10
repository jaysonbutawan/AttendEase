package com.example.attendease.student.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.attendease.R
import com.example.attendease.databinding.FragmentScheduleScreenBinding // Assuming this is the correct binding class

class ScheduleFragmentActivity : Fragment() {

    private var _binding: FragmentScheduleScreenBinding? = null
    private val binding get() = _binding!!
    private val FILE_PICKER_REQUEST_CODE = 101
    private var isCsvLoaded: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScheduleScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // All view manipulation and click listeners must be called here,
        // after the view has been created and binding is valid.

        // 1. Initial State Check
        isCsvLoaded = checkIfCsvDataExists()
        updateUploadUiState(isCsvLoaded)


        // 2. Set up Click Listeners
        // Click the entire drop area to open the file manager
        binding.dropArea.setOnClickListener {
            openFileManager()
        }

        // Click the Update button to open the file manager
        binding.updateCsvButton.setOnClickListener {
            openFileManager()
        }

        // Click the Remove button
        binding.removeCsvButton.setOnClickListener {
            removeCsvData()
        }
    }

    // --- UI/State Management Functions ---

    private fun updateUploadUiState(isLoaded: Boolean) {
        if (isLoaded) {
            // CSV is loaded: Show manage area, hide drop area
            binding.dropArea.visibility = View.GONE
            binding.manageArea.visibility = View.VISIBLE
            // Set file name (e.g., from saved state)
            binding.fileNameText.text = getSavedCsvFileName()

            // TODO: Also load the student's schedule data and display it
            showStudentSchedule(true)
        } else {
            // CSV is NOT loaded: Show drop area, hide manage area
            binding.dropArea.visibility = View.VISIBLE
            binding.manageArea.visibility = View.GONE

            // TODO: Hide/clear the student's schedule views
            showStudentSchedule(false)
        }
    }

    private fun showStudentSchedule(show: Boolean) {
        // Example: Hide/show the included card views based on CSV state
        val visibility = if (show) View.VISIBLE else View.GONE
        binding.todayLabel.visibility = visibility
        binding.dateLabel.visibility = visibility

        binding.upcomingClassCard.root.visibility = visibility
        binding.liveClassCard.root.visibility = visibility
    }

    // --- File Manager/Data Management Functions ---

    private fun openFileManager() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/csv" // Only show CSV files
        }
        // Use startActivityForResult for Fragments (deprecated in new APIs, but fits your current method signature)
        startActivityForResult(intent, FILE_PICKER_REQUEST_CODE)
    }

    private fun removeCsvData() {
        // TODO: 1. Delete or clear the saved schedule data (database/SharedPreferences)
        // TODO: 2. Clear the flag indicating a CSV is loaded

        isCsvLoaded = false // Update local flag
        updateUploadUiState(false) // Update UI
        // Optional: Show a Toast message
    }

    // --- Lifecycle and File Result ---

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILE_PICKER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                // Process the selected CSV file URI here
                val fileName = getFileName(uri)

                // TODO: 1. Parse the CSV file (using a library like opencsv or custom logic)
                // TODO: 2. Save the schedule data to your local storage (Room, SharedPreferences)

                // 3. Update state upon successful processing
                saveCsvFileName(fileName) // Save file name for display
                isCsvLoaded = true
                updateUploadUiState(true)

                // Optional: Show a success message
            }
        }
    }

    // Simple helper to get the display name of the selected file
    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        result = it.getString(nameIndex)
                    }
                }
            }
        }
        // Fallback for file name
        return result ?: uri.path?.substringAfterLast('/') ?: "unknown_file.csv"
    }

    // --- Placeholder Data Functions (Replace with actual data layer calls) ---
    private fun checkIfCsvDataExists(): Boolean {
        // Replace with logic to check if schedule data exists
        return false
    }

    private fun getSavedCsvFileName(): String {
        // Replace with logic to retrieve the saved file name
        return "schedule_data_2025.csv"
    }

    private fun saveCsvFileName(name: String) {
        // Replace with logic to save the file name
    }

    // Always clear the binding reference in onDestroyView to prevent memory leaks
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}