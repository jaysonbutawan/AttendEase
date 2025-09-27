package com.example.attendease.data.repositories

import android.util.Log
import com.example.attendease.data.model.ClassSession
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SessionRepository {
    private val db = FirebaseDatabase.getInstance().getReference("sessions")

    fun createSession(session: ClassSession, callback: (Boolean, String?) -> Unit) {
        val sessionId = db.push().key
        if (sessionId == null) {
            callback(false, null)
            return
        }
        val sessionWithId = session.copy(sessionId = sessionId)
        FirebaseDatabase.getInstance()
            .getReference("rooms")
            .child(session.roomId ?: "unknown")
            .child("sessions")
            .child(sessionId)
            .setValue(sessionWithId)
            .addOnSuccessListener { callback(true, sessionId) }
            .addOnFailureListener { callback(false, null) }
    }

    fun getSessions(
        onResult: (List<ClassSession>) -> Unit,
        onError: (String) -> Unit
    ) {
        val roomsRef = FirebaseDatabase.getInstance().getReference("rooms")

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
                            sessionList.add(it)

                            // Debugging log
                            Log.d(
                                "SessionRepository",
                                "Loaded session: ${it.subject} in Room: $roomName ($roomId)"
                            )
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





    fun updateQrCode(sessionId: String, qrCode: String) {
        val qrData = mapOf(
            "qrCode" to qrCode,
            "qrValid" to true,
            "updatedAt" to System.currentTimeMillis()
        )
        db.child(sessionId).updateChildren(qrData)
    }
}
