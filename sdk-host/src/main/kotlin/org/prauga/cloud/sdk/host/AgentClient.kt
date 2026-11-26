// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.sdk.host

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.prauga.cloud.sdk.BackupContract
import org.prauga.cloud.sdk.IBackupAgent

class AgentClient @Inject constructor(@ApplicationContext private val context: Context) {

    suspend fun connect(packageName: String): AgentSession {
        var connection: ServiceConnection? = null
        val binder =
            withTimeoutOrNull(BIND_TIMEOUT_MS) {
                suspendCancellableCoroutine { continuation ->
                    val callback =
                        object : ServiceConnection {
                            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                                if (continuation.isActive) continuation.resume(service)
                            }

                            override fun onServiceDisconnected(name: ComponentName) = Unit

                            override fun onNullBinding(name: ComponentName) {
                                if (continuation.isActive) continuation.resume(null)
                            }
                        }
                    connection = callback
                    val intent = Intent(BackupContract.ACTION_AGENT).setPackage(packageName)
                    if (!context.bindService(intent, callback, Context.BIND_AUTO_CREATE)) {
                        continuation.resume(null)
                    }
                }
            }
        val bound = connection
        if (binder == null) {
            bound?.let(::unbind)
            throw IOException("could not connect to $packageName")
        }
        return AgentSession(IBackupAgent.Stub.asInterface(binder), binder) { bound?.let(::unbind) }
    }

    private fun unbind(connection: ServiceConnection) {
        try {
            context.unbindService(connection)
        } catch (_: IllegalArgumentException) {
            return
        }
    }

    private companion object {
        const val BIND_TIMEOUT_MS = 10_000L
    }
}
