package com.example.attendease.ui.session

import androidx.lifecycle.ViewModel
import com.example.attendease.data.repositories.SessionRepository

class SessionViewModel(private val repo: SessionRepository) : ViewModel() {
    fun updateQr(sessionId: String, qrCode: String) {
        repo.updateQrCode(sessionId, qrCode)
    }
}
