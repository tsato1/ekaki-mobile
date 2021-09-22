package com.tsato.mobile.ekaki.data.remote.ws

import com.tinder.scarlet.Stream
import com.tinder.scarlet.StreamAdapter
import com.tinder.scarlet.utils.getRawType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.lang.reflect.Type

@ExperimentalCoroutinesApi
class FlowStreamAdapter<T> : StreamAdapter<T, Flow<T>> {

    override fun adapt(stream: Stream<T>): Flow<T> {
        // callbackFlow: similar to SharedFlow; we use it to send events to this callbackFlow.
        // Pro: we can actively suspend this flow so that It will keep listening to events
        return callbackFlow {
            stream.start(object : Stream.Observer<T> {
                override fun onComplete() {
                    close()
                }

                override fun onError(throwable: Throwable) {
                    close(cause = throwable)
                }

                override fun onNext(data: T) {
                    if (!isClosedForSend) {
                        // send this data through Flow
                        offer(data)
                    }
                }
            })
            awaitClose { } // Flow is kept alive until it gets closed
        }
    }

    object Factory : StreamAdapter.Factory { // similar to ViewModelFactory
        override fun create(type: Type): StreamAdapter<Any, Any> {
            return when (type.getRawType()) {
                Flow::class.java -> FlowStreamAdapter()
                else -> throw IllegalStateException("Invalid Stream Adapter") // will never happen
            }
        }
    }

}