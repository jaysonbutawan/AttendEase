package com.example.attendease.ui.session

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.attendease.R
import com.example.attendease.databinding.ManageClassScreenBinding
import com.example.attendease.ui.classschedule.ClassScheduleDialog

class ManageSessionActivity : AppCompatActivity() {
    private lateinit var binding: ManageClassScreenBinding
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ManageClassScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.newClass.setOnClickListener {
            val dialog = ClassScheduleDialog()
            dialog.show(supportFragmentManager, "ClassScheduleDialog")
        }
    }
}