package com.tsato.mobile.ekaki.data.models

import com.tsato.mobile.ekaki.util.Constants.TYPE_NEW_WORDS

data class NewWords(
    val newWords: List<String>
) : BaseModel(TYPE_NEW_WORDS)
