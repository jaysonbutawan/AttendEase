package com.example.attendease.data.repositories

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

    fun getSessions(onResult: (List<ClassSession>) -> Unit, onError: (String) -> Unit) {
        db.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val sessionList = mutableListOf<ClassSession>()
                for (sessionSnapshot in snapshot.children) {
                    val session = sessionSnapshot.getValue(ClassSession::class.java)
                    session?.let { sessionList.add(it) }
                }
                onResult(sessionList)
            }

            override fun onCancelled(error: DatabaseError) {
                onError(error.message)
            }
        })
    }



    fun updateQrCode(sessionId: String, qrCode: String) {
        db.child(sessionId).child("qrCode").setValue(qrCode)
        db.child(sessionId).child("qrGeneratedAt").setValue(System.currentTimeMillis())
    }

    fun generateSessionId(): String {
        return "session_${System.currentTimeMillis()}"
    }
}
