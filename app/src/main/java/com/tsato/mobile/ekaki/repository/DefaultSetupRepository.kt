package com.tsato.mobile.ekaki.repository

import android.content.Context
import com.tsato.mobile.ekaki.R
import com.tsato.mobile.ekaki.data.remote.api.SetupApi
import com.tsato.mobile.ekaki.data.remote.ws.Room
import com.tsato.mobile.ekaki.util.Resource
import com.tsato.mobile.ekaki.util.checkForInternetConnection
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class DefaultSetupRepository @Inject constructor(
    private val setupApi: SetupApi,
    private val context: Context
): SetupRepository {

    override suspend fun createRoom(room: Room): Resource<Unit> {
        if (!context.checkForInternetConnection()) { // just checks if internet connection transports are turned on
            return Resource.Error(context.getString(R.string.error_internet_turned_off))
        }

        // internet should be turned on from here after
        // catching the case when the setup for internet is on, but there are some other problems
        val response = try{
            setupApi.createRoom(room)
        }
        catch (e: HttpException) { // some internet connection error from the server side
            return Resource.Error(context.getString(R.string.error_http))
        }
        catch (e: IOException) { // thrown when there is a problem in transport from the client side
            return Resource.Error(context.getString(R.string.check_internet_connection))
        }

        // response.body()?.successful == true: means on server side, a room was created
        return if (response.isSuccessful && response.body()?.successful == true) {
            Resource.Success(Unit)
        }
        else if (response.body()?.successful == false) {
            Resource.Error(response.body()!!.message!!)
        }
        else {
            Resource.Error(context.getString(R.string.error_unknown))
        }
    }

    override suspend fun getRooms(searchQuery: String): Resource<List<Room>> {
        if (!context.checkForInternetConnection()) { // just checks if internet connection transports are turned on
            return Resource.Error(context.getString(R.string.error_internet_turned_off))
        }

        // internet should be turned on from here after
        // catching the case when the setup for internet is on, but there are some other problems
        val response = try{
            setupApi.getRooms(searchQuery)
        }
        catch (e: HttpException) { // some internet connection error from the server side
            return Resource.Error(context.getString(R.string.error_http))
        }
        catch (e: IOException) { // thrown when there is a problem in transport from the client side
            return Resource.Error(context.getString(R.string.check_internet_connection))
        }

        // response.body()?.successful == true: means on server side, a room was created
        return if (response.isSuccessful && response.body() != null) {
            Resource.Success(response.body()!!)
        }
        else {
            Resource.Error(context.getString(R.string.error_unknown))
        }
    }

    override suspend fun joinRoom(userName: String, roomName: String): Resource<Unit> {
        if (!context.checkForInternetConnection()) { // just checks if internet connection transports are turned on
            return Resource.Error(context.getString(R.string.error_internet_turned_off))
        }

        // internet should be turned on from here after
        // catching the case when the setup for internet is on, but there are some other problems
        val response = try{
            setupApi.joinRoom(userName, roomName)
        }
        catch (e: HttpException) { // some internet connection error from the server side
            return Resource.Error(context.getString(R.string.error_http))
        }
        catch (e: IOException) { // thrown when there is a problem in transport from the client side
            return Resource.Error(context.getString(R.string.check_internet_connection))
        }

        // response.body()?.successful == true: means on server side, a room was created
        return if (response.isSuccessful && response.body()?.successful == true) {
            Resource.Success(Unit)
        }
        else if (response.body()?.successful == false) {
            Resource.Error(response.body()!!.message!!)
        }
        else {
            Resource.Error(context.getString(R.string.error_unknown))
        }
    }
}