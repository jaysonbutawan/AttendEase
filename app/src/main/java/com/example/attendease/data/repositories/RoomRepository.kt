package com.example.attendease.data.repositories

import com.example.attendease.data.model.Room
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class RoomRepository {

    private val database: DatabaseReference =
        FirebaseDatabase.getInstance().getReference("rooms")

    fun getRooms(onResult: (List<Room>) -> Unit, onError: (String) -> Unit) {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val roomList = mutableListOf<Room>()
                for (roomSnapshot in snapshot.children) {
                    val room = roomSnapshot.getValue(Room::class.java)
                    room?.let {
                        // inject the Firebase key as roomId
                        val roomWithId = it.copy(roomId = roomSnapshot.key)
                        roomList.add(roomWithId)
                    }
                }
                onResult(roomList)
            }

            override fun onCancelled(error: DatabaseError) {
                onError(error.message)
            }
        })
    }
}
