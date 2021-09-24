package com.tsato.mobile.ekaki.util

import kotlinx.coroutines.*

class CoroutineTimer {

    /*
     returns this running coroutine job
     the client time might be not up-to-date when the server gives the client time,
     maybe for some connection issues,
     so the client doesn't receive the server's up-to-date time, which cause time out of sync

     in such cases, we want to cancel an old timer, and renew with the latest time
     */
    fun timeAndEmit(
        duration: Long, // how much one phase lasts ex. 30 seconds
        coroutineScope: CoroutineScope,
        emissionFrequency: Long = 100L, // how often we update UI ex. updating progress bar
        dispatcher: CoroutineDispatcher = Dispatchers.Main,
        onEmit: (Long) -> Unit // callback function called every emissionFrequency seconds
    ) : Job {

        return coroutineScope.launch(dispatcher) {
            var time = duration // current time

            while (time >= 0) {
                // subtract the emissionFrequency from the current time,
                // and call onEmit() and delay this coroutine
                onEmit(time)
                time -= emissionFrequency
                delay(emissionFrequency)
            }
        }
    }

}