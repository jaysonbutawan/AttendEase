package com.example.attendease.teacher.data.repositories

import android.util.Log
import com.example.attendease.teacher.data.model.AttendanceRecord
import com.example.attendease.teacher.data.model.ClassSession
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SessionRepository {

    private val roomsRef = FirebaseDatabase.getInstance().getReference("rooms")
    private val currentTeacherId: String? get() = FirebaseAuth.getInstance().currentUser?.uid

    // -------------------------------------------------------------------------
    //  CRUD OPERATIONS
    // -------------------------------------------------------------------------

    /**
     * Creates a new session under the correct room and generates a unique ID.
     */
    fun createSession(session: ClassSession, callback: (Boolean, String?) -> Unit) {
        val sessionsRootRef = roomsRef
            .child(session.roomId ?: "unknown")
            .child("sessions")

        val newSessionRef = sessionsRootRef.push()
        val sessionId = newSessionRef.key

        if (sessionId == null) {
            callback(false, null)
            return
        }

        val sessionWithId = session.copy(sessionId = sessionId)

        newSessionRef.setValue(sessionWithId)
            .addOnSuccessListener {
                Log.d("SessionRepository", "✅ Successfully created session $sessionId")
                callback(true, sessionId)
            }
            .addOnFailureListener {
                Log.e("SessionRepository", "❌ Failed to create session: ${it.message}")
                callback(false, null)
            }
    }

    /**
     * Updates the QR code field in a specific session node.
     */
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
            .addOnSuccessListener {
                Log.d("SessionRepository", "✅ QR Code updated for session $sessionId")
            }
            .addOnFailureListener {
                Log.e("SessionRepository", "❌ Failed to update QR code: ${it.message}")
            }
    }

    // -------------------------------------------------------------------------
    //  FETCH OPERATIONS
    // -------------------------------------------------------------------------

    /**
     * Fetches ALL sessions created by the currently logged-in teacher.
     */
    fun getSessions(
        onResult: (List<ClassSession>) -> Unit,
        onError: (String) -> Unit
    ) {
        val teacherId = currentTeacherId ?: return onError("No authenticated user ID found.")

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
                            if (it.teacherId == teacherId) {
                                sessionList.add(it)
                                Log.d("SessionRepository", "Loaded session: ${it.subject} in Room: $roomName")
                                Log.d("SessionRepository","Session${session.sessionStatus}")

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

    /**
     * Fetches attendance records for a specific session for the CURRENT DATE.
     */
    fun getAttendancePerSession(
        roomId: String,
        sessionId: String,
        onResult: (List<AttendanceRecord>) -> Unit,
        onError: (String) -> Unit
    ) {
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val attendanceRef = roomsRef
            .child(roomId)
            .child("sessions")
            .child(sessionId)
            .child("attendance")
            .child(currentDate)

        attendanceRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("SessionRepository", "Snapshot exists: ${snapshot.exists()}, children count: ${snapshot.childrenCount}")

                val attendanceList = mutableListOf<AttendanceRecord>()
                for (attendanceSnap in snapshot.children) {
                    Log.d("SessionRepository", "Raw snapshot key=${attendanceSnap.key}, value=${attendanceSnap.value}")

                    val record = attendanceSnap.getValue(AttendanceRecord::class.java)
                    if (record == null) {
                        Log.w("SessionRepository", "⚠️ Failed to parse record for key=${attendanceSnap.key}")
                    } else {
                        record.id = attendanceSnap.key
                        attendanceList.add(record)

                        // Log all fields, even if some are null
                        Log.d(
                            "SessionRepository",
                            """
                        📄 Attendance Record:
                        ├─ ID: ${record.id}
                        ├─ Name: ${record.name ?: "NULL"}
                        ├─ Status: ${record.status ?: "NULL"}
                        ├─ Confidence: ${record.confidence ?: "NULL"}
                        ├─ Time Scanned: ${record.timeScanned ?: "NULL"}
                        ├─ Late Duration: ${record.lateDuration ?: "NULL"}
                        ├─ Total Outside Time: ${record.totalOutsideTime ?: "NULL"}
                        └─ Outside Time Display: ${record.outsideTimeDisplay ?: "NULL"}
                        """.trimIndent()
                        )
                    }
                }

                Log.d(
                    "SessionRepository",
                    "✅ Loaded ${attendanceList.size} attendance records for session $sessionId on $currentDate"
                )
                onResult(attendanceList)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("SessionRepository", "Firebase cancelled: ${error.message}")
                onError(error.message)
            }
        })
    }



    /**
     * Fetches a list of all historical class instances (sessions identified by date)
     * where attendance was recorded for the logged-in teacher's classes.
     *
     * @NOTE: This function was corrected to ensure it only fetches sessions belonging
     * to the current teacher.
     */
    fun getSessionByDate(
        onResult: (List<ClassSession>) -> Unit,
        onError: (String) -> Unit
    ) {
        val teacherId = currentTeacherId ?: return onError("No authenticated user ID found.")
        val sessionList = mutableListOf<ClassSession>()

        roomsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (roomSnapshot in snapshot.children) {
                    val roomId = roomSnapshot.key ?: continue
                    val roomName = roomSnapshot.child("name").getValue(String::class.java) ?: "Unknown Room"

                    val sessionsSnapshot = roomSnapshot.child("sessions")
                    for (sessionSnapshot in sessionsSnapshot.children) {
                        val sessionId = sessionSnapshot.key ?: continue
                        val session = sessionSnapshot.getValue(ClassSession::class.java) ?: ClassSession()
                        session.roomId = roomId
                        session.roomName = roomName
                        session.sessionId = sessionId

                        // ✅ CORRECTION: Filter to only include sessions taught by the current teacher
                        if (session.teacherId != teacherId) continue

                        // Now iterate through recorded attendance dates
                        val attendanceSnapshot = sessionSnapshot.child("attendance")
                        if (attendanceSnapshot.exists()) {
                            for (dateSnapshot in attendanceSnapshot.children) {
                                val dateKey = dateSnapshot.key ?: continue

                                // Create a copy representing the historical class instance
                                val dateSession = session.copy(date = dateKey)
                                sessionList.add(dateSession)

                                Log.d("SessionRepository", "✅ Found session date: $dateKey in $roomName")
                            }
                        }
                    }
                }

                Log.d("SessionRepository", "📘 Total unique class instances fetched: ${sessionList.size}")
                onResult(sessionList)
            }

            override fun onCancelled(error: DatabaseError) {
                onError(error.message)
            }
        })
    }

    /**
     * Fetches all attendance records grouped by date for a specific session.
     */
    fun getAttendanceByDate(
        roomId: String,
        sessionId: String,
        date: String,
        onResult: (List<AttendanceRecord>) -> Unit,
        onError: (String) -> Unit
    ) {
        val attendanceRef = roomsRef
            .child(roomId)
            .child("sessions")
            .child(sessionId)
            .child("attendance")
            .child(date) // ✅ Use the date clicked from the UI

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
                    "✅ Loaded ${attendanceList.size} attendance records for $sessionId on $date"
                )
                onResult(attendanceList)
            }

            override fun onCancelled(error: DatabaseError) {
                onError(error.message)
            }
        })
    }

}