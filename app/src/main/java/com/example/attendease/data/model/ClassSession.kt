package com.example.attendease.data.model

data class ClassSession(
    val sessionId: String? = null,
    val roomId: String? = null,
    val subject: String? = null,
    val date: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val allowanceTime: Int? = null,
    val teacherId: String? = null,
    val qrCode: String? = null,
)
