package com.example.attendease.teacher.ui.profile

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.attendease.R
import com.example.attendease.common.firebase.AuthRepository
import com.example.attendease.databinding.TeacherDialogEditProfileBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class EditProfileBottomSheet : BottomSheetDialogFragment() {

    // ✅ ViewBinding reference
    private var _binding: TeacherDialogEditProfileBinding? = null
    private val binding get() = _binding!!

    // ✅ Use lazy-loaded variables (read inside lifecycle)
    private var name: String? = null
    private var image: String? = null
    private var email: String? = null

    override fun getTheme(): Int = R.style.AppTheme_BottomSheetDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ✅ Retrieve arguments safely here (before view creation)
        arguments?.let {
            name = it.getString("name")
            email = it.getString("email")
            image = it.getString("image")
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<View>(
                com.google.android.material.R.id.design_bottom_sheet
            ) ?: return@setOnShowListener
            setupBottomSheetBehavior(bottomSheet)
        }
        return dialog
    }

    private fun setupBottomSheetBehavior(bottomSheet: View) {
        val behavior = BottomSheetBehavior.from(bottomSheet)
        val displayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels
        val topMarginPx = (50 * displayMetrics.density).toInt()

        behavior.expandedOffset = topMarginPx
        behavior.state = BottomSheetBehavior.STATE_EXPANDED

        val layoutParams = bottomSheet.layoutParams
        if (layoutParams.height == ViewGroup.LayoutParams.WRAP_CONTENT) {
            layoutParams.height = screenHeight - topMarginPx
            bottomSheet.layoutParams = layoutParams
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

        // ✅ Populate data into views
        binding.etEditName.setText(name)
        binding.etEditEmail.setText(email)

        if (!image.isNullOrEmpty()) {
            Glide.with(requireContext())
                .load(image)
                .placeholder(R.drawable.default_avatar)
                .error(R.drawable.default_avatar)
                .into(binding.profileImageEdit)
        } else {
            binding.profileImageEdit.setImageResource(R.drawable.default_avatar)
        }

        // ✅ Handle Save button
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
                    Toast.makeText(requireContext(), "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                    dismiss()
                } else {
                    Toast.makeText(requireContext(), "Failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }


        // ✅ Handle Edit Image click
        binding.ivEditImage.setOnClickListener {
            Toast.makeText(requireContext(), "Change image clicked!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Prevent memory leaks
    }

    companion object {
        // ✅ Static helper for creating instance with arguments
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
