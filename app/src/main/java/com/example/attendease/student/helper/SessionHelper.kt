package com.example.attendease.student.helper

import android.util.Log
import com.example.attendease.student.data.Session
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object SessionHelper {

    private val database = FirebaseDatabase.getInstance().reference
    private val currentUser = FirebaseAuth.getInstance().currentUser
    private const val TAG = "SESSION_HELPER"

    suspend fun getMatchedSessions(): List<Session> = withContext(Dispatchers.IO) {
        val userId = currentUser?.uid ?: return@withContext emptyList()

        try {
            val userScheduleRef = database.child("users").child(userId).child("schedule")
            val roomsRef = database.child("rooms")

            val userSnapshot = userScheduleRef.get().await()
            if (!userSnapshot.exists()) return@withContext emptyList()

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
                val roomName = roomSnap.child("name").getValue(String::class.java) ?: continue
                val sessionsNode = roomSnap.child("sessions")

                for (sessionSnap in sessionsNode.children) {
                    val sessionSubject = sessionSnap.child("subject").getValue(String::class.java)
                    val teacherId = sessionSnap.child("teacherId").getValue(String::class.java)
                    val startTime = sessionSnap.child("startTime").getValue(String::class.java)
                    val endTime = sessionSnap.child("endTime").getValue(String::class.java)
                    val sessionStatus = sessionSnap.child("sessionStatus").getValue(String::class.java)
                    val sessionId = sessionSnap.key ?: continue

                    val teacherSnap = database.child("users").child(teacherId ?: "").get().await()
                    val instructorName = teacherSnap.child("fullname").getValue(String::class.java) ?: continue

                    val sessionFullTime = "$startTime - $endTime"

                    val match = studentSchedule.any { entry ->
                        entry["subject"].equals(sessionSubject, true) &&
                                entry["instructor"].equals(instructorName, true) &&
                                entry["room"].equals(roomName, true) &&
                                entry["time"].equals(sessionFullTime, true)
                    }

                    if (match) {
                        val status = if (sessionStatus == "started") "Live" else "Upcoming"
                        matchedSessions.add(
                            Session(
                                sessionId = sessionId,
                                subject = sessionSubject ?: "",
                                instructor = instructorName,
                                startTime = startTime ?: "",
                                endTime = endTime ?: "",
                                room = roomName,
                                status = status
                            )
                        )
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
}
