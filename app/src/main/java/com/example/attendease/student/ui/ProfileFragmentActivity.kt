package com.example.attendease.student.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.attendease.common.ui.auth.StudentLoginActivity
import com.example.attendease.databinding.FragmentProfileScreenBinding
import com.example.attendease.common.firebase.AuthRepository
import com.google.firebase.auth.FirebaseAuth

class ProfileFragmentActivity : Fragment() {
    private lateinit var auth : AuthRepository
private lateinit var binding: FragmentProfileScreenBinding

    @SuppressLint("SuspiciousIndentation")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
      binding = FragmentProfileScreenBinding.inflate(inflater, container, false)
        auth = AuthRepository(FirebaseAuth.getInstance())
        return binding.root
    }
   override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        binding.logoutButton.setOnClickListener {
            auth.signOut()
            val intent = Intent(requireContext(), StudentLoginActivity::class.java)
            startActivity(intent)
        }
    }
}