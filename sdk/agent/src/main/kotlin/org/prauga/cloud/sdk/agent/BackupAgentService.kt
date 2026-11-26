// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.cloud.sdk.agent

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.os.Process
import android.os.RemoteException
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FilterOutputStream
import java.io.IOException
import java.io.OutputStream
import org.prauga.cloud.sdk.BackupContract
import org.prauga.cloud.sdk.IBackupAgent
import org.prauga.cloud.sdk.IResultCallback
import org.prauga.cloud.sdk.ResultCode

class BackupAgentService : Service() {

    private val executor = AgentRuntime.executor
    private val work by lazy { AgentRuntime.work(this) }

    private val binder =
        object : IBackupAgent.Stub() {
            override fun contractVersion(): Int = BackupContract.VERSION

            override fun describe(): Bundle = work.describe(trusted()).toBundle()

            override fun exportData(
                type: String,
                sink: ParcelFileDescriptor,
                callback: IResultCallback,
            ) {
                val trusted = trusted()
                executor.execute {
                    finish(sink, callback, trusted) {
                        val out = Progress(FileOutputStream(sink.fileDescriptor), callback)
                        work.export(type, out).also { out.flush() }
                    }
                }
            }

            override fun importData(
                type: String,
                fromSchema: Int,
                source: ParcelFileDescriptor,
                callback: IResultCallback,
            ) {
                val trusted = trusted()
                executor.execute {
                    finish(source, callback, trusted) {
                        work.import(type, fromSchema, FileInputStream(source.fileDescriptor))
                    }
                }
            }

            override fun restoreFinished() {
                if (!trusted()) return
                executor.execute {
                    work.restoreFinished()
                    if (work.restartRequired) Process.killProcess(Process.myPid())
                }
            }
        }

    override fun onBind(intent: Intent): IBinder? = binder.takeIf {
        intent.action == BackupContract.ACTION_AGENT
    }

    private fun trusted(): Boolean = TrustedProviders.isTrusted(this, Binder.getCallingUid())

    private fun finish(
        pipe: ParcelFileDescriptor,
        callback: IResultCallback,
        trusted: Boolean,
        block: () -> ResultCode,
    ) {
        val (code, reason) =
            if (!trusted) {
                ResultCode.UNTRUSTED_PEER to UNTRUSTED
            } else {
                try {
                    block() to null
                } catch (e: IOException) {
                    ResultCode.IO to e.message
                } catch (e: IllegalStateException) {
                    ResultCode.INTERNAL to e.message
                } catch (e: IllegalArgumentException) {
                    ResultCode.INTERNAL to e.message
                } catch (e: SecurityException) {
                    ResultCode.INTERNAL to e.message
                }
            }
        try {
            if (code == ResultCode.OK || code == ResultCode.SKIPPED) pipe.close()
            else pipe.closeWithError(reason ?: code.name)
        } catch (_: IOException) {
            pipe.close()
        }
        try {
            callback.onResult(code.code, reason)
        } catch (_: RemoteException) {
            return
        }
    }

    private class Progress(out: OutputStream, private val callback: IResultCallback) :
        FilterOutputStream(out) {
        private var written = 0L
        private var reported = 0L

        override fun write(b: ByteArray, off: Int, len: Int) {
            out.write(b, off, len)
            written += len
            if (written - reported >= REPORT_EVERY) {
                reported = written
                try {
                    callback.onProgress(written)
                } catch (_: RemoteException) {
                    throw IOException("provider went away")
                }
            }
        }
    }

    private companion object {
        const val UNTRUSTED =
            "Pvot Cloud SDK: this provider is not approved by the app or its user."
        const val REPORT_EVERY = 1L shl 20
    }
}
