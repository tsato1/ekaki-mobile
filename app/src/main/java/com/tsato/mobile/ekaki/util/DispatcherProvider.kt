package com.tsato.mobile.ekaki.util

import kotlinx.coroutines.CoroutineDispatcher

/*
    this class is to make different usage of coroutine in tests;
    when we test functions that use coroutines, we want to use special test coroutine dispatcher
    which uses all of the options
 */
interface DispatcherProvider {
    val main: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
}