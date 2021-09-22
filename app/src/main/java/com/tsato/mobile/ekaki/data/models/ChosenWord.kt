package com.tsato.mobile.ekaki.data.models

import com.tsato.mobile.ekaki.util.Constants.TYPE_CHOSEN_WORD

data class ChosenWord(
    val chosenWord: String,
    val roomName: String
) : BaseModel(TYPE_CHOSEN_WORD)
