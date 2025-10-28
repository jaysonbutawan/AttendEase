package com.example.attendease.student.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendease.student.data.Session
import com.example.attendease.student.helper.SessionHelper
import kotlinx.coroutines.launch

class SubjectListViewModel : ViewModel() {

    private val _subjects = MutableLiveData<List<Session>>()
    val subjects: LiveData<List<Session>> get() = _subjects

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    private val _emptyState = MutableLiveData<Boolean>()
    val emptyState: LiveData<Boolean> get() = _emptyState

    /**
     * Fetch only matched sessions based on uploaded CSV.
     * Handles loading, empty, and error UI states properly.
     */
    fun fetchMatchedSubjects() {
        _isLoading.value = true
        _errorMessage.value = null
        _emptyState.value = false

        viewModelScope.launch {
            try {
                Log.d("SubjectListVM", "🚀 Fetching matched subjects...")
                val matchedSessions = SessionHelper.getMatchedSessions()

                if (matchedSessions.isEmpty()) {
                    Log.w("SubjectListVM", "⚠️ No matched subjects found.")
                    _emptyState.postValue(true)
                } else {
                    Log.d("SubjectListVM", "✅ Found ${matchedSessions.size} matched subjects.")
                    _subjects.postValue(matchedSessions)
                }
            } catch (e: Exception) {
                Log.e("SubjectListVM", "❌ Error fetching matched subjects: ${e.message}", e)
                _errorMessage.postValue("Failed to load subjects. Please try again.")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
}
