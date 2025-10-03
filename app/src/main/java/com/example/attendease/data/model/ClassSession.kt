package com.example.attendease.data.model

data class ClassSession(
    var sessionId: String? = null,
    var roomId: String? = null,
    var roomName: String? = null,
    var subject: String? = null,
    var date: String? = null,
    var startTime: String? = null,
    var endTime: String? = null,
    var allowanceTime: Int? = null,
    var teacherId: String? = null,
    var qrCode: String? = null,
)
