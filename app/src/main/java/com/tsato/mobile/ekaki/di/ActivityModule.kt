package com.tsato.mobile.ekaki.di

import android.app.Application
import android.content.Context
import com.google.gson.Gson
import com.tinder.scarlet.Scarlet
import com.tinder.scarlet.lifecycle.android.AndroidLifecycle
import com.tinder.scarlet.retry.LinearBackoffStrategy
import com.tinder.scarlet.websocket.okhttp.newWebSocketFactory
import com.tsato.mobile.ekaki.data.remote.api.SetupApi
import com.tsato.mobile.ekaki.data.remote.ws.CustomGsonMessageAdapter
import com.tsato.mobile.ekaki.data.remote.ws.DrawingApi
import com.tsato.mobile.ekaki.data.remote.ws.FlowStreamAdapter
import com.tsato.mobile.ekaki.repository.DefaultSetupRepository
import com.tsato.mobile.ekaki.repository.SetupRepository
import com.tsato.mobile.ekaki.util.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.ExperimentalCoroutinesApi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@ExperimentalCoroutinesApi
@Module
@InstallIn(ActivityRetainedComponent::class)
/*
 ActivityComponent cares about configuration changes, ActivityRetainedComponent doesn't

 this way, DrawingActivity will be recreated when we rejoin the same room
 */
object ActivityModule {

    @ActivityRetainedScoped
    @Provides
    fun provideSetupRepository(
        setupApi: SetupApi,
        @ApplicationContext context: Context
    ) : SetupRepository = DefaultSetupRepository(setupApi, context)

    @ActivityRetainedScoped
    @Provides
    fun provideDrawingApi(
        app: Application, // hilt automatically provides this
        okHttpClient: OkHttpClient, // the function provideOkHttpClient will provides this
        gson: Gson
    ) : DrawingApi {
        return Scarlet.Builder()
            // used if things go wrong, such as unexpected disconnection
            .backoffStrategy(LinearBackoffStrategy(Constants.RECONNECT_INTERVAL))
            // Scarlet will detect if the app is out of lifecycle
            .lifecycle(AndroidLifecycle.ofApplicationForeground(app))
            // to which url we want to connect
            .webSocketFactory(
                okHttpClient.newWebSocketFactory(if (Constants.USE_LOCALHOST) Constants.WS_BASE_URL_LOCALHOST else Constants.WS_BASE_URL)
            )
            // transforms incoming data to object that we want to have.
            // in DrawingApi, we want to use Flow, but Scarlet doesn't know how to transform
            // incoming json data into Flow. We create our own Factory to do this: FlowStreamAdapter.kt
            .addStreamAdapterFactory(FlowStreamAdapter.Factory)
            // equivalent to addConverterFactory in Retrofit
            .addMessageAdapterFactory(CustomGsonMessageAdapter.Factory(gson))
            .build()
            .create()
    }

    @ActivityRetainedScoped
    @Provides
    fun provideSetupApi(okHttpClient: OkHttpClient): SetupApi {
        return Retrofit.Builder()
            .baseUrl(if (Constants.USE_LOCALHOST) Constants.HTTP_BASE_URL_LOCALHOST else Constants.HTTP_BASE_URL)
            // automates parsing incoming websocket json data to our data classes
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
            .create(SetupApi::class.java)
    }
}