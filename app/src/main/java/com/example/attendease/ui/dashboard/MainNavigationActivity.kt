package com.example.attendease.ui.dashboard

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.attendease.R
import com.example.attendease.databinding.MainNavScreenBinding

class MainNavigationActivity : AppCompatActivity() {

    private lateinit var binding: MainNavScreenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = MainNavScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainNavigation)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        binding.cvManageClasses.setOnClickListener {
            startActivity(Intent(this, ManageClassActivity::class.java))

        }

        binding.cvAttendanceReport.setOnClickListener {
            startActivity(Intent(this, AttendanceReportActivity::class.java))
        }


    }
}