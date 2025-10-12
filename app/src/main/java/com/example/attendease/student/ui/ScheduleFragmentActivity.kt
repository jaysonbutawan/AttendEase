package com.example.attendease.student.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.attendease.databinding.FragmentScheduleScreenBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.BufferedReader
import java.io.InputStreamReader

class ScheduleFragmentActivity : Fragment() {

    private var _binding: FragmentScheduleScreenBinding? = null
    private val binding get() = _binding!!
    private var isCsvLoaded = false

    private val database = FirebaseDatabase.getInstance().reference
    private val currentUser = FirebaseAuth.getInstance().currentUser
    private var uploadedCsvUri: Uri? = null

    private val TAG = "CSV_DEBUG"

    // Modern File Picker
    private val openCsvFileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            data?.data?.let { uri ->
                try {
                    requireContext().contentResolver.takePersistableUriPermission(
                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: SecurityException) {
                    Log.e(TAG, "URI permission error: ${e.message}", e)
                }

                uploadedCsvUri = uri
                val fileName = getFileName(uri)
                parseAndUploadCsv(uri, fileName)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScheduleScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ✅ Load previous cached CSV data (if available)
        checkCachedCsvData()

        binding.dropArea.setOnClickListener { openFileManager() }
        binding.updateCsvButton.setOnClickListener { openFileManager() }
        binding.removeCsvButton.setOnClickListener { removeCsvData() }
    }

    // File Picker
    private fun openFileManager() {
        val mimeTypes = arrayOf(
            "text/csv", "application/csv", "application/vnd.ms-excel",
            "text/comma-separated-values", "text/plain"
        )

        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }

        openCsvFileLauncher.launch(intent)
    }

    // ✅ Parse CSV and upload extracted columns to Firebase
    @SuppressLint("SetTextI18n")
    private fun parseAndUploadCsv(uri: Uri, fileName: String) {
        val userId = currentUser?.uid ?: return
        val userRef = database.child("users").child(userId).child("schedule")

        lifecycleScope.launch {
            try {
                binding.fileNameText.text = "Reading CSV..."
                val inputStream = requireContext().contentResolver.openInputStream(uri)
                val reader = BufferedReader(InputStreamReader(inputStream))
                val lines = reader.readLines()

                Log.d(TAG, "Total CSV Lines: ${lines.size}")
                if (lines.isEmpty()) {
                    binding.fileNameText.text = "Error: Empty CSV"
                    Log.e(TAG, "CSV file is empty")
                    return@launch
                }

                val scheduleList = mutableListOf<Map<String, String>>()

                for (i in 1 until lines.size) { // Skip header
                    val columns = lines[i].split(",")
                    Log.d(TAG, "Line $i -> ${columns.joinToString()}")

                    if (columns.size >= 4) {
                        val scheduleItem = mapOf(
                            "subject" to columns[0].trim(),
                            "room" to columns[1].trim(),
                            "time" to columns[2].trim(),
                            "instructor" to columns[3].trim()
                        )
                        scheduleList.add(scheduleItem)
                    } else {
                        Log.w(TAG, "Skipping invalid row at line $i: ${lines[i]}")
                    }
                }

                if (scheduleList.isEmpty()) {
                    binding.fileNameText.text = "Error: No valid rows found"
                    Log.e(TAG, "Parsed list is empty.")
                    return@launch
                }

                // Upload parsed data to Firebase
                userRef.setValue(scheduleList).await()
                Log.d(TAG, "Successfully uploaded ${scheduleList.size} rows to Firebase")

                // Cache CSV content locally for persistence
                cacheCsvData(fileName, scheduleList)

                // Update UI
                saveCsvFileName(fileName)
                isCsvLoaded = true
                updateUploadUiState(true)
                binding.fileNameText.text = "Uploaded: $fileName"

            } catch (e: Exception) {
                Log.e(TAG, "Error parsing/uploading CSV: ${e.message}", e)
                binding.fileNameText.text = "Error: ${e.message}"
            }
        }
    }

    // ✅ Local cache so user won’t lose CSV after logout/restart
    private fun cacheCsvData(fileName: String, data: List<Map<String, String>>) {
        val prefs = requireContext().getSharedPreferences("csv_cache", Activity.MODE_PRIVATE)
        val dataString = data.joinToString(";") {
            "${it["subject"]},${it["room"]},${it["time"]},${it["instructor"]}"
        }
        prefs.edit()
            .putString("csvFileName", fileName)
            .putString("csvData", dataString)
            .apply()
        Log.d(TAG, "Cached CSV data locally: $fileName")
    }

    // ✅ Check cache when opening app again
    private fun checkCachedCsvData() {
        val prefs = requireContext().getSharedPreferences("csv_cache", Activity.MODE_PRIVATE)
        val fileName = prefs.getString("csvFileName", null)
        val dataString = prefs.getString("csvData", null)

        if (fileName != null && dataString != null) {
            isCsvLoaded = true
            updateUploadUiState(true)
            binding.fileNameText.text = "Cached: $fileName"
            Log.d(TAG, "Restored cached CSV: $fileName")
        } else {
            Log.d(TAG, "No cached CSV found.")
            updateUploadUiState(false)
        }
    }

    private fun removeCsvData() {
        val userId = currentUser?.uid ?: return
        val userRef = database.child("users").child(userId)

        userRef.child("schedule").removeValue()
        userRef.child("csvFileName").removeValue()

        // Clear local cache too
        requireContext().getSharedPreferences("csv_cache", Activity.MODE_PRIVATE)
            .edit().clear().apply()

        isCsvLoaded = false
        updateUploadUiState(false)
        Log.d(TAG, "CSV data removed from Firebase and cache")
    }

    private fun updateUploadUiState(isLoaded: Boolean) {
        if (isLoaded) {
            binding.dropArea.visibility = View.GONE
            binding.manageArea.visibility = View.VISIBLE
            binding.fileNameText.text = getSavedCsvFileName()
            showStudentSchedule(true)
        } else {
            binding.dropArea.visibility = View.VISIBLE
            binding.manageArea.visibility = View.GONE
            showStudentSchedule(false)
        }
    }

    private fun showStudentSchedule(show: Boolean) {
        val visibility = if (show) View.VISIBLE else View.GONE
        binding.todayLabel.visibility = visibility
        binding.dateLabel.visibility = visibility
        binding.upcomingClassCard.root.visibility = visibility
        binding.liveClassCard.root.visibility = visibility
    }

    private fun getFileName(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) result = it.getString(nameIndex)
                }
            }
        }
        return result ?: uri.path?.substringAfterLast('/') ?: "unknown_file.csv"
    }

    private fun saveCsvFileName(name: String) {
        requireContext().getSharedPreferences("csv_prefs", Activity.MODE_PRIVATE)
            .edit().putString("csvFileName", name).apply()
    }

    private fun getSavedCsvFileName(): String {
        return requireContext().getSharedPreferences("csv_prefs", Activity.MODE_PRIVATE)
            .getString("csvFileName", "No CSV uploaded") ?: "No CSV uploaded"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
