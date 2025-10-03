package com.example.attendease.ui.dashboard

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.attendease.R
import com.example.attendease.databinding.AttendanceReportScreenBinding
import com.example.attendease.databinding.MainNavScreenBinding

class AttendanceReportActivity : AppCompatActivity() {
    private lateinit var binding: AttendanceReportScreenBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AttendanceReportScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}