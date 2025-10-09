package com.example.attendease.common.firebase

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.attendease.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val firebaseAuth: FirebaseAuth
) {
    private val database = FirebaseDatabase.getInstance().reference


    suspend fun signInWithGoogle(context: Context, role: String): Result<Unit> {
        val credentialManager = CredentialManager.create(context)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .setServerClientId(context.getString(R.string.default_web_client_id))
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential

            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val idToken = googleIdTokenCredential.idToken
                val authCredential = GoogleAuthProvider.getCredential(idToken, null)

                val authResult = firebaseAuth.signInWithCredential(authCredential).await()
                val user = authResult.user ?: return Result.failure(Exception("User not found"))
                val uid = user.uid

                val userSnapshot = database.child("users").child(uid).get().await()
                if (userSnapshot.exists()) {
                    val storedRole = userSnapshot.child("role").getValue(String::class.java)
                    if (storedRole != role) {
                        firebaseAuth.signOut()
                        return Result.failure(Exception("Access denied: account is for $storedRole only"))
                    }
                } else {
                    // First-time Google login → store new role
                    val userData = mapOf("email" to user.email, "role" to role)
                    database.child("users").child(uid).setValue(userData).await()
                }

                Result.success(Unit)
            } else {
                Result.failure(Exception("Invalid credential type"))
            }
        } catch (e: NoCredentialException) {
            val addAccountIntent = Intent(Settings.ACTION_ADD_ACCOUNT).apply {
                putExtra(Settings.EXTRA_ACCOUNT_TYPES, arrayOf("com.google"))
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(addAccountIntent)
            Result.failure(Exception("No Google accounts available, please add one."))
        } catch (e: GetCredentialException) {
            Result.failure(e)
        }
    }


    suspend fun signInWithEmail(email: String, password: String, role: String): Result<Unit> {
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val uid = authResult.user?.uid ?: return Result.failure(Exception("Invalid user"))

            val userSnapshot = database.child("users").child(uid).get().await()
            val storedRole = userSnapshot.child("role").getValue(String::class.java)

            if (storedRole == null) {
                firebaseAuth.signOut()
                return Result.failure(Exception("Account role not found"))
            }

            if (storedRole != role) {
                firebaseAuth.signOut()
                return Result.failure(Exception("Access denied: this account is for $storedRole only"))
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUpWithEmail(email: String, password: String, role: String): Result<Unit> {
        return try {
            if (email.isBlank() || password.isBlank()) {
                return Result.failure(IllegalArgumentException("Email and password cannot be empty"))
            }

            // Step 1: Check if this email exists under a different role
            val usersSnapshot = database.child("users").get().await()
            for (userSnap in usersSnapshot.children) {
                val existingEmail = userSnap.child("email").getValue(String::class.java)
                val existingRole = userSnap.child("role").getValue(String::class.java)

                if (existingEmail.equals(email, ignoreCase = true) && existingRole != role) {
                    return Result.failure(Exception("This email is already registered as a $existingRole"))
                }
            }

            // Step 2: Create user in Firebase Authentication
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: return Result.failure(Exception("User creation failed"))

            // Step 3: Save role in Realtime Database
            val userData = mapOf(
                "email" to email,
                "role" to role
            )

            database.child("users").child(user.uid).setValue(userData).await()

            // Optional: send email verification
            user.sendEmailVerification().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        firebaseAuth.signOut()
    }

}