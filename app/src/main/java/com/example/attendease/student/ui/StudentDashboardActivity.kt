package com.example.attendease.student.ui

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.attendease.R
import com.example.attendease.databinding.StudentDashboardScreenBinding

class StudentDashboardActivity : AppCompatActivity() {
    private lateinit var binding: StudentDashboardScreenBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        binding = StudentDashboardScreenBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)


    }
}