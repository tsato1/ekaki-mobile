package com.tsato.mobile.ekaki.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tsato.mobile.ekaki.data.remote.ws.Room
import com.tsato.mobile.ekaki.repository.SetupRepository
import com.tsato.mobile.ekaki.util.Constants.MAX_ROOMNAME_LENGTH
import com.tsato.mobile.ekaki.util.Constants.MAX_USERNAME_LENGTH
import com.tsato.mobile.ekaki.util.Constants.MIN_ROOMNAME_LENGTH
import com.tsato.mobile.ekaki.util.Constants.MIN_USERNAME_LENGTH
import com.tsato.mobile.ekaki.util.DispatcherProvider
import com.tsato.mobile.ekaki.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val setupRepository: SetupRepository,
    private val dispatcher: DispatcherProvider
): ViewModel() {

    sealed class SetupEvent {
        object InputEmptyError : SetupEvent()
        object InputTooShortError : SetupEvent()
        object InputTooLongError : SetupEvent()

        data class CreateRoomEvent(val room: Room) : SetupEvent()
        data class CreateRoomErrorEvent(val error: String) : SetupEvent()

        data class NavigateToSelectRoomEvent(val userName: String) : SetupEvent()
        data class NavigateToSelectRoomErrorEvent(val error: String) : SetupEvent()

        data class GetRoomEvent(val rooms: List<Room>) : SetupEvent()
        data class GetRoomErrorEvent(val error: String) : SetupEvent()
        object GetRoomLoadingEvent : SetupEvent()
        object GetRoomEmptyEvent : SetupEvent() // initial event in our state flow; we don't have to crete null


        data class JoinRoomEvent(val roomName: String) : SetupEvent()
        data class JoinRoomErrorEvent(val error: String) : SetupEvent()
    }

    /*
     kotlin channel vs flow:
     channel should be used when there is a single observer of channels
     flow should be used when multiple observers need to receive events that we send into the flow
     */

    private val _setupEvent = MutableSharedFlow<SetupEvent>()
    val setupEvent: SharedFlow<SetupEvent> = _setupEvent

    private val _rooms = MutableStateFlow<SetupEvent>(SetupEvent.GetRoomEmptyEvent)
    val rooms: StateFlow<SetupEvent> = _rooms

    fun validateUserNameAndNavigateToSelectRoom(userName: String) {
        viewModelScope.launch(dispatcher.main) {
            val trimmedUserName = userName.trim()
            when {
                trimmedUserName.isEmpty() -> {
                    _setupEvent.emit(SetupEvent.InputEmptyError)
                }
                trimmedUserName.length < MIN_USERNAME_LENGTH -> {
                    _setupEvent.emit(SetupEvent.InputTooShortError)
                }
                trimmedUserName.length > MAX_USERNAME_LENGTH -> {
                    _setupEvent.emit(SetupEvent.InputTooLongError)
                }
                else -> {
                    _setupEvent.emit(SetupEvent.NavigateToSelectRoomEvent(trimmedUserName))
                }
            }
        }
    }

    fun createRoom(room: Room) {
        viewModelScope.launch(dispatcher.main) {
            val trimmedRoomName = room.name.trim()
            when {
                trimmedRoomName.isEmpty() -> {
                    _setupEvent.emit(SetupEvent.InputEmptyError)
                }
                trimmedRoomName.length < MIN_ROOMNAME_LENGTH -> {
                    _setupEvent.emit(SetupEvent.InputTooShortError)
                }
                trimmedRoomName.length > MAX_ROOMNAME_LENGTH -> {
                    _setupEvent.emit(SetupEvent.InputTooLongError)
                }
                else -> {
                    val result = setupRepository.createRoom(room)
                    if (result is Resource.Success) {
                        _setupEvent.emit(SetupEvent.CreateRoomEvent(room))
                    }
                    else {
                        _setupEvent.emit(SetupEvent.CreateRoomErrorEvent(
                            result.message ?: return@launch)
                        )
                    }
                }
            }
        }
    }

    fun getRooms(searchQuery: String) {
        _rooms.value = SetupEvent.GetRoomLoadingEvent
        viewModelScope.launch(dispatcher.main) {
            val result = setupRepository.getRooms(searchQuery)
            if (result is Resource.Success) {
                _rooms.value = SetupEvent.GetRoomEvent(result.data ?: return@launch)
            }
            else {
                _setupEvent.emit(SetupEvent.GetRoomErrorEvent(result.message ?: return@launch))
            }
        }
    }

    fun joinRoom(userName: String, roomName: String) {
        _rooms.value = SetupEvent.GetRoomLoadingEvent
        viewModelScope.launch(dispatcher.main) {
            val result = setupRepository.joinRoom(userName, roomName)
            if (result is Resource.Success) {
                _setupEvent.emit(SetupEvent.JoinRoomEvent(roomName))
            }
            else {
                _setupEvent.emit(SetupEvent.JoinRoomErrorEvent(result.message ?: return@launch))
            }
        }
    }
}