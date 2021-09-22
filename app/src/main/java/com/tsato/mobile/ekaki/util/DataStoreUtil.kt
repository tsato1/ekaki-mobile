package com.tsato.mobile.ekaki.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import java.util.*

val Context.dataStore by preferencesDataStore("settings")

/*
returns clientId from dataStore. if there isn't clientId, it will generate one and save it
 */
suspend fun DataStore<Preferences>.clientId(): String {
    val clientIdKey = stringPreferencesKey("clientId")
    val preferences = data.first()
    val clientIdExists = preferences[clientIdKey] != null
    return if (clientIdExists) {
        preferences[clientIdKey] ?: ""
    }
    else {
        val clientId = UUID.randomUUID().toString()
        edit { settings ->
            settings[clientIdKey] = clientId
        }
        clientId
    }
}