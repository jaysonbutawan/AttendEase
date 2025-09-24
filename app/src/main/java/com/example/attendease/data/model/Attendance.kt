package com.example.attendease.data.model

data class Attendance(
    val status: String? = null,
    val isLate: Boolean? = null,
    val time_in: String? = null,
    val time_outs: Map<String, TimeOutLog>? = null,
    val total_time_outside: Int? = null
)
data class TimeOutLog(
    val out: String? = null,
    val `in`: String? = null
)

