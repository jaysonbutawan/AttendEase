package com.example.attendease.student.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.attendease.databinding.FragmentScheduleScreenBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ScheduleFragmentActivity : Fragment() {

    private var _binding: FragmentScheduleScreenBinding? = null
    private val binding get() = _binding!!
    private var isCsvLoaded = false

    private val database = FirebaseDatabase.getInstance().reference
    private val storage = FirebaseStorage.getInstance().reference
    private val currentUser = FirebaseAuth.getInstance().currentUser

    private var uploadedCsvUri: Uri? = null

    // ✅ Modern file picker
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
                    e.printStackTrace()
                }

                uploadedCsvUri = uri
                val fileName = getFileName(uri)

                // ✅ Upload to Firebase
                uploadCsvToFirebase(uri, fileName)
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

        isCsvLoaded = false
        updateUploadUiState(isCsvLoaded)

        binding.dropArea.setOnClickListener { openFileManager() }
        binding.updateCsvButton.setOnClickListener { openFileManager() }
        binding.removeCsvButton.setOnClickListener { removeCsvData() }
    }

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

    private fun uploadCsvToFirebase(uri: Uri, fileName: String) {
        val userId = currentUser?.uid ?: return

        lifecycleScope.launch {
            binding.fileNameText.text = "Uploading..."

            val csvRef = storage.child("csv_uploads/$userId/$fileName")

            try {
                // Firebase SDK runs async, but we can await() using coroutines
                csvRef.putFile(uri).await()
                val downloadUrl = csvRef.downloadUrl.await()

                // Save to database (also async)
                val userRef = database.child("users").child(userId)
                userRef.child("csvFileName").setValue(fileName).await()
                userRef.child("csvFileUrl").setValue(downloadUrl.toString()).await()

                saveCsvFileName(fileName)
                isCsvLoaded = true
                updateUploadUiState(true)

            } catch (e: Exception) {
                binding.fileNameText.text = "Upload failed: ${e.message}"
            }
        }
    }


    private fun removeCsvData() {
        val userId = currentUser?.uid ?: return
        val userRef = database.child("users").child(userId)

        userRef.child("csvFileName").removeValue()
        userRef.child("csvFileUrl").removeValue()

        isCsvLoaded = false
        updateUploadUiState(false)
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
