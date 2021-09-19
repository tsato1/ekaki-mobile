package com.tsato.mobile.ekaki.di

import com.google.gson.Gson
import com.tsato.mobile.ekaki.data.remote.api.SetupApi
import com.tsato.mobile.ekaki.util.Constants.HTTP_BASE_URL
import com.tsato.mobile.ekaki.util.Constants.HTTP_BASE_URL_LOCALHOST
import com.tsato.mobile.ekaki.util.Constants.USE_LOCALHOST
import com.tsato.mobile.ekaki.util.DispatcherProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

/*
    contains all of our dependencies that are bounded to the application life time (singletons)
 */

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton // means the client that is returned from this func is a singleton
    @Provides
    fun probideOkHttpClient(): OkHttpClient {
        // interceptor modifies any request that is sent by this OkHttp client
        return OkHttpClient
            .Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    @Singleton
    @Provides
    fun provideSetupApi(okHttpClient: OkHttpClient): SetupApi {
        return Retrofit.Builder()
            .baseUrl(if (USE_LOCALHOST) HTTP_BASE_URL_LOCALHOST else HTTP_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
            .create(SetupApi::class.java)
    }

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