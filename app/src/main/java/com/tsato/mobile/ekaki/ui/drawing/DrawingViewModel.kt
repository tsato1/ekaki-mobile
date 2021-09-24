package com.tsato.mobile.ekaki.ui.drawing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.tinder.scarlet.WebSocket
import com.tsato.mobile.ekaki.R
import com.tsato.mobile.ekaki.data.models.*
import com.tsato.mobile.ekaki.data.models.DrawAction.Companion.ACTION_UNDO
import com.tsato.mobile.ekaki.data.remote.ws.DrawingApi
import com.tsato.mobile.ekaki.data.remote.ws.Room
import com.tsato.mobile.ekaki.ui.views.DrawingView
import com.tsato.mobile.ekaki.util.CoroutineTimer
import com.tsato.mobile.ekaki.util.DispatcherProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class DrawingViewModel @Inject constructor(
    private val drawingApi: DrawingApi,
    private val dispatchers: DispatcherProvider,
    private val gson: Gson
) : ViewModel() {

    // directly sending the BaseModel class in the channel is also ok
    // creating events give us the option to pass more data with our BaseModels, but not needed here
    sealed class SocketEvent {
        data class ChatMessageEvent(val data: ChatMessage) : SocketEvent()
        data class AnnouncementEvent(val data: Announcement) : SocketEvent()
        data class GameStateEvent(val data: GameState) : SocketEvent()
        data class DrawDataEvent(val data: DrawData) : SocketEvent()
        data class NewWordsEvent(val data: NewWords) : SocketEvent()
        data class ChosenWordEvent(val data: ChosenWord) : SocketEvent()
        data class GameErrorEvent(val data: GameError) : SocketEvent()
        data class RoundDrawInfoEvent(val data: RoundDrawInfo) : SocketEvent()
        object UndoEvent : SocketEvent()
    }

    // needed to restore the state that was lost upon configuration change
    private val _pathData = MutableStateFlow(Stack<DrawingView.PathData>())
    val pathData: StateFlow<Stack<DrawingView.PathData>> = _pathData

    private val _phase = MutableStateFlow(PhaseChange(null, 0L, null))
    val phase: StateFlow<PhaseChange> = _phase

    private val _phaseTime = MutableStateFlow(0L)
    val phaseTime: StateFlow<Long> = _phaseTime

    private val _gameState = MutableStateFlow(GameState("", ""))
    val gameState: StateFlow<GameState> = _gameState

    private val _newWords = MutableStateFlow(NewWords(listOf()))
    val newWords: StateFlow<NewWords> = _newWords

    private val _chat = MutableStateFlow<List<BaseModel>>(listOf())
    val chat: StateFlow<List<BaseModel>> = _chat

    private val _selectedColorButtonId = MutableStateFlow(R.id.rbBlack)
    val selectedColorButtonId: StateFlow<Int> = _selectedColorButtonId

    private val _connectionProgressBarVisible = MutableStateFlow(true)
    val connectionProgressBarVisible: StateFlow<Boolean> = _connectionProgressBarVisible

    private val _chooseWordOverlayVisible = MutableStateFlow(false)
    val chooseWordOverlayVisible: StateFlow<Boolean> = _chooseWordOverlayVisible

    // this is the channel in which we sends events
    private val connectionEventChannel = Channel<WebSocket.Event>()
    val connectionEvent = connectionEventChannel.receiveAsFlow().flowOn(dispatchers.io)

    // socketEvent here is the event that we get from our server (WebSocketMessage)
    private val socketEventChannel = Channel<SocketEvent>()
    val socketEvent = socketEventChannel.receiveAsFlow().flowOn(dispatchers.io)

    private val coroutineTimer = CoroutineTimer()
    private var timerJob: Job? = null

    init {
        observeBaseModels()
        observeEvents()
    }

    fun cancelTimer() {
        timerJob?.cancel()
    }

    private fun setTimer(duration: Long) {
        timerJob?.cancel()
        timerJob = coroutineTimer.timeAndEmit(duration, viewModelScope) {
            _phaseTime.value = it
        }
    }

    fun setPathData(stack: Stack<DrawingView.PathData>) {
        _pathData.value = stack
    }

    fun setChooseWordOverlayVisibility(isVisible: Boolean) {
        _chooseWordOverlayVisible.value = isVisible
    }

    fun setConnectionProgressBarVisibility(isVisible: Boolean) {
        _connectionProgressBarVisible.value = isVisible
    }

    fun checkRadioButton(id: Int) {
        _selectedColorButtonId.value = id
    }

    // just observe connection events
    private fun observeEvents() {
        viewModelScope.launch(dispatchers.io) {
            drawingApi.observeEvents().collect { event ->
                connectionEventChannel.send(event)
            }
        }
    }

    // hub that observes BaseModels that our server sends
    private fun observeBaseModels() {
        viewModelScope.launch(dispatchers.io) {
            drawingApi.observeBaseModels().collect { data ->
                when (data) {
                    is DrawData -> {
                        socketEventChannel.send(SocketEvent.DrawDataEvent(data))
                    }
                    is ChatMessage -> {
                        socketEventChannel.send(SocketEvent.ChatMessageEvent(data))
                    }
                    is ChosenWord -> {
                        socketEventChannel.send(SocketEvent.ChosenWordEvent(data))
                    }
                    is Announcement -> {
                        socketEventChannel.send(SocketEvent.AnnouncementEvent(data))
                    }
                    is GameState -> {
                        _gameState.value = data
                        socketEventChannel.send(SocketEvent.GameStateEvent(data))
                    }
                    is NewWords -> {
                        _newWords.value = data
                        socketEventChannel.send(SocketEvent.NewWordsEvent(data))
                    }
                    is DrawAction -> {
                        when (data.action) {
                            ACTION_UNDO -> socketEventChannel.send(SocketEvent.UndoEvent)
                        }
                    }
                    is PhaseChange -> {
                        // if data.phase is null, we would lose track of phase upon configuration change
                        data.phase?.let {
                            _phase.value = data
                        }

                        // time cannot be null
                        _phaseTime.value = data.time

                        // WAITING_FOR_PLAYERS is the phase the we don't need timer
                        if (data.phase != Room.Phase.WAITING_FOR_PLAYERS) {
                            setTimer(data.time)
                        }
                    }
                    is Ping -> {
                        sendBaseModel(Ping())
                    }
                    is GameError -> {
                        socketEventChannel.send(SocketEvent.GameErrorEvent(data))
                    }
                }
            }
        }
    }

    fun chooseWord(word: String, roomName: String) {
        val chosenWord = ChosenWord(word, roomName)
        sendBaseModel(chosenWord)
    }

    fun sendChatMessage(message: ChatMessage) {
        if (message.message.trim().isEmpty())
            return

        viewModelScope.launch(dispatchers.io) {
            drawingApi.sendBaseModel(message)
        }
    }

    fun sendBaseModel(data: BaseModel) {
        viewModelScope.launch(dispatchers.io) {
            drawingApi.sendBaseModel(data)
        }
    }
}