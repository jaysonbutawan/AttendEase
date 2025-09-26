package com.example.attendease.ui.roomlist


import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.attendease.data.model.Room
import com.example.attendease.data.repositories.RoomRepository

class RoomListViewModel : ViewModel() {

    private val repository = RoomRepository()

    private val _rooms = MutableLiveData<List<Room>>()
    val rooms: LiveData<List<Room>> get() = _rooms

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> get() = _error

    fun loadRooms() {
        repository.getRooms(
            onResult = { roomList -> _rooms.value = roomList },
            onError = { errorMsg -> _error.value = errorMsg }
        )
    }
}
