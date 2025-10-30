package com.example.attendease.student.data

data class Session(
    val sessionId: String = "",
    val subject: String = "",
    val room: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val instructor: String = "",
    val roomId: String = "",
    val status: String = "",
    val attendance: List<AttendanceStatus> = emptyList()
)

