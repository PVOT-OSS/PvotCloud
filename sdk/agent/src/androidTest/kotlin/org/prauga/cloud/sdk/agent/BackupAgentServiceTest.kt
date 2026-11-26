// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.cloud.sdk.agent

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import java.io.File
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.prauga.cloud.sdk.AgentInfo
import org.prauga.cloud.sdk.BackupContract
import org.prauga.cloud.sdk.IBackupAgent
import org.prauga.cloud.sdk.IResultCallback
import org.prauga.cloud.sdk.ResultCode

@RunWith(AndroidJUnit4::class)
class BackupAgentServiceTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val note = File(context.filesDir, "service-note.txt")
    private val connection =
        object : ServiceConnection {
            val connected = CountDownLatch(1)
            var binder: IBinder? = null

            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                binder = service
                connected.countDown()
            }

            override fun onServiceDisconnected(name: ComponentName) = Unit
        }
    private lateinit var testSubject: IBackupAgent

    @Before
    fun setUp() {
        TrustedProviders.revokeAll(context)
        note.writeText("keep me")
        context.bindService(
            Intent(BackupContract.ACTION_AGENT).setPackage(context.packageName),
            connection,
            Context.BIND_AUTO_CREATE,
        )
        assertThat(connection.connected.await(TIMEOUT, TimeUnit.SECONDS)).isTrue()
        testSubject = IBackupAgent.Stub.asInterface(connection.binder)
    }

    @After
    fun tearDown() {
        context.unbindService(connection)
        TrustedProviders.revokeAll(context)
        note.delete()
    }

    @Test
    fun anUnapprovedCallerIsRefused() {
        val info = AgentInfo.fromBundle(testSubject.describe())
        val (code, _) = export()

        assertThat(info.callerTrusted).isFalse()
        assertThat(code).isEqualTo(ResultCode.UNTRUSTED_PEER)
    }

    @Test
    fun anApprovedCallerBacksUpAndRestores() {
        TrustedProviders.approve(
            context,
            SigningCertificates.sha256(context.packageManager, context.packageName),
        )
        val info = AgentInfo.fromBundle(testSubject.describe())
        val (code, archive) = export()
        note.delete()

        val restored = import(archive)

        assertThat(info.callerTrusted).isTrue()
        assertThat(info.types.map { it.name }).isEqualTo(listOf(BackupContract.TYPE_APP_DATA))
        assertThat(code).isEqualTo(ResultCode.OK)
        assertThat(restored).isEqualTo(ResultCode.OK)
        assertThat(note.readText()).isEqualTo("keep me")
    }

    @Test
    fun aNewerSchemaIsRefused() {
        TrustedProviders.approve(
            context,
            SigningCertificates.sha256(context.packageManager, context.packageName),
        )

        assertThat(import(ByteArray(0), fromSchema = 99)).isEqualTo(ResultCode.SCHEMA_TOO_NEW)
    }

    private fun export(): Pair<ResultCode, ByteArray> {
        val (read, write) = ParcelFileDescriptor.createReliablePipe()
        val callback = Callback()
        testSubject.exportData(BackupContract.TYPE_APP_DATA, write, callback)
        val bytes =
            try {
                ParcelFileDescriptor.AutoCloseInputStream(read).use { it.readBytes() }
            } catch (_: IOException) {
                ByteArray(0)
            }
        return callback.await() to bytes
    }

    private fun import(archive: ByteArray, fromSchema: Int = 1): ResultCode {
        val (read, write) = ParcelFileDescriptor.createReliablePipe()
        val callback = Callback()
        testSubject.importData(BackupContract.TYPE_APP_DATA, fromSchema, read, callback)
        ParcelFileDescriptor.AutoCloseOutputStream(write).use { it.write(archive) }
        return callback.await()
    }

    private class Callback : IResultCallback.Stub() {
        private val done = CountDownLatch(1)
        private var code = ResultCode.INTERNAL

        override fun onProgress(bytes: Long) = Unit

        override fun onResult(code: Int, reason: String?) {
            this.code = ResultCode.of(code)
            done.countDown()
        }

        fun await(): ResultCode {
            check(done.await(TIMEOUT, TimeUnit.SECONDS)) { "no result" }
            return code
        }
    }

    private companion object {
        const val TIMEOUT = 10L
    }
}
