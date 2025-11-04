package com.example.attendease.student.helper

import android.os.Build
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
                val roomKey = roomSnap.key ?: continue
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
                        entry["subject"]?.normalizeForMatch() == sessionSubject?.normalizeForMatch() &&
                                entry["instructor"]?.normalizeForMatch() == instructorName.normalizeForMatch() &&
                                entry["room"]?.normalizeForMatch() == roomName.normalizeForMatch() &&
                                entry["time"]?.normalizeForMatch() == sessionFullTime.normalizeForMatch()
                    }


                    if (match) {
                        val status = when (sessionStatus) {
                            "started" -> "Live"
                            "ended" -> "Ended"
                            else -> "Upcoming"
                        }

                        matchedSessions.add(
                            Session(
                                sessionId = sessionId,
                                subject = sessionSubject ?: "",
                                instructor = instructorName,
                                startTime = startTime ?: "",
                                endTime = endTime ?: "",
                                room = roomName,
                                roomId = roomKey,
                                status = status
                            )
                        )

                    }
                }
            }

            return@withContext matchedSessions

        } catch (e: Exception) {
            return@withContext emptyList()
        }
    }
    fun String.normalizeForMatch(): String =
        this.trim().replace("\\s+".toRegex(), " ").lowercase()


    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun getSessionsWithAttendance(): List<Session> = withContext(Dispatchers.IO) {
        val userId = currentUser?.uid ?: return@withContext emptyList()
        val sessionsWithAttendance = mutableListOf<Session>()

        try {

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

                    val attendanceNode = sessionSnap.child("attendance")
                    val hasAttendance = attendanceNode.children.any { dateSnap ->
                        dateSnap.child(userId).exists()
                    }

                    if (hasAttendance) {
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
                                attendance = getStudentAttendance(roomId, sessionId)
                            )
                        )

                    }
                }
            }

            return@withContext sessionsWithAttendance

        } catch (e: Exception) {
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
            val sessionRef = database
                .child("rooms")
                .child(roomId)
                .child("sessions")
                .child(sessionId)
            val sessionSnapshot = sessionRef.get().await()
            if (!sessionSnapshot.exists()) {
                return@withContext emptyList()
            }

            sessionSnapshot.child("subject").getValue(String::class.java) ?: "Unknown Subject"
            val attendanceRef = sessionRef.child("attendance")

            val attendanceSnapshot = attendanceRef.get().await()
            if (!attendanceSnapshot.exists()) {
                return@withContext emptyList()
            }

            for (dateSnap in attendanceSnapshot.children) {
                val dateStr = dateSnap.key ?: continue
                val studentSnap = dateSnap.child(userId)

                if (studentSnap.exists()) {
                    val status = studentSnap.child("status").getValue(String::class.java) ?: "Unknown"
                    val formattedDate = try {
                        val parsedDate = LocalDate.parse(dateStr, DateTimeFormatter.ISO_DATE)
                        parsedDate.format(DateTimeFormatter.ofPattern("MMMM, dd, yyyy", Locale.ENGLISH))
                    } catch (e: Exception) {
                        dateStr
                    }

                    attendanceList.add(
                        AttendanceStatus(
                            timeText = formattedDate,
                            statusText = "${status.replaceFirstChar { it.uppercase() }}"
                        )
                    )
                }
            }

            return@withContext attendanceList

        } catch (e: Exception) {
            return@withContext emptyList()
        }
    }


}
