package com.example.attendease.teacher.ui.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.attendease.teacher.data.model.ClassSession
import com.example.attendease.teacher.data.repositories.SessionRepository

class SessionListViewModel : ViewModel() {

    private val repository = SessionRepository()

    private val _sessions = MutableLiveData<List<ClassSession>>()
    val sessions: LiveData<List<ClassSession>> get() = _sessions

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

    fun loadSessions() {
        repository.getSessions(
            onResult = { sessionList ->
                _sessions.postValue(sessionList)

            },
            onError = { errorMessage ->
                _error.postValue(errorMessage)
            }
        )
    }
}