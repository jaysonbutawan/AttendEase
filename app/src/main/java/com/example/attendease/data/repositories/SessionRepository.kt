package com.example.attendease.data.repositories

import android.util.Log
import com.example.attendease.data.model.ClassSession
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class SessionRepository {
    private val roomsRef = FirebaseDatabase.getInstance().getReference("rooms")

    // ✅ Create a new session under the correct room
    fun createSession(session: ClassSession, callback: (Boolean, String?) -> Unit) {
        val sessionId = roomsRef.push().key
        if (sessionId == null) {
            callback(false, null)
            return
        }

        val sessionWithId = session.copy(sessionId = sessionId)
        roomsRef.child(session.roomId ?: "unknown")
            .child("sessions")
            .child(sessionId)
            .setValue(sessionWithId)
            .addOnSuccessListener { callback(true, sessionId) }
            .addOnFailureListener { callback(false, null) }
    }

    // ✅ Get all sessions for the logged-in teacher
    fun getSessions(
        onResult: (List<ClassSession>) -> Unit,
        onError: (String) -> Unit
    ) {
        val currentTeacherId = FirebaseAuth.getInstance().currentUser?.uid

        roomsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val sessionList = mutableListOf<ClassSession>()
                for (roomSnapshot in snapshot.children) {
                    val roomId = roomSnapshot.key
                    val roomName = roomSnapshot.child("name").getValue(String::class.java) ?: "Unknown Room"
                    val sessionsSnapshot = roomSnapshot.child("sessions")

                    for (sessionSnapshot in sessionsSnapshot.children) {
                        val session = sessionSnapshot.getValue(ClassSession::class.java)
                        session?.let {
                            it.roomName = roomName
                            it.roomId = roomId

                            // Only sessions of the logged-in teacher
                            if (it.teacherId == currentTeacherId) {
                                sessionList.add(it)

                                Log.d(
                                    "SessionRepository",
                                    "Loaded session: ${it.subject} in Room: $roomName ($roomId)"
                                )
                            }
                        }
                    }
                }
                onResult(sessionList)
            }

            override fun onCancelled(error: DatabaseError) {
                onError(error.message)
            }
        })
    }

    // ✅ Update QR code for a session (inside its room)
    fun updateQrCode(roomId: String, sessionId: String, qrCode: String) {
        val qrData = mapOf(
            "qrCode" to qrCode,
            "qrValid" to true,
            "updatedAt" to System.currentTimeMillis()
        )

        roomsRef.child(roomId)
            .child("sessions")
            .child(sessionId)
            .updateChildren(qrData)
    }
}
