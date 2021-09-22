package com.tsato.mobile.ekaki.data.models

import com.tsato.mobile.ekaki.util.Constants.TYPE_JOIN_ROOM_HANDSHAKE

data class JoinRoomHandshake(
    val userName: String,
    val roomName: String,
    val clientId: String
): BaseModel(TYPE_JOIN_ROOM_HANDSHAKE)
