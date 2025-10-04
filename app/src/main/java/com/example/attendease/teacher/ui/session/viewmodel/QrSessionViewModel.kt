package com.example.attendease.teacher.ui.session.viewmodel

import androidx.lifecycle.ViewModel
import com.example.attendease.teacher.data.repositories.SessionRepository

class QrSessionViewModel(private val repo: SessionRepository) : ViewModel() {
    fun updateQr(roomId: String, sessionId: String, qrCode: String) {
        repo.updateQrCode(roomId,sessionId, qrCode)
    }
}