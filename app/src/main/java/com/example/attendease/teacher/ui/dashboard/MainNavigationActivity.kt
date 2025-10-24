package com.example.attendease.teacher.ui.dashboard

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.bumptech.glide.Glide
import com.example.attendease.R
import com.example.attendease.databinding.MainNavScreenBinding
import com.example.attendease.teacher.ui.profile.ProfileActivity
import com.example.attendease.teacher.ui.session.ui.ManageSessionActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import androidx.core.graphics.toColorInt
import com.example.attendease.teacher.ui.session.ui.AttendanceReportActivity
import com.example.attendease.teacher.ui.session.ui.HistorySubjectActivity

class MainNavigationActivity : AppCompatActivity() {

    private lateinit var binding: MainNavScreenBinding
    private lateinit var databaseRef: DatabaseReference
    private var userListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainNavScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val user = FirebaseAuth.getInstance().currentUser
        val userId = user?.uid ?: return

        databaseRef = FirebaseDatabase.getInstance()
            .getReference("users")
            .child(userId)

        // ✅ Attach listener safely
        userListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Make sure activity is still alive before updating UI
                if (isDestroyed || isFinishing) return

                val fullName = snapshot.child("fullname").getValue(String::class.java)
                val email = snapshot.child("email").getValue(String::class.java)
                val imageUrl = FirebaseAuth.getInstance().currentUser?.photoUrl?.toString()
                val displayName = fullName ?: FirebaseAuth.getInstance().currentUser?.displayName

                setupUserInfo(displayName, email, imageUrl)
                setupClickListeners(displayName, email, imageUrl)
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle database error
            }
        }

        databaseRef.addValueEventListener(userListener as ValueEventListener)
    }

    private fun setupUserInfo(name: String?, email: String?, imageUrl: String?) = with(binding) {
        tvUserName.text = name

        // ✅ Guard Glide with lifecycle check
        if (!this@MainNavigationActivity.isDestroyed && !this@MainNavigationActivity.isFinishing) {
            if (!imageUrl.isNullOrEmpty()) {
                Glide.with(this@MainNavigationActivity)
                    .load(imageUrl)
                    .placeholder(R.drawable.default_avatar)
                    .error(R.drawable.default_avatar)
                    .into(profileImage)
            } else {
                profileImage.setImageResource(R.drawable.default_avatar)
            }
        }
    }

    private fun setupClickListeners(name: String?, email: String?, imageUrl: String?) = with(binding) {
        setupCardToggle(cvManageClasses) {
            startActivity(Intent(this@MainNavigationActivity, ManageSessionActivity::class.java))
        }

        setupCardToggle(cvAttendanceReport) {
            startActivity(Intent(this@MainNavigationActivity, HistorySubjectActivity::class.java))
        }

        profileImage.setOnClickListener {
            val intent = Intent(this@MainNavigationActivity, ProfileActivity::class.java).apply {
                putExtra("name", name)
                putExtra("email", email)
                putExtra("image", imageUrl)
            }
            startActivity(intent)
        }
    }

    private fun setupCardToggle(
        cardView: CardView,
        selectedColorHex: String = "#6E8CFB",
        onClick: () -> Unit
    ) {
        cardView.setOnClickListener {
            val selectedColor = selectedColorHex.toColorInt()
            val normalColor = Color.WHITE

            cardView.setCardBackgroundColor(selectedColor)
            cardView.postDelayed({
                cardView.setCardBackgroundColor(normalColor)
            }, 200)

            onClick()
        }
    }

    // ✅ Detach Firebase listener when leaving the screen
    override fun onDestroy() {
        super.onDestroy()
        userListener?.let { databaseRef.removeEventListener(it) }
    }
}
