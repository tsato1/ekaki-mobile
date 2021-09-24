package com.tsato.mobile.ekaki.ui.drawing

import android.graphics.Color
import android.os.Bundle
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.*
import androidx.navigation.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.tinder.scarlet.WebSocket
import com.tsato.mobile.ekaki.R
import com.tsato.mobile.ekaki.adapters.ChatMessageAdapter
import com.tsato.mobile.ekaki.adapters.PlayerAdapter
import com.tsato.mobile.ekaki.data.models.*
import com.tsato.mobile.ekaki.data.remote.ws.Room
import com.tsato.mobile.ekaki.databinding.ActivityDrawingBinding
import com.tsato.mobile.ekaki.util.Constants
import com.tsato.mobile.ekaki.util.hideKeyboard
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DrawingActivity : AppCompatActivity(), LifecycleObserver {

    private lateinit var binding: ActivityDrawingBinding

    private val viewModel: DrawingViewModel by viewModels()

    private val args: DrawingActivityArgs by navArgs()

    @Inject
    lateinit var clientId: String // from provideClientId() in AppModule.kt

    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var rvPlayers: RecyclerView
    @Inject
    lateinit var playerAdapter: PlayerAdapter

    private lateinit var chatMessageAdapter: ChatMessageAdapter

    private var updateChatJob: Job? = null // there is only one job that updates our RecyclerView
    private var updatePlayersJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDrawingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this) // 'this' is LifecycleObserver interface
        subscribeToUiStateUpdates()
        listenToConnectionEvents()
        listenToSocketEvents()
        setupRecyclerView()

        toggle = ActionBarDrawerToggle(this, binding.root, R.string.open, R.string.close)
        toggle.syncState()

        binding.drawingView.roomName = args.roomName

        chatMessageAdapter.stateRestorationPolicy =
            RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY // restored at onPause()

        val header = layoutInflater.inflate(R.layout.nav_drawer_header, binding.navView)
        rvPlayers = header.findViewById(R.id.rvPlayers)
        binding.root.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED) // nav drawer closed

        rvPlayers.apply {
            adapter = playerAdapter
            layoutManager = LinearLayoutManager(this@DrawingActivity)
        }

        binding.ibPlayers.setOnClickListener {
            binding.root.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
            binding.root.openDrawer(GravityCompat.START)
        }

        binding.root.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) = Unit

            override fun onDrawerOpened(drawerView: View) = Unit

            override fun onDrawerClosed(drawerView: View) {
                binding.root.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            }

            override fun onDrawerStateChanged(newState: Int) = Unit
        })

        binding.ibClearText.setOnClickListener {
            binding.etMessage.text?.clear()
        }

        binding.ibSend.setOnClickListener {
            viewModel.sendChatMessage(
                ChatMessage(
                    args.userName,
                    args.roomName,
                    binding.etMessage.text.toString(),
                    System.currentTimeMillis())
            )
            binding.etMessage.text?.clear()
            hideKeyboard(binding.root)
        }

        binding.ibUndo.setOnClickListener {
            if (binding.drawingView.isUserDrawing) {
                binding.drawingView.undo()
                viewModel.sendBaseModel(DrawAction(DrawAction.ACTION_UNDO))
            }
        }

        binding.drawingView.setPathDataChangedListener {
            viewModel.setPathData(it)
        }

        binding.colorGroup.setOnCheckedChangeListener { _, checkedId ->
            viewModel.checkRadioButton(checkedId)
        }

        binding.drawingView.setOnDrawListener {
            if (binding.drawingView.isUserDrawing) { // send DrawData only if user is drawing
                viewModel.sendBaseModel(it)
            }
        }
    }

    private fun setColorGroupVisibility(isVisible: Boolean) {
        binding.colorGroup.isVisible = isVisible
        binding.ibUndo.isVisible = isVisible
    }

    private fun setMessageInputVisibility(isVisible: Boolean) {
        binding.apply {
            tilMessage.isVisible = isVisible
            ibSend.isVisible = isVisible
            ibClearText.isVisible = isVisible
        }
    }

    private fun selectColor(color: Int) {
        binding.drawingView.setColor(color)
        binding.drawingView.setThickness(Constants.DEFAULT_PAINT_THICKNESS)
    }

    // collect(observe) state flows
    private fun subscribeToUiStateUpdates() {
        lifecycleScope.launchWhenStarted {
            viewModel.chat.collect { chat ->
                if (chatMessageAdapter.chatObjects.isEmpty()) {
                    updateChatMessageList(chat)
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.newWords.collect {
                val newWords = it.newWords

                if (newWords.isEmpty()) // initial emission
                    return@collect

                binding.apply {
                    btnFirstWord.text = newWords[0]
                    btnSecondWord.text = newWords[1]
                    btnThirdWord.text = newWords[2]
                    btnFirstWord.setOnClickListener {
                        viewModel.chooseWord(newWords[0], args.roomName)
                        viewModel.setChooseWordOverlayVisibility(false)
                    }
                    btnSecondWord.setOnClickListener {
                        viewModel.chooseWord(newWords[1], args.roomName)
                        viewModel.setChooseWordOverlayVisibility(false)
                    }
                    btnThirdWord.setOnClickListener {
                        viewModel.chooseWord(newWords[2], args.roomName)
                        viewModel.setChooseWordOverlayVisibility(false)
                    }
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.gameState.collect { gameState ->
                binding.apply {
                    tvCurWord.text = gameState.word // plain text for the drawing player or the underscored word for the others

                    val isUserDrawing = gameState.drawingPlayer == args.userName
                    setColorGroupVisibility(isUserDrawing) // if drawing, show color groups
                    setMessageInputVisibility(!isUserDrawing) // if not drawing, show message box
                    drawingView.isUserDrawing = isUserDrawing
                    ibUndo.isEnabled = isUserDrawing
                    ibMic.isVisible = !isUserDrawing
                    drawingView.isEnabled = isUserDrawing
                }
            }
        }

        lifecycleScope.launchWhenStarted { // when we receive new players from the server, update
            viewModel.players.collect { players ->
                updatePlayersList(players)
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.phaseTime.collect { time ->
                binding.roundTimerProgressBar.progress = time.toInt()
                binding.tvRemainingTimeChooseWord.text = (time / 1000L).toString()
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.phase.collect { phaseChange ->
                when (phaseChange.phase) {
                    Room.Phase.WAITING_FOR_PLAYERS -> {
                        binding.tvCurWord.text = getString(R.string.waiting_for_players)
                        viewModel.cancelTimer()
                        viewModel.setConnectionProgressBarVisibility(false)
                        binding.roundTimerProgressBar.progress = binding.roundTimerProgressBar.max
                    }
                    Room.Phase.WAITING_FOR_START -> {
                        binding.roundTimerProgressBar.progress = phaseChange.time.toInt()
                        binding.tvCurWord.text = getString(R.string.waiting_for_start)
                    }
                    Room.Phase.NEW_ROUND -> {
                        phaseChange.drawingPlayer?.let { drawingPlayer ->
                            binding.tvCurWord.text = getString(R.string.player_is_drawing, drawingPlayer)
                        }
                        binding.apply {
                            drawingView.isEnabled = false // nobody should be able to draw
                            drawingView.setColor(Color.BLACK)
                            drawingView.setThickness(Constants.DEFAULT_PAINT_THICKNESS)
                            roundTimerProgressBar.max = phaseChange.time.toInt()

                            val isPlayerDrawer = phaseChange.drawingPlayer == args.userName
                            binding.chooseWordOverlay.isVisible = isPlayerDrawer
                        }
                    }
                    Room.Phase.GAME_RUNNING -> {
                        binding.chooseWordOverlay.isVisible = false
                        binding.roundTimerProgressBar.max = phaseChange.time.toInt()
                    }
                    Room.Phase.AFTER_GAME -> {
                        binding.apply {
                            if (drawingView.isDrawing) {
                                drawingView.finishOffDrawing()
                            }

                            drawingView.isEnabled = false // nobody should be able to draw
                            drawingView.setColor(Color.BLACK)
                            drawingView.setThickness(Constants.DEFAULT_PAINT_THICKNESS)
                            roundTimerProgressBar.max = phaseChange.time.toInt()
                        }
                    }
                    else -> Unit
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.selectedColorButtonId.collect { id ->
                binding.colorGroup.check(id)

                when (id) {
                    R.id.rbBlack -> selectColor(Color.BLACK)
                    R.id.rbBlue -> selectColor(Color.BLUE)
                    R.id.rbGreen -> selectColor(Color.GREEN)
                    R.id.rbOrange -> {
                        selectColor(ContextCompat.getColor(this@DrawingActivity, R.color.orange))
                    }
                    R.id.rbRed -> selectColor(Color.RED)
                    R.id.rbYellow -> selectColor(Color.YELLOW)
                    R.id.rbEraser -> {
                        binding.drawingView.setColor(Color.WHITE)
                        binding.drawingView.setThickness(40f)
                    }
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.connectionProgressBarVisible.collect { isVisible ->
                binding.connectionProgressBar.isVisible = isVisible
            }
        }

        lifecycleScope.launchWhenStarted {
            viewModel.chooseWordOverlayVisible.collect { isVisible ->
                binding.chooseWordOverlay.isVisible = isVisible
            }
        }
    }

    // gets all the websocket data in this collect block
    private fun listenToSocketEvents() = lifecycleScope.launchWhenStarted {
        viewModel.socketEvent.collect { event ->
            when (event) {
                is DrawingViewModel.SocketEvent.DrawDataEvent -> {
                    val drawData = event.data

                    if (!binding.drawingView.isUserDrawing) { // somebody else is drawing
                        when (drawData.motionEvent) {
                            MotionEvent.ACTION_DOWN -> {
                                binding.drawingView.simulateStartedTouch(drawData)
                            }
                            MotionEvent.ACTION_MOVE -> {
                                binding.drawingView.simulateMovedTouch(drawData)
                            }
                            MotionEvent.ACTION_UP -> {
                                binding.drawingView.simulateReleasedTouch(drawData)
                            }
                        }
                    }
                }
                // once a player leaves and rejoins, update the screen with the latest info
                is DrawingViewModel.SocketEvent.RoundDrawInfoEvent -> {
                    binding.drawingView.update(event.data)
                }
                // when we are about to start, clear everything
                is DrawingViewModel.SocketEvent.GameStateEvent -> {
                    binding.drawingView.clear()
                }
                // receiving chosenWord means another player is the drawer
                is DrawingViewModel.SocketEvent.ChosenWordEvent -> {
                    binding.tvCurWord.text = event.data.chosenWord
                    binding.ibUndo.isEnabled = false
                }
                is DrawingViewModel.SocketEvent.ChatMessageEvent -> {
                    addChatObjectToRecyclerView(event.data)
                }
                is DrawingViewModel.SocketEvent.AnnouncementEvent -> {
                    addChatObjectToRecyclerView(event.data)
                }
                is DrawingViewModel.SocketEvent.UndoEvent -> {
                    binding.drawingView.undo()
                }
                is DrawingViewModel.SocketEvent.GameErrorEvent -> {
                    when (event.data.errorType) {
                        GameError.ERROR_ROOM_NOT_FOUND -> finish()
                    }
                }
                else -> Unit
            }
        }
    }

    private fun listenToConnectionEvents() = lifecycleScope.launchWhenStarted {
        viewModel.connectionEvent.collect { event ->
            when (event) {
                is WebSocket.Event.OnConnectionOpened<*> -> { // connection is established to server
                    viewModel.sendBaseModel(
                        JoinRoomHandshake(
                            args.userName, args.roomName, clientId
                        )
                    )
                    viewModel.setConnectionProgressBarVisibility(false)
                }
                is WebSocket.Event.OnConnectionFailed -> {
                    viewModel.setConnectionProgressBarVisibility(false)
                    Snackbar.make(binding.root, R.string.error_connection_failed, Snackbar.LENGTH_LONG).show()
                    event.throwable.printStackTrace()
                }
                is WebSocket.Event.OnConnectionClosed -> {
                    viewModel.setConnectionProgressBarVisibility(false)
                }
//                is WebSocket.Event.OnConnectionClosing -> { }
//                is WebSocket.Event.OnMessageReceived -> { } // observeBaseModels() handles this case
                else -> Unit
            }
        }
    }

    private fun updateChatMessageList(chat: List<BaseModel>) {
        updateChatJob?.cancel()
        updateChatJob = lifecycleScope.launch {
            chatMessageAdapter.updateDataSet(chat) // **
        }
    }

    private fun updatePlayersList(players: List<PlayerData>) {
        updatePlayersJob?.cancel()
        updatePlayersJob = lifecycleScope.launch {
            playerAdapter.updateDataSet(players)
        }
    }

    private suspend fun addChatObjectToRecyclerView(chatObject: BaseModel) {
        val canScrollDown = binding.rvChat.canScrollVertically(1) // positive: scroll down
        updateChatMessageList(chatMessageAdapter.chatObjects + chatObject)
        updateChatJob?.join() // suspends addChatObjectToRecyclerView() until updateChatJob finishes **

        // now new item is in the list. so we can scroll to the bottom
        if (!canScrollDown) { // if already at the bottom, go to the last item
            binding.rvChat.scrollToPosition(chatMessageAdapter.chatObjects.size - 1)
        }

    }

    private fun setupRecyclerView() = binding.rvChat.apply {
        chatMessageAdapter = ChatMessageAdapter(args.userName)
        adapter = chatMessageAdapter
        layoutManager = LinearLayoutManager(this@DrawingActivity)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        super.onPause()
        binding.rvChat.layoutManager?.onSaveInstanceState() // state of RecyclerView is saved
    }

    // gets called by LifecycleObserver interface
    // why not override onPause(): becuase we launch a dialog to ask permission for mic usage,
    // which calls onStop(). that is not what we want
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    private fun onAppInBackground() {
        viewModel.disconnect()
    }
}