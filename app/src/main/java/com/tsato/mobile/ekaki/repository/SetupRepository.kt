package com.tsato.mobile.ekaki.repository

import com.tsato.mobile.ekaki.data.remote.ws.Room
import com.tsato.mobile.ekaki.util.Resource

/*
    using this interface in deployed version

    in tests, we don't want to use the real api and make real network request
    instead, we build repositories for test cases that simulate the api behavior
 */
interface SetupRepository {

    suspend fun createRoom(room: Room): Resource<Unit>

    suspend fun getRooms(searchQuery: String): Resource<List<Room>>

    suspend fun joinRoom(userName: String, roomName: String): Resource<Unit>

}