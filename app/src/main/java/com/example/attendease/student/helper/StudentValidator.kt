package com.example.attendease.student.helper

object StudentValidator {

    fun isValidStudentName(name: String?): Boolean {
        if (name.isNullOrBlank()) return false
        return !name.equals("Unknown Student", ignoreCase = true)
    }
}