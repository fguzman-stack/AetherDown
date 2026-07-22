package com.aetherdown.app.download

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue

class SpeedLimiter(
    maxBytesPerSecond: Long = 0L
) {
    @Volatile
    var maxBytesPerSecond: Long = maxBytesPerSecond
        set(value) {
            field = value
            window.clear()
        }

    private data class Chunk(val bytes: Int, val timestamp: Long)

    private val window: Queue<Chunk> = ConcurrentLinkedQueue()

    suspend fun limit(byteCount: Int, dispatcher: CoroutineDispatcher) = withContext(dispatcher) {
        if (maxBytesPerSecond <= 0L) return@withContext

        val now = System.currentTimeMillis()
        window.add(Chunk(byteCount, now))

        while (window.isNotEmpty() && now - window.peek().timestamp > 1000) {
            window.poll()
        }

        val windowBytes = window.sumOf { it.bytes.toLong() }

        if (windowBytes > maxBytesPerSecond) {
            val sleepTime = ((windowBytes - maxBytesPerSecond) * 1000) / maxBytesPerSecond
            delay(sleepTime.coerceAtMost(1000L))
        }
    }

    fun getCurrentSpeed(): Long {
        val now = System.currentTimeMillis()
        while (window.isNotEmpty() && now - window.peek().timestamp > 1000) {
            window.poll()
        }
        return window.sumOf { it.bytes.toLong() }
    }

    fun isLimited(): Boolean = maxBytesPerSecond > 0L
}
