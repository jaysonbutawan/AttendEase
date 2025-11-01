package com.example.attendease.teacher.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.attendease.R
import com.example.attendease.common.firebase.AuthRepository
import com.example.attendease.databinding.TeacherDialogEditProfileBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class EditProfileBottomSheet : BottomSheetDialogFragment() {

    private var _binding: TeacherDialogEditProfileBinding? = null
    private val binding get() = _binding!!

    private var name: String? = null
    private var image: String? = null
    private var email: String? = null

    override fun getTheme(): Int = R.style.AppTheme_BottomSheetDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            name = it.getString("name")
            email = it.getString("email")
            image = it.getString("image")
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = TeacherDialogEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.etEditName.setText(name)

        if (!image.isNullOrEmpty()) {
            Glide.with(requireContext())
                .load(image)
                .placeholder(R.drawable.default_avatar)
                .error(R.drawable.default_avatar)
                .into(binding.profileImageEdit)
        } else {
            binding.profileImageEdit.setImageResource(R.drawable.default_avatar)
        }

        binding.btnSaveProfile.setOnClickListener {
            val newName = binding.etEditName.text.toString().trim()

            if (newName.isEmpty()) {
                AlertDialog.Builder(requireContext())
                    .setTitle("Empty Field")
                    .setMessage("Please provide input before saving.")
                    .setPositiveButton("OK") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setCancelable(true)
                    .show()
                return@setOnClickListener
            }

            val authRepository = AuthRepository(FirebaseAuth.getInstance())

            lifecycleScope.launch {
                val result = authRepository.updateUserFullName(newName)
                if (result.isSuccess) {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Success")
                        .setMessage("Successfully updated")
                        .setPositiveButton("OK") { dialog, _ ->
                            dialog.dismiss()
                        }
                        .setCancelable(true)
                        .show()
                    dismiss()
                } else {
                    Toast.makeText(requireContext(), "Failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.ivEditImage.setOnClickListener {
            Toast.makeText(requireContext(), "Change image clicked!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(name: String?, email: String?, image: String?): EditProfileBottomSheet {
            val fragment = EditProfileBottomSheet()
            val args = Bundle().apply {
                putString("name", name)
                putString("email", email)
                putString("image", image)
            }
            fragment.arguments = args
            return fragment
        }
    }
}
