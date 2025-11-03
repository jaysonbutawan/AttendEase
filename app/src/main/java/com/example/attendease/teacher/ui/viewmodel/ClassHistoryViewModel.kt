package com.example.attendease.teacher.ui.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.attendease.teacher.data.model.ClassSession
import com.example.attendease.teacher.data.repositories.SessionRepository

class ClassHistoryViewModel : ViewModel() {

    private val repository = SessionRepository()

    private val _classHistoryList = MutableLiveData<List<ClassSession>>()
    val classHistoryList: LiveData<List<ClassSession>> get() = _classHistoryList

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    fun fetchClassHistory() {
        _isLoading.value = true
        Log.d("ClassHistoryVM", "📡 Fetching class history from Firebase...")

        repository.getSessionByDate(
            onResult = { sessions ->
                _isLoading.postValue(false)
                Log.d("ClassHistoryVM", "✅ Received ${sessions.size} total sessions from repository")

                if (sessions.isEmpty()) {
                    Log.w("ClassHistoryVM", "⚠️ No class sessions found in database.")
                } else {
                    sessions.forEach { session ->
                        Log.d(
                            "ClassHistoryVM",
                            "➡️ Room: ${session.roomName ?: "N/A"} | Date: ${session.date ?: "No Date"} | Subject: ${session.subject ?: "N/A"}"
                        )
                    }
                }

                _classHistoryList.postValue(sessions)
            },
            onError = { error ->
                _isLoading.postValue(false)
                _errorMessage.postValue(error)
                Log.e("ClassHistoryVM", "❌ Error fetching class history: $error")
            }
        )
    }
}
