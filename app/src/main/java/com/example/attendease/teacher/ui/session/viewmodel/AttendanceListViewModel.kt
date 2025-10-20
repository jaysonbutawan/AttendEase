package com.example.attendease.teacher.ui.session.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.attendease.teacher.data.model.AttendanceRecord
import com.example.attendease.teacher.data.repositories.SessionRepository

class AttendanceListViewModel : ViewModel() {

    private val repository = SessionRepository()

    private val _attendanceList = MutableLiveData<List<AttendanceRecord>>()
    val attendanceList: LiveData<List<AttendanceRecord>> get() = _attendanceList

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> get() = _loading

    /**
     * Loads all attendance records for a given room and session.
     */
    fun loadAttendance(roomId: String, sessionId: String) {
        _loading.postValue(true)

        repository.getAttendancePerSession(
            roomId = roomId,
            sessionId = sessionId,
            onResult = { records ->
                _attendanceList.postValue(records)
                _loading.postValue(false)
            },
            onError = { errorMessage ->
                _error.postValue(errorMessage)
                _loading.postValue(false)
            }
        )
    }
}