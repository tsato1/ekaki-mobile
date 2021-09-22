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
import com.tsato.mobile.ekaki.util.Constants.HTTP_BASE_URL
import com.tsato.mobile.ekaki.util.Constants.HTTP_BASE_URL_LOCALHOST
import com.tsato.mobile.ekaki.util.Constants.RECONNECT_INTERVAL
import com.tsato.mobile.ekaki.util.Constants.USE_LOCALHOST
import com.tsato.mobile.ekaki.util.Constants.WS_BASE_URL
import com.tsato.mobile.ekaki.util.Constants.WS_BASE_URL_LOCALHOST
import com.tsato.mobile.ekaki.util.DispatcherProvider
import com.tsato.mobile.ekaki.util.clientId
import com.tsato.mobile.ekaki.util.dataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

/*
    contains all of our dependencies that are bounded to the application for life time (singletons)
 */

@ExperimentalCoroutinesApi
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideSetupRepository(
        setupApi: SetupApi,
        @ApplicationContext context: Context
    ) : SetupRepository = DefaultSetupRepository(setupApi, context)

    @Singleton // means the client that is returned from this func is a singleton
    @Provides
    fun provideOkHttpClient(clientId: String): OkHttpClient {
        // interceptor modifies any request that is sent by this OkHttp client
        return OkHttpClient
            .Builder()
            .addInterceptor { chain -> // chain contains info of current request we want to send to api
                val url = chain.request().url
                    .newBuilder()
                    .addQueryParameter("client_id", clientId)
                    .build()
                val request = chain.request()
                    .newBuilder()
                    .url(url)
                    .build()
                chain.proceed(request) // proceed to the next interceptor
            }
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    @Singleton
    @Provides
    fun provideClientId(@ApplicationContext context: Context): String {
        return runBlocking { context.dataStore.clientId() }
    }

    @Singleton
    @Provides
    fun provideDrawingApi(
        app: Application, // hilt automatically provides this
        okHttpClient: OkHttpClient, // the function provideOkHttpClient will provides this
        gson: Gson
    ) : DrawingApi {
        return Scarlet.Builder()
            // used if things go wrong, such as unexpected disconnection
            .backoffStrategy(LinearBackoffStrategy(RECONNECT_INTERVAL))
            // Scarlet will detect if the app is out of lifecycle
            .lifecycle(AndroidLifecycle.ofApplicationForeground(app))
            // to which url we want to connect
            .webSocketFactory(
                okHttpClient.newWebSocketFactory(if (USE_LOCALHOST) WS_BASE_URL_LOCALHOST else WS_BASE_URL)
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

    @Singleton
    @Provides
    fun provideSetupApi(okHttpClient: OkHttpClient): SetupApi {
        return Retrofit.Builder()
            .baseUrl(if (USE_LOCALHOST) HTTP_BASE_URL_LOCALHOST else HTTP_BASE_URL)
            // automates parsing incoming websocket json data to our data classes
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
            .create(SetupApi::class.java)
    }

    @Singleton
    @Provides
    fun provideApplicationContext(
        @ApplicationContext context: Context
    ) = context

    @Singleton
    @Provides
    fun provideGsonInstance(): Gson {
        return Gson()
    }

    @Singleton
    @Provides
    fun provideDispatcherProvider(): DispatcherProvider {
        return object : DispatcherProvider {
            override val main: CoroutineDispatcher
                get() = Dispatchers.Main
            override val io: CoroutineDispatcher
                get() = Dispatchers.IO
            override val default: CoroutineDispatcher
                get() = Dispatchers.Default
        }
    }

}