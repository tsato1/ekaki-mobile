package com.tsato.mobile.ekaki.data.models

import com.tsato.mobile.ekaki.util.Constants.TYPE_CURR_ROUND_DRAW_INFO

data class RoundDrawInfo(
    val data: List<String>
) : BaseModel(TYPE_CURR_ROUND_DRAW_INFO)
