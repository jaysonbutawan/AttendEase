package com.example.attendease.teacher.ui.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.attendease.teacher.data.model.AttendanceRecord
import com.example.attendease.teacher.data.repositories.SessionRepository
import com.google.firebase.database.FirebaseDatabase

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
                Log.d("AttendanceViewModel", "🧩 Received ${records.size} records from repository")
                records.forEach { Log.d("AttendanceViewModel", "🔄 ${it.name}: ${it.confidence}") }

            },
            onError = { errorMessage ->
                _error.postValue(errorMessage)
                _loading.postValue(false)
            }
        )
    }


    //use in AttendanceListActivity
    fun fetchAttendanceList(roomId: String?, sessionId: String, date: String) {
        if (roomId == null) {
            _error.postValue("Missing roomId — cannot load attendance.")
            return
        }

        repository.getAttendanceByDate(
            roomId = roomId,
            sessionId = sessionId,
            date = date, // ✅ pass selected date
            onResult = { records ->
                _attendanceList.postValue(records)
            },
            onError = { err ->
                _error.postValue(err)
            }
        )
    }

    fun updateAttendanceStatus(roomId: String?, sessionId: String?, date: String?, record: AttendanceRecord?) {
        if (roomId.isNullOrBlank() || sessionId.isNullOrBlank() || date.isNullOrBlank() || record == null) {
            _error.postValue("Missing data: room, session, date, or record is invalid.")
            return
        }

        val studentId = record.id ?: run {
            _error.postValue("Invalid student ID")
            return
        }

        val studentRef = FirebaseDatabase.getInstance()
            .getReference("rooms")
            .child(roomId)
            .child("sessions")
            .child(sessionId)
            .child("attendance")
            .child(date)
            .child(studentId)

        studentRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val currentData = snapshot.getValue(AttendanceRecord::class.java)
                if (currentData != null) {
                    // Decide status based on lateDuration
                    val newStatus = if ((currentData.lateDuration ?: 0) > 0) {
                        "Late"
                    } else {
                        "Present"
                    }

                    studentRef.child("status").setValue(newStatus)
                        .addOnSuccessListener {
                            val updatedList = attendanceList.value?.map {
                                if (it.id == studentId) it.copy(status = newStatus) else it
                            } ?: emptyList()
                            _attendanceList.postValue(updatedList)
                        }
                        .addOnFailureListener {
                            _error.postValue("Failed to update status for $studentId")
                        }
                } else {
                    _error.postValue("No attendance data found for this student.")
                }
            } else {
                _error.postValue("Attendance record not found in database.")
            }
        }.addOnFailureListener {
            _error.postValue("Error retrieving student data: ${it.message}")
        }
    }

}




