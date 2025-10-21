package com.example.attendease.teacher.data.repositories

import android.util.Log
import com.example.attendease.teacher.data.model.AttendanceRecord
import com.example.attendease.teacher.data.model.ClassSession
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SessionRepository {
    private val roomsRef = FirebaseDatabase.getInstance().getReference("rooms")

    // ✅ Create a new session under the correct room
    fun createSession(session: ClassSession, callback: (Boolean, String?) -> Unit) {
        // 1. Reference to the root of the database (or the /rooms node, depending on roomsRef setup)
        // Assuming roomsRef is a reference to the Firebase root or "rooms" node
        val sessionsRootRef = FirebaseDatabase.getInstance()
            .getReference("rooms")
            .child(session.roomId ?: "unknown") // Use the actual Room ID from the session object
            .child("sessions")

        // 2. Generate the unique key using push() on the sessions reference
        val newSessionRef = sessionsRootRef.push()
        val sessionId = newSessionRef.key

        if (sessionId == null) {
            callback(false, null)
            return
        }

        // 3. Create a copy of the session object with the newly generated ID
        val sessionWithId = session.copy(sessionId = sessionId)

        // 4. Set the value using the reference created by push()
        newSessionRef.setValue(sessionWithId)
            .addOnSuccessListener {
                callback(true, sessionId)
            }
            .addOnFailureListener {
                callback(false, null)
            }
    }

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
    fun getAttendancePerSession(
        roomId: String,
        sessionId: String,
        onResult: (List<AttendanceRecord>) -> Unit,
        onError: (String) -> Unit
    ) {

        val currentDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())

        val attendanceRef = roomsRef
            .child(roomId)
            .child("sessions")
            .child(sessionId)
            .child("attendance")
            .child(currentDate)

        attendanceRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val attendanceList = mutableListOf<AttendanceRecord>()
                for (attendanceSnap in snapshot.children) {
                    val record = attendanceSnap.getValue(AttendanceRecord::class.java)
                    record?.id = attendanceSnap.key
                    record?.let { attendanceList.add(it) }
                }

                Log.d(
                    "SessionRepository",
                    "Loaded ${attendanceList.size} attendance records for session $sessionId"
                )
                onResult(attendanceList)
            }

            override fun onCancelled(error: DatabaseError) {
                onError(error.message)
            }
        })
    }



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