package com.tsato.mobile.ekaki.data.models

import com.tsato.mobile.ekaki.util.Constants.TYPE_CURR_ROUND_DRAW_INFO

data class RoundDrawInfo(
    // this string is a json string that represents a single DrawData a client sends to the server
    // it also contains DrawActions
    val data: List<String>
) : BaseModel(TYPE_CURR_ROUND_DRAW_INFO)
