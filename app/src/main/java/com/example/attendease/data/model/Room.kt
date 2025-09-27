package com.example.attendease.data.model


data class Room(
    val roomId: String? = null,
    val name: String? = null,
    val boundary: List<Coordinate>? = null
){
    override fun toString(): String {
        return name ?: "Unnamed Room"
    }
}

data class Coordinate(
    val latitude: Double? = null,
    val longitude: Double? = null
)
