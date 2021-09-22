package com.tsato.mobile.ekaki.data.remote.ws

import com.tinder.scarlet.WebSocket
import com.tinder.scarlet.ws.Receive
import com.tinder.scarlet.ws.Send
import com.tsato.mobile.ekaki.data.models.BaseModel
import kotlinx.coroutines.flow.Flow

interface DrawingApi {

    /*
        observe websocket events in Flow object: when opening and closing connections
     */
    @Receive
    fun observeEvents() : Flow<WebSocket.Event>

    /*
        send websocket data. returns if it was successful or not
     */
    @Send
    fun sendBaseModel(baseModel: BaseModel) : Boolean

    /*
        observe all incoming websocket data
     */
    @Receive
    fun observeBaseModels() : Flow<BaseModel>
}