package com.example.attendease.student.viewmodel

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.attendease.student.data.AttendanceStatus
import com.example.attendease.student.helper.SessionHelper
import kotlinx.coroutines.launch

class AttendanceStatusListViewModel : ViewModel() {

    private val _attendanceList = MutableLiveData<List<AttendanceStatus>>()
    val attendanceList: LiveData<List<AttendanceStatus>> get() = _attendanceList

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> get() = _errorMessage

    private val _emptyState = MutableLiveData<Boolean>()
    val emptyState: LiveData<Boolean> get() = _emptyState

    @RequiresApi(Build.VERSION_CODES.O)
    fun fetchAttendanceForSession(roomId: String, sessionId: String) {
        _isLoading.value = true
        _errorMessage.value = null
        _emptyState.value = false

        viewModelScope.launch {
            try {
                Log.d("AttendanceVM", "🚀 Fetching attendance for room: $roomId, session: $sessionId")

                val attendanceRecords = SessionHelper.getStudentAttendance(roomId, sessionId)

                if (attendanceRecords.isEmpty()) {
                    Log.w("AttendanceVM", "⚠️ No attendance records found for session: $sessionId")
                    _emptyState.postValue(true)
                } else {
                    Log.d("AttendanceVM", "✅ Found ${attendanceRecords.size} attendance records.")
                    _attendanceList.postValue(attendanceRecords)
                }
            } catch (e: Exception) {
                Log.e("AttendanceVM", "❌ Error fetching attendance: ${e.message}", e)
                _errorMessage.postValue("Failed to load attendance. Please try again.")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
}
