package com.example.attendease.teacher.data.model

data class AttendanceRecord(
    var id: String? = null,
    var name: String? = null,
    var status: String? = null,
    var confidence: String? = null,
    var timeScanned: String? = null,
    var lateDuration: Int? = null,
    var totalOutsideTime: Int? = null
)
