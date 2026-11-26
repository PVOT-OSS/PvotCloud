// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.sdk.host

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isGreaterThan
import assertk.assertions.isNotNull
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.prauga.cloud.sdk.BackupContract
import org.prauga.cloud.sdk.ResultCode
import org.prauga.cloud.sdk.agent.TrustedProviders

@RunWith(AndroidJUnit4::class)
class AgentSessionTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val note = File(context.filesDir, "host-note.txt")
    private val archive = File(context.cacheDir, "host-test.tar")

    @Before
    fun setUp() {
        TrustedProviders.revokeAll(context)
        note.writeText("from the host test")
    }

    @After
    fun tearDown() {
        TrustedProviders.revokeAll(context)
        note.delete()
        archive.delete()
    }

    @Test
    fun anUnapprovedProviderGetsTheAgentsAnswer() = runBlocking {
        val result =
            AgentClient(context).connect(context.packageName).use { it.export(TYPE, archive) }

        assertThat(result.code).isEqualTo(ResultCode.UNTRUSTED_PEER)
    }

    @Test
    fun exportsToAFileAndImportsItBack() = runBlocking {
        approveSelf()
        val testSubject = AgentClient(context).connect(context.packageName)

        val info = testSubject.describe()
        val exported = testSubject.export(TYPE, archive)
        note.delete()
        val imported = testSubject.import(TYPE, 1, archive)
        testSubject.close()

        assertThat(info?.callerTrusted).isEqualTo(true)
        assertThat(exported.code).isEqualTo(ResultCode.OK)
        assertThat(exported.bytes).isGreaterThan(0)
        assertThat(imported.code).isEqualTo(ResultCode.OK)
        assertThat(note.readText()).isEqualTo("from the host test")
    }

    @Test
    fun discoveryLeavesOutTheProviderItself() {
        val agents = AgentDiscovery(context).agents()

        assertThat(agents.any { it.packageName == context.packageName }).isFalse()
        assertThat(AgentDiscovery(context).app(context.packageName)?.certificates).isNotNull()
    }

    private fun approveSelf() {
        val certificates = AgentDiscovery(context).app(context.packageName)?.certificates.orEmpty()
        val dir = File(context.noBackupFilesDir, "org.prauga.cloud.sdk").apply { mkdirs() }
        File(dir, "trusted_providers").writeText(certificates.joinToString("\n"))
    }

    private companion object {
        const val TYPE = BackupContract.TYPE_APP_DATA
    }
}
