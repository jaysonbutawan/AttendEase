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
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.attendease.databinding.FragmentScheduleScreenBinding
import com.example.attendease.student.adapter.SessionAdapter
import com.example.attendease.student.data.Session
import com.example.attendease.student.helper.SessionHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScheduleFragmentActivity : Fragment() {

    private var _binding: FragmentScheduleScreenBinding? = null
    private val binding get() = _binding!!

    private val database = FirebaseDatabase.getInstance().reference
    private val currentUser = FirebaseAuth.getInstance().currentUser

    private var isCsvLoaded = false
    private var uploadedCsvUri: Uri? = null

    private val TAG = "CSV_DEBUG"

    private val openCsvFileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                handleCsvUri(uri)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentScheduleScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkCachedCsvData()

        binding.dropArea.setOnClickListener { openFileManager() }
        binding.updateCsvButton.setOnClickListener { openFileManager() }
        binding.removeCsvButton.setOnClickListener { removeCsvData() }

        binding.swipeRefreshLayout.setOnRefreshListener {
            refreshFragmentData()
        }
        loadMatchedSessions()
        updateDateLabel()
    }
    private fun updateDateLabel() {


        val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
        val currentDate = Date()
        val dateString = dateFormat.format(currentDate)

        binding.dateLabel.text = dateString
    }

    private fun refreshFragmentData() {
        // Start the spinner immediately
        binding.swipeRefreshLayout.isRefreshing = true

        // Call the main function to reload all data in the fragment
        loadMatchedSessions()
    }

    // ------------------------------- //
    // CSV Upload and Parsing
    // ------------------------------- //

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

    private fun handleCsvUri(uri: Uri) {
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

    @SuppressLint("SetTextI18n")
    private fun parseAndUploadCsv(uri: Uri, fileName: String) {
        val user = currentUser ?: run {
            binding.fileNameText.text = "Error: Not logged in"
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val reader = BufferedReader(InputStreamReader(requireContext().contentResolver.openInputStream(uri)))
                val lines = reader.readLines().also { reader.close() }

                if (lines.isEmpty()) {
                    withContext(Dispatchers.Main) { binding.fileNameText.text = "Error: Empty CSV" }
                    return@launch
                }

                val scheduleList = lines.drop(1).mapNotNull { line ->
                    val columns = line.split(",")
                    if (columns.size >= 4) {
                        mapOf(
                            "subject" to columns[0].trim(),
                            "room" to columns[1].trim(),
                            "time" to columns[2].trim(),
                            "instructor" to columns[3].trim()
                        )
                    } else null
                }

                database.child("users").child(user.uid).child("schedule").setValue(scheduleList).await()

                withContext(Dispatchers.Main) {
                    cacheCsvData(fileName, scheduleList)
                    saveCsvFileName(fileName)
                    isCsvLoaded = true
                    updateUploadUiState(true)
                    binding.fileNameText.text = "Uploaded: $fileName"
                    loadMatchedSessions()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error parsing/uploading CSV: ${e.message}", e)
                withContext(Dispatchers.Main) { binding.fileNameText.text = "Error: ${e.message}" }
            }
        }
    }

    // ------------------------------- //
    // CSV Cache Management
    // ------------------------------- //

    private fun cacheCsvData(fileName: String, data: List<Map<String, String>>) {
        val userId = currentUser?.uid ?: return
        val prefs = requireContext().getSharedPreferences("csv_cache_$userId", Activity.MODE_PRIVATE)
        val dataString = data.joinToString(";") {
            "${it["subject"]},${it["room"]},${it["time"]},${it["instructor"]}"
        }
        prefs.edit {
            putString("csvFileName", fileName)
            putString("csvData", dataString)
        }
        Log.d(TAG, "Cached CSV data locally: $fileName")
    }

    @SuppressLint("SetTextI18n")
    private fun checkCachedCsvData() {
        val userId = currentUser?.uid ?: return
        showLoadingState(true)

        viewLifecycleOwner.lifecycleScope.launch {
            val prefs = requireContext().getSharedPreferences("csv_cache_$userId", Activity.MODE_PRIVATE)
            val fileName = prefs.getString("csvFileName", null)
            val dataString = prefs.getString("csvData", null)

            if (fileName != null && dataString != null) {
                isCsvLoaded = true
                updateUploadUiState(true)
                binding.fileNameText.text = "Cached: $fileName"
                showLoadingState(false)
                clearRecyclerView()
                loadMatchedSessions()
            } else {
                val userRef = database.child("users").child(userId).child("schedule")
                userRef.get()
                    .addOnSuccessListener { snapshot ->
                        isCsvLoaded = snapshot.exists()
                        updateUploadUiState(snapshot.exists())
                        binding.fileNameText.text =
                            if (snapshot.exists()) "Loaded from Firebase" else "No schedule found"
                        showLoadingState(false)
                        if (snapshot.exists()) {
                            // 🔁 Cache it locally for next launch
                            val scheduleList = snapshot.children.mapNotNull { it.value as? Map<String, String> }
                            cacheCsvData("Firebase Backup", scheduleList)
                            loadMatchedSessions()
                        }
                    }
                    .addOnFailureListener { e ->
                        updateUploadUiState(false)
                        showLoadingState(false)
                        Log.e(TAG, "Error loading from Firebase: ${e.message}")
                    }
            }
        }
    }

    private fun removeCsvData() {
        val userId = currentUser?.uid ?: return
        val userRef = database.child("users").child(userId)

        // ✅ Show confirmation dialog before removing CSV
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Remove CSV File")
            .setMessage("Are you sure you want to remove the CSV file?")
            .setPositiveButton("Yes") { dialog, _ ->
                dialog.dismiss()

                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        // Remove schedule data from Firebase
                        userRef.child("schedule").removeValue().await()
                        userRef.child("csvFileName").removeValue().await()

                        withContext(Dispatchers.Main) {
                            // ✅ Clear the correct user-specific cache
                            requireContext().getSharedPreferences("csv_cache_$userId", Activity.MODE_PRIVATE)
                                .edit { clear() }

                            // ✅ Clear any saved CSV filename from global prefs
                            requireContext().getSharedPreferences("csv_prefs", Activity.MODE_PRIVATE)
                                .edit { remove("csvFileName") }

                            // ✅ Reset state and update UI
                            isCsvLoaded = false
                            uploadedCsvUri = null
                            updateUploadUiState(false)
                            clearRecyclerView()
                            binding.fileNameText.text = "CSV removed"
                            Log.d(TAG, "✅ CSV data and cache removed successfully")

                            // Reload fragment data to ensure UI sync
                            loadMatchedSessions()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error deleting CSV data: ${e.message}", e)
                    }
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                Log.d(TAG, "❎ CSV removal canceled by user")
            }
            .show()
    }


    // ------------------------------- //
    // UI Updates
    // ------------------------------- //

    private fun showLoadingState(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.fileNameText.alpha = if (isLoading) 0.6f else 1f
    }

    private fun updateUploadUiState(isLoaded: Boolean) {
        if (isLoaded) {
            binding.dropArea.visibility = View.GONE
            binding.manageArea.visibility = View.VISIBLE
            binding.fileNameText.text = getSavedCsvFileName()
            showStudentSchedule(true)
            clearRecyclerView()
            loadMatchedSessions()
        } else {
            binding.dropArea.visibility = View.VISIBLE
            binding.manageArea.visibility = View.GONE
            showStudentSchedule(false)
            clearRecyclerView()
        }
    }

    // ------------------------------- //
    // Session Matching and Display
    // ------------------------------- //

    private fun loadMatchedSessions() {
        viewLifecycleOwner.lifecycleScope.launch {
            if (!isCsvLoaded) {
                showUploadCsvPrompt()
                clearRecyclerView()
                return@launch
            }
            showLoadingState(true)
            try {
                val matchedSessions = SessionHelper.getMatchedSessions()

                if (matchedSessions.isNotEmpty()) {
                    setupRecyclerView(matchedSessions.map { session ->
                        session.copy(
                            status = if (session.status == "Live") "Live" else "Upcoming"
                        )
                    })
                } else {
                    clearRecyclerView()
                    showNoClassesAvailableDialog()
                }
            } catch (e: Exception) {
                Log.e("ScheduleFragment", "Error loading matched sessions: ${e.message}", e)
                clearRecyclerView()
            } finally {
                showLoadingState(false)
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun showUploadCsvPrompt() {
        if (!isAdded) return

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("CSV Required")
            .setMessage("Please upload a CSV file to load your subjects and schedules.")
            .setPositiveButton("Upload Now") { dialog, _ ->
                dialog.dismiss()
                openFileManager()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showNoClassesAvailableDialog() {

        if (!isAdded) return

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("No Classes Found")
            .setMessage("No classes found with the schedule you uploaded.")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }


    @SuppressLint("NotifyDataSetChanged")
    private fun clearRecyclerView() {
        binding.upcomingClassesRecyclerView.apply {
            adapter = SessionAdapter(emptyList())
            adapter?.notifyDataSetChanged()
            visibility = View.GONE
        }
        Log.d(TAG, "🧹 Cleared RecyclerView")
    }

    private fun setupRecyclerView(sessions: List<Session>) {
        with(binding.upcomingClassesRecyclerView) {
            visibility = View.VISIBLE
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
            adapter = SessionAdapter(sessions)
        }
        Log.d(TAG, "📋 RecyclerView loaded with ${sessions.size} sessions")
    }

    private fun showStudentSchedule(show: Boolean) {
        val visibility = if (show) View.VISIBLE else View.GONE
        binding.todayLabel.visibility = visibility
        binding.dateLabel.visibility = visibility
    }

    // ------------------------------- //
    // SharedPreferences Helpers
    // ------------------------------- //

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
            .edit { putString("csvFileName", name) }
    }

    private fun getSavedCsvFileName(): String {
        return requireContext().getSharedPreferences("csv_prefs", Activity.MODE_PRIVATE)
            .getString("csvFileName", "No CSV uploaded") ?: "No CSV uploaded"
    }
}
