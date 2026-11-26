// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.sdk.host

import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.os.RemoteException
import android.os.SystemClock
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withTimeoutOrNull
import org.prauga.cloud.sdk.AgentInfo
import org.prauga.cloud.sdk.IBackupAgent
import org.prauga.cloud.sdk.IResultCallback
import org.prauga.cloud.sdk.ResultCode

class AgentSession
internal constructor(
    private val agent: IBackupAgent,
    private val binder: IBinder,
    private val onClose: () -> Unit,
) : Closeable {

    private val local = binder.queryLocalInterface(IBackupAgent.DESCRIPTOR) != null

    fun describe(): AgentInfo? =
        try {
            AgentInfo.fromBundle(agent.describe())
        } catch (_: RemoteException) {
            null
        }

    suspend fun export(type: String, target: File): TransferResult {
        val (read, write) = ParcelFileDescriptor.createReliablePipe()
        return transfer(ours = read, theirs = write) { callback ->
            agent.exportData(type, write, callback)
            handOff(write)
            target.outputStream().use {
                callback.copy(ParcelFileDescriptor.AutoCloseInputStream(read), it)
            }
        }
    }

    suspend fun import(type: String, fromSchema: Int, source: File): TransferResult {
        val (read, write) = ParcelFileDescriptor.createReliablePipe()
        return transfer(ours = write, theirs = read) { callback ->
            agent.importData(type, fromSchema, read, callback)
            handOff(read)
            source.inputStream().use {
                callback.copy(it, ParcelFileDescriptor.AutoCloseOutputStream(write))
            }
        }
    }

    fun restoreFinished() {
        try {
            agent.restoreFinished()
        } catch (_: RemoteException) {
            return
        }
    }

    override fun close() {
        onClose()
    }

    private fun handOff(theirs: ParcelFileDescriptor) {
        if (!local) theirs.close()
    }

    private suspend fun transfer(
        ours: ParcelFileDescriptor,
        theirs: ParcelFileDescriptor,
        stream: suspend (Callback) -> Long,
    ): TransferResult = coroutineScope {
        val callback = Callback()
        val death = IBinder.DeathRecipient { callback.fail(STOPPED) }
        val watchdog = launch {
            while (isActive) {
                delay(WATCHDOG_TICK_MS)
                if (
                    SystemClock.elapsedRealtime() - callback.lastProgress.get() > STALL_TIMEOUT_MS
                ) {
                    callback.fail(STALLED)
                    ours.close()
                }
            }
        }
        try {
            binder.linkToDeath(death, 0)
            var streamError: String? = null
            val bytes =
                try {
                    stream(callback)
                } catch (e: IOException) {
                    streamError = e.message ?: "stream failed"
                    0L
                }
            val result = withTimeoutOrNull(STALL_TIMEOUT_MS) { callback.result.await() }
            when {
                result == null -> TransferResult(ResultCode.IO, streamError ?: "no result", bytes)
                result.code == ResultCode.OK && streamError != null ->
                    TransferResult(ResultCode.IO, streamError, bytes)
                else -> result.copy(bytes = bytes)
            }
        } catch (_: RemoteException) {
            TransferResult(ResultCode.INTERNAL, STOPPED, 0)
        } finally {
            watchdog.cancel()
            try {
                binder.unlinkToDeath(death, 0)
            } catch (_: NoSuchElementException) {
                Unit
            }
            handOff(theirs)
            ours.close()
        }
    }

    private class Callback : IResultCallback.Stub() {
        val result = CompletableDeferred<TransferResult>()
        val lastProgress = AtomicLong(SystemClock.elapsedRealtime())

        override fun onProgress(bytes: Long) {
            lastProgress.set(SystemClock.elapsedRealtime())
        }

        override fun onResult(code: Int, reason: String?) {
            result.complete(TransferResult(ResultCode.of(code), reason, 0))
        }

        fun fail(reason: String) {
            result.complete(TransferResult(ResultCode.IO, reason, 0))
        }

        suspend fun copy(input: InputStream, out: OutputStream): Long =
            runInterruptible(Dispatchers.IO) {
                val buffer = ByteArray(BUFFER)
                var total = 0L
                input.use {
                    out.use {
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            out.write(buffer, 0, read)
                            total += read
                            lastProgress.set(SystemClock.elapsedRealtime())
                        }
                    }
                }
                total
            }
    }

    private companion object {
        const val BUFFER = 64 * 1024
        const val WATCHDOG_TICK_MS = 5_000L
        const val STALL_TIMEOUT_MS = 120_000L
        const val STOPPED = "the app stopped"
        const val STALLED = "the app stopped sending data"
    }
}
