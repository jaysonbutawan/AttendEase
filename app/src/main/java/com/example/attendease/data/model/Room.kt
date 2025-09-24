package com.example.attendease.data.model

data class Room(
    val name: String? = null,
    val building: String? = null,
    val boundary: List<Coordinate>? = null   
)

data class Coordinate(
    val latitude: Double? = null,
    val longitude: Double? = null
)
