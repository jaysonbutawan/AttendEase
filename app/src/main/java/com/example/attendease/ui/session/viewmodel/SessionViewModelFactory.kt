package com.example.attendease.ui.session.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.attendease.data.repositories.SessionRepository
import com.example.attendease.ui.session.viewmodel.QrSessionViewModel

class SessionViewModelFactory(
    private val repo: SessionRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(QrSessionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return QrSessionViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}