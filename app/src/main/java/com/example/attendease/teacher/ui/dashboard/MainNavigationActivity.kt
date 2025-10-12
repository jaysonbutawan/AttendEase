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
import androidx.core.graphics.toColorInt

class MainNavigationActivity : AppCompatActivity() {

    private lateinit var binding: MainNavScreenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainNavScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val user = FirebaseAuth.getInstance().currentUser
        val userId = user?.uid ?: return

        val databaseRef = com.google.firebase.database.FirebaseDatabase.getInstance()
            .getReference("users").child(userId)

        // ✅ Real-time listener for user info
        databaseRef.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                val fullName = snapshot.child("fullname").getValue(String::class.java)
                val email = snapshot.child("email").getValue(String::class.java)
                val imageUrl = user.photoUrl?.toString()

                val displayName = fullName ?: user.displayName

                // Update UI immediately when data changes
                setupUserInfo(displayName, email, imageUrl)
                setupClickListeners(displayName, email, imageUrl)
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                // Handle database error
            }
        })
    }



    private fun setupUserInfo(name: String?, email: String?, imageUrl: String?) = with(binding) {
        tvUserName.text = name
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

    private fun setupClickListeners(name: String?, email: String?, imageUrl: String?) =
        with(binding) {

            setupCardToggle(cvManageClasses) {
                startActivity(
                    Intent(
                        this@MainNavigationActivity,
                        ManageSessionActivity::class.java
                    )
                )
            }

            setupCardToggle(cvAttendanceReport) {
                startActivity(
                    Intent(
                        this@MainNavigationActivity,
                        AttendanceReportActivity::class.java
                    )
                )
            }

            profileImage.setOnClickListener {
                val intent =
                    Intent(this@MainNavigationActivity, ProfileActivity::class.java).apply {
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

            // Run click action
            onClick()
        }
    }
}


