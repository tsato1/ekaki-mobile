package com.tsato.mobile.ekaki.data.models

import com.tsato.mobile.ekaki.data.remote.ws.Room
import com.tsato.mobile.ekaki.util.Constants.TYPE_PHASE_CHANGE

data class PhaseChange(
    var phase: Room.Phase?, // nullable for the time when the game needs to synchronize the phase the timestamp
    var time: Long,
    val drawingPlayer: String? = null
) : BaseModel(TYPE_PHASE_CHANGE)
