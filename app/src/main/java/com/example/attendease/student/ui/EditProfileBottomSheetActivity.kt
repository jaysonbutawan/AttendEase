package com.example.attendease.student.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.attendease.R
import com.example.attendease.common.firebase.AuthRepository
import com.example.attendease.databinding.ActivityEditProfileBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class EditProfileBottomSheetActivity : BottomSheetDialogFragment() {

    private var _binding: ActivityEditProfileBottomSheetBinding? = null
    private val binding get() = _binding!!
    private var name: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            name = it.getString("name")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ActivityEditProfileBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.etEditName.setText(name)


        binding.btnSaveProfile.setOnClickListener {
            val newName = binding.etEditName.text.toString().trim()

            if (newName.isEmpty()) {
                Toast.makeText(requireContext(), "Name cannot be empty!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val authRepository = AuthRepository(FirebaseAuth.getInstance())

            lifecycleScope.launch {
                val result = authRepository.updateUserFullName(newName)
                if (result.isSuccess) {
                            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                                .setTitle("Profile Updated")
                                .setMessage("Profile changed successfully!")
                                .setCancelable(false)
                                .setPositiveButton("OK") { dialog, _ ->
                                    dialog.dismiss()
                                    dismiss()
                                }
                                .show()
                } else {
                    Toast.makeText(requireContext(), "Failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(name: String?): EditProfileBottomSheetActivity {
            val fragment = EditProfileBottomSheetActivity()
            val args = Bundle().apply {
                putString("name", name)
            }
            fragment.arguments = args
            return fragment
        }
    }
}