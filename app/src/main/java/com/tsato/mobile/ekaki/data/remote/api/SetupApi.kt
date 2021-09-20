package com.tsato.mobile.ekaki.data.remote.api

import com.tsato.mobile.ekaki.data.remote.responses.BasicApiResponse
import com.tsato.mobile.ekaki.data.remote.ws.Room
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface SetupApi {

    @POST("/api/createRoom")
    suspend fun createRoom(
        @Body room: Room
    ): Response<BasicApiResponse>

    @GET("/api/getRooms")
    suspend fun getRooms(
        @Query("searchQuery") searchQuery: String
    ): Response<List<Room>>

    @GET("/api/joinRoom")
    suspend fun joinRoom(
        @Query("userName") userName: String,
        @Query("roomName") roomName: String
    ): Response<BasicApiResponse>

}