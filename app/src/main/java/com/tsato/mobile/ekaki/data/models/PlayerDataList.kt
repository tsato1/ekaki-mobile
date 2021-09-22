package com.tsato.mobile.ekaki.data.models

import com.tsato.mobile.ekaki.util.Constants.TYPE_PLAYER_DATA_LIST

data class PlayerDataList(
    val players: List<PlayerData>
) : BaseModel(TYPE_PLAYER_DATA_LIST)
