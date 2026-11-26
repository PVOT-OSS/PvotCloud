// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.sdk.host

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.prauga.cloud.repo.source.SourceDirectory
import org.prauga.cloud.repo.source.SourceFile
import org.prauga.cloud.sdk.ResultCode
import org.prauga.cloud.sdk.agent.TrustedProviders

@RunWith(AndroidJUnit4::class)
class AppBackupTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val note = File(context.filesDir, "app-backup-note.txt")
    private val workDir = File(context.cacheDir, "app-backup-test")
    private val discovery = AgentDiscovery(context)
    private val testSubject = AppBackup(discovery, AgentClient(context))
    private val self = context.packageName
    private val certificate = discovery.app(self)?.certificates.orEmpty().first()

    @Before
    fun setUp() {
        TrustedProviders.revokeAll(context)
        note.writeText("before")
    }

    @After
    fun tearDown() {
        TrustedProviders.revokeAll(context)
        note.delete()
        workDir.deleteRecursively()
    }

    @Test
    fun unapprovedAppsAreReportedAndLeftOut() =
        runBlocking<Unit> {
            val export = testSubject.export(mapOf(self to certificate), workDir)

            assertThat(export.root).isNull()
            assertThat(export.results.map { it.code }).containsExactly(ResultCode.NOT_CONSENTED)
        }

    @Test
    fun aChangedCertificateIsSkipped() =
        runBlocking<Unit> {
            approveSelf()

            val export = testSubject.export(mapOf(self to "00ff"), workDir)

            assertThat(export.results.single().code).isEqualTo(ResultCode.SKIPPED)
        }

    @Test
    fun anApprovedAppIsExportedAndRestored() =
        runBlocking<Unit> {
            approveSelf()
            val export = testSubject.export(mapOf(self to certificate), workDir)
            val appDir = (export.root?.children()?.single() as SourceDirectory)
            val files =
                appDir.children().filterIsInstance<SourceFile>().associate { it.name to it::open }
            val copies = files.mapValues { (_, open) -> open().readBytes() }
            note.writeText("after")

            val restored =
                testSubject.restore(
                    self,
                    copies.mapValues { (_, bytes) -> { bytes.inputStream() } },
                    workDir,
                )

            assertThat(export.root?.name).isEqualTo(AppBackup.APPS)
            assertThat(appDir.name).isEqualTo(self)
            assertThat(copies.keys.sorted()).containsExactly("app-data", "app.json")
            assertThat(restored.code).isEqualTo(ResultCode.OK)
            assertThat(note.readText()).isEqualTo("before")
            assertThat(testSubject.isApproved(self)).isEqualTo(true)
        }

    @Test
    fun aBackupWithoutItsManifestFails() =
        runBlocking<Unit> {
            approveSelf()

            val restored =
                testSubject.restore(
                    self,
                    mapOf("app-data" to { ByteArray(0).inputStream() }),
                    workDir,
                )

            assertThat(restored.code).isEqualTo(ResultCode.IO)
            assertThat(restored.detail).isNotNull()
        }

    private fun approveSelf() {
        val dir = File(context.noBackupFilesDir, "org.prauga.cloud.sdk").apply { mkdirs() }
        File(dir, "trusted_providers").writeText(certificate)
    }
}
