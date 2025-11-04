package com.example.attendease.student.ui

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.example.attendease.R
import com.example.attendease.common.firebase.AuthRepository
import com.example.attendease.common.ui.auth.StudentLoginActivity
import com.example.attendease.databinding.FragmentProfileScreenBinding
import com.example.attendease.teacher.ui.activity.EditProfileBottomSheet
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ProfileFragmentActivity : Fragment() {

    private lateinit var auth: AuthRepository
    private lateinit var binding: FragmentProfileScreenBinding
    private lateinit var databaseRef: DatabaseReference
    private var userListener: ValueEventListener? = null

    @SuppressLint("SuspiciousIndentation")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileScreenBinding.inflate(inflater, container, false)
        auth = AuthRepository(FirebaseAuth.getInstance())

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            val intent = Intent(requireContext(), StudentLoginActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        } else {
            databaseRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(currentUser.uid)

            setupUserListener()
        }
        binding.btnEditProfile.setOnClickListener {
            showEditProfileSheet()

        }

        setupLogoutButton()
        return binding.root
    }
    private fun showEditProfileSheet() {
        val name = binding.tvStudentName.text.toString()

        val editProfileSheet = EditProfileBottomSheetActivity.newInstance(name)
        editProfileSheet.show(parentFragmentManager, "EditProfileBottomSheetActivity")
    }

    private fun setupUserListener() {
        userListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userName = snapshot.child("fullname").getValue(String::class.java)
                val course = snapshot.child("course").getValue(String::class.java)
                val imageUrl = snapshot.child("profileImage").getValue(String::class.java)

                setupUserInfo(
                    name = userName ?: "Unknown Student",
                    course = course ?: "No course assigned",
                    imageUrl = imageUrl
                )
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Error loading profile", Toast.LENGTH_SHORT).show()
            }
        }

        databaseRef.addValueEventListener(userListener!!)
    }

    private fun setupUserInfo(name: String, course: String, imageUrl: String?) = with(binding) {
        tvStudentName.text = name
        tvStudentProgram.text = course

        if (imageUrl != null && imageUrl.isNotEmpty()) {
            Glide.with(requireContext())
                .load(imageUrl)
                .placeholder(R.drawable.ic_profile)
                .error(R.drawable.ic_profile)
                .into(ivStudentAvatar)
        } else {
            ivStudentAvatar.setImageResource(R.drawable.ic_profile)
        }
    }
    private fun setupLogoutButton() {
        binding.btnStudentLogout.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Logout Confirmation")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Yes") { dialog, _ ->
                    auth.signOut()
                    Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()

                    val intent = Intent(requireContext(), StudentLoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    requireActivity().finish()
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .create()
                .show()
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        userListener?.let { databaseRef.removeEventListener(it) }
    }
}
