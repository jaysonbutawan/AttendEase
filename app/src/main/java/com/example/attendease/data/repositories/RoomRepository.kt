package com.example.attendease.data.repositories

import com.example.attendease.data.model.Room
import com.google.firebase.database.*

class RoomRepository {

    private val database: DatabaseReference =
        FirebaseDatabase.getInstance().getReference("rooms")

    fun getRooms(onResult: (List<Room>) -> Unit, onError: (String) -> Unit) {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val roomList = mutableListOf<Room>()
                for (roomSnapshot in snapshot.children) {
                    val room = roomSnapshot.getValue(Room::class.java)
                    room?.let { roomList.add(it) }
                }
                onResult(roomList)
            }

            override fun onCancelled(error: DatabaseError) {
                onError(error.message)
            }
        })
    }

}
