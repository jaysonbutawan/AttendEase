package com.example.attendease.student.helper

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.attendease.student.data.AttendanceStatus
import com.example.attendease.student.data.Session
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object SessionHelper {

    private val database = FirebaseDatabase.getInstance().reference
    private val currentUser = FirebaseAuth.getInstance().currentUser
    private const val TAG = "SESSION_HELPER"

    suspend fun getMatchedSessions(): List<Session> = withContext(Dispatchers.IO) {
        val userId = currentUser?.uid ?: return@withContext emptyList()
        Log.d(TAG, "👤 Current Firebase UID: $userId")

        try {
            val userScheduleRef = database.child("users").child(userId).child("schedule")
            val roomsRef = database.child("rooms")

            val userSnapshot = userScheduleRef.get().await()
            if (!userSnapshot.exists()) return@withContext emptyList()

            // 🔹 Load the student's schedule data
            val studentSchedule = userSnapshot.children.mapNotNull {
                val subject = it.child("subject").getValue(String::class.java)
                val time = it.child("time").getValue(String::class.java)
                val instructor = it.child("instructor").getValue(String::class.java)
                val room = it.child("room").getValue(String::class.java)
                if (subject != null && time != null && instructor != null && room != null)
                    mapOf("subject" to subject, "time" to time, "instructor" to instructor, "room" to room)
                else null
            }

            val roomsSnapshot = roomsRef.get().await()
            val matchedSessions = mutableListOf<Session>()

            for (roomSnap in roomsSnapshot.children) {
                val roomKey = roomSnap.key ?: continue // ✅ Firebase room ID
                val roomName = roomSnap.child("name").getValue(String::class.java) ?: continue
                val sessionsNode = roomSnap.child("sessions")

                for (sessionSnap in sessionsNode.children) {
                    val sessionSubject = sessionSnap.child("subject").getValue(String::class.java)
                    val teacherId = sessionSnap.child("teacherId").getValue(String::class.java)
                    val startTime = sessionSnap.child("startTime").getValue(String::class.java)
                    val endTime = sessionSnap.child("endTime").getValue(String::class.java)
                    val sessionStatus = sessionSnap.child("sessionStatus").getValue(String::class.java)
                    val sessionId = sessionSnap.key ?: continue

                    // ✅ Get teacher name
                    val teacherSnap = database.child("users").child(teacherId ?: "").get().await()
                    val instructorName = teacherSnap.child("fullname").getValue(String::class.java) ?: continue

                    val sessionFullTime = "$startTime - $endTime"

                    // ✅ Check if student's schedule matches this session
                    val match = studentSchedule.any { entry ->
                        entry["subject"].equals(sessionSubject, true) &&
                                entry["instructor"].equals(instructorName, true) &&
                                entry["room"].equals(roomName, true) &&
                                entry["time"].equals(sessionFullTime, true)
                    }

                    if (match) {
                        val status = when (sessionStatus) {
                            "started" -> "Live"
                            "ended" -> "Ended"
                            else -> "Upcoming"
                        }

                        // ✅ Add the session with roomId
                        matchedSessions.add(
                            Session(
                                sessionId = sessionId,
                                subject = sessionSubject ?: "",
                                instructor = instructorName,
                                startTime = startTime ?: "",
                                endTime = endTime ?: "",
                                room = roomName,
                                roomId = roomKey, // ✅ actual Firebase key
                                status = status
                            )
                        )

                        Log.d(TAG, "✅ Matched: $sessionSubject in $roomName ($roomKey)")
                    }
                }
            }

            Log.d(TAG, "✅ Found ${matchedSessions.size} matched sessions")
            return@withContext matchedSessions

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching matched sessions: ${e.message}", e)
            return@withContext emptyList()
        }
    }

    suspend fun getSessionsWithAttendance(): List<Session> = withContext(Dispatchers.IO) {
        val userId = currentUser?.uid ?: return@withContext emptyList()
        val sessionsWithAttendance = mutableListOf<Session>()

        try {
            Log.d(TAG, "📡 Fetching sessions with attendance for user: $userId")

            val roomsSnapshot = database.child("rooms").get().await()
            for (roomSnap in roomsSnapshot.children) {
                val roomId = roomSnap.key ?: continue
                val roomName = roomSnap.child("name").getValue(String::class.java) ?: continue
                val sessionsNode = roomSnap.child("sessions")

                for (sessionSnap in sessionsNode.children) {
                    val sessionId = sessionSnap.key ?: continue
                    val sessionSubject = sessionSnap.child("subject").getValue(String::class.java) ?: "Unknown"
                    val teacherId = sessionSnap.child("teacherId").getValue(String::class.java)
                    val startTime = sessionSnap.child("startTime").getValue(String::class.java) ?: ""
                    val endTime = sessionSnap.child("endTime").getValue(String::class.java) ?: ""
                    val sessionStatus = sessionSnap.child("sessionStatus").getValue(String::class.java) ?: "upcoming"

                    // Check if the user has attendance for this session
                    val attendanceNode = sessionSnap.child("attendance")
                    val hasAttendance = attendanceNode.children.any { dateSnap ->
                        dateSnap.child(userId).exists()
                    }

                    if (hasAttendance) {
                        // Get teacher name
                        val instructorName = if (!teacherId.isNullOrEmpty()) {
                            database.child("users").child(teacherId).child("fullname").get().await().getValue(String::class.java) ?: "Unknown"
                        } else "Unknown"

                        val status = when (sessionStatus.lowercase()) {
                            "started" -> "Live"
                            "ended" -> "Ended"
                            else -> "Upcoming"
                        }

                        sessionsWithAttendance.add(
                            Session(
                                sessionId = sessionId,
                                subject = sessionSubject,
                                instructor = instructorName,
                                startTime = startTime,
                                endTime = endTime,
                                room = roomName,
                                roomId = roomId,
                                status = status,
                                attendance = getStudentAttendance(roomId, sessionId) // attach attendance
                            )
                        )

                        Log.d(TAG, "✅ Added session with attendance: $sessionSubject in $roomName ($sessionId)")
                    }
                }
            }

            Log.d(TAG, "✅ Total sessions with attendance: ${sessionsWithAttendance.size}")
            return@withContext sessionsWithAttendance

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching sessions with attendance: ${e.message}", e)
            return@withContext emptyList()
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getStudentAttendance(
        roomId: String,
        sessionId: String
    ): List<AttendanceStatus> = withContext(Dispatchers.IO) {
        val userId = currentUser?.uid ?: return@withContext emptyList()
        val attendanceList = mutableListOf<AttendanceStatus>()

        try {
            Log.d(TAG, "📡 Fetching attendance for user: $userId, room: $roomId, session: $sessionId")

            // ✅ Reference to the specific session
            val sessionRef = database
                .child("rooms")
                .child(roomId)
                .child("sessions")
                .child(sessionId)

            // ✅ Fetch the session details (to get subject name, etc.)
            val sessionSnapshot = sessionRef.get().await()
            if (!sessionSnapshot.exists()) {
                Log.w(TAG, "⚠️ Session not found for ID: $sessionId in room: $roomId")
                return@withContext emptyList()
            }

            val subject = sessionSnapshot.child("subject").getValue(String::class.java) ?: "Unknown Subject"
            val attendanceRef = sessionRef.child("attendance")

            // ✅ Fetch attendance data
            val attendanceSnapshot = attendanceRef.get().await()
            if (!attendanceSnapshot.exists()) {
                Log.w(TAG, "⚠️ No attendance data found for session: $sessionId ($subject)")
                return@withContext emptyList()
            }

            for (dateSnap in attendanceSnapshot.children) {
                val dateStr = dateSnap.key ?: continue
                val studentSnap = dateSnap.child(userId)

                if (studentSnap.exists()) {
                    val status = studentSnap.child("status").getValue(String::class.java) ?: "Unknown"

                    // Convert "2025-10-30" → "October, 30, 2025"
                    val formattedDate = try {
                        val parsedDate = LocalDate.parse(dateStr, DateTimeFormatter.ISO_DATE)
                        parsedDate.format(DateTimeFormatter.ofPattern("MMMM, dd, yyyy", Locale.ENGLISH))
                    } catch (e: Exception) {
                        dateStr // fallback if parsing fails
                    }

                    attendanceList.add(
                        AttendanceStatus(
                            timeText = formattedDate,
                            statusText = "${status.replaceFirstChar { it.uppercase() }}"
                        )
                    )

                    Log.d(TAG, "✅ Found record for $subject on $formattedDate → $status")
                }
            }

            if (attendanceList.isEmpty()) {
                Log.w(TAG, "⚠️ No attendance records found for $subject (user: $userId, session: $sessionId)")
            } else {
                Log.d(TAG, "✅ Loaded ${attendanceList.size} attendance records for $subject")
            }

            return@withContext attendanceList

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching attendance: ${e.message}", e)
            return@withContext emptyList()
        }
    }


}
