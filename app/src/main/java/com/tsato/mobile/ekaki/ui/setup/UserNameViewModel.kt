package com.tsato.mobile.ekaki.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tsato.mobile.ekaki.repository.SetupRepository
import com.tsato.mobile.ekaki.util.Constants.MAX_USERNAME_LENGTH
import com.tsato.mobile.ekaki.util.Constants.MIN_USERNAME_LENGTH
import com.tsato.mobile.ekaki.util.DispatcherProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserNameViewModel @Inject constructor(
    private val setupRepository: SetupRepository,
    private val dispatcher: DispatcherProvider
): ViewModel() {

    sealed class SetupEvent {
        object InputEmptyError : SetupEvent()
        object InputTooShortError : SetupEvent()
        object InputTooLongError : SetupEvent()

        data class NavigateToSelectRoomEvent(val userName: String) : SetupEvent()
//        data class NavigateToSelectRoomErrorEvent(val error: String) : SetupEvent() // no need
    }

    private val _setupEvent = MutableSharedFlow<SetupEvent>()
    val setupEvent: SharedFlow<SetupEvent> = _setupEvent

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

}