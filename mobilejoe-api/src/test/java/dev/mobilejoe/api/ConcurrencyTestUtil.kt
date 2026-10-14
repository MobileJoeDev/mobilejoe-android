// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.api

import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

/**
 * Runs [block] on [threads] threads that all start at the same moment, and waits for them.
 *
 * @param threads The number of threads calling [block].
 * @param block The work to run, given the index of its thread.
 * @return Every thread's result, by thread index.
 * @throws java.util.concurrent.ExecutionException if [block] failed on any thread.
 */
internal fun <T> runConcurrently(threads: Int, block: (index: Int) -> T): List<T> {
    val start = CountDownLatch(1)
    val executor = Executors.newFixedThreadPool(threads)

    try {
        val results = (0 until threads).map { index ->
            executor.submit(Callable {
                start.await()
                block(index)
            })
        }
        start.countDown()
        return results.map { result -> result.get() }
    } finally {
        executor.shutdownNow()
    }
}
