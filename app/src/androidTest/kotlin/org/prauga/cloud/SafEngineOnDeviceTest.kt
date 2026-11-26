// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud

import android.os.Bundle
import android.provider.DocumentsContract
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.time.OffsetDateTime
import kotlin.random.Random
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.prauga.cloud.repo.Repository
import org.prauga.cloud.repo.ZstdCompressor
import org.prauga.cloud.repo.backup.Backup
import org.prauga.cloud.repo.check.Check
import org.prauga.cloud.repo.index.MasterIndex
import org.prauga.cloud.repo.restore.Restore
import org.prauga.cloud.repo.source.SourceDirectory
import org.prauga.cloud.repo.source.SourceEntry
import org.prauga.cloud.repo.source.SourceFile
import org.prauga.cloud.repo.tree.NodeType
import org.prauga.cloud.storage.SafBackend
import org.prauga.cloud.storage.SafDocumentTree

@RunWith(AndroidJUnit4::class)
class SafEngineOnDeviceTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val resolver = context.contentResolver
    private val grant =
        resolver.persistedUriPermissions.firstOrNull { it.isReadPermission && it.isWritePermission }
    private var testFolder: String? = null

    private val time = OffsetDateTime.parse("2026-11-27T12:00:00Z")
    private val files =
        (1..4).associate { "IMG_000$it.jpg" to Random(it).nextBytes(3 * 1024 * 1024) } +
            (1..50).associate { "note-$it.txt" to "note $it\n".repeat(100).toByteArray() }

    private inner class Folder(
        override val name: String,
        private val entries: Map<String, ByteArray>,
    ) : SourceDirectory {
        override val modTime: OffsetDateTime = time

        override fun children(): List<SourceEntry> =
            entries.map { (fileName, data) ->
                object : SourceFile {
                    override val name: String = fileName
                    override val modTime: OffsetDateTime = time

                    override fun open(): InputStream = ByteArrayInputStream(data)
                }
            }
    }

    @Before
    fun needsAGrantedFolder() {
        assumeTrue("choose a backup folder in the app first", grant != null)
    }

    @After
    fun removeTestFolder() {
        val uri = grant?.uri ?: return
        testFolder?.let {
            DocumentsContract.deleteDocument(
                resolver,
                DocumentsContract.buildDocumentUriUsingTree(uri, it),
            )
        }
    }

    @Test
    fun backsUpAndRestoresThroughTheChosenFolder() {
        val treeUri = checkNotNull(grant).uri
        val rootId = DocumentsContract.getTreeDocumentId(treeUri)
        val created =
            checkNotNull(
                DocumentsContract.createDocument(
                    resolver,
                    DocumentsContract.buildDocumentUriUsingTree(treeUri, rootId),
                    DocumentsContract.Document.MIME_TYPE_DIR,
                    "pvot-cloud-device-test",
                )
            )
        val folderId = DocumentsContract.getDocumentId(created).also { testFolder = it }
        val compressor = ZstdCompressor()

        val repository =
            timed("create repository") {
                Repository.create(
                    SafBackend(SafDocumentTree(resolver, treeUri, folderId)),
                    compressor,
                    PHRASE,
                    "pixel",
                    "test",
                )
            }
        val first =
            timed("first backup") {
                Backup(repository, MasterIndex.empty())
                    .run(Folder("phone", files), "pixel", listOf("/"))
            }
        report("first backup: ${first.files} files, ${first.bytes} bytes, ${first.newPacks} packs")

        val reopened =
            timed("open repository") {
                Repository.open(
                    SafBackend(SafDocumentTree(resolver, treeUri, folderId)),
                    compressor,
                    PHRASE,
                )
            }
        val second =
            timed("second backup") {
                Backup(reopened, MasterIndex.load(reopened))
                    .run(Folder("phone", files), "pixel", listOf("/"))
            }
        assertEquals(0, second.newBlobs)

        val restore = Restore(reopened, MasterIndex.load(reopened))
        val restored =
            timed("restore and compare") {
                restore
                    .entries(first.tree)
                    .filter { it.node.type == NodeType.FILE }
                    .onEach {
                        assertArrayEquals(
                            it.path,
                            files.getValue(it.path),
                            restore.open(it.node).use { s -> s.readBytes() },
                        )
                    }
                    .count()
            }
        assertEquals(files.size, restored)

        val check =
            timed("check with all data") {
                Check(reopened, MasterIndex.load(reopened)).run(readDataPercent = 100)
            }
        assertTrue(check.problems.toString(), check.ok)
    }

    private fun report(message: String) {
        InstrumentationRegistry.getInstrumentation()
            .sendStatus(STATUS, Bundle().apply { putString(TAG, message) })
    }

    private fun <T> timed(label: String, block: () -> T): T {
        val start = System.nanoTime()
        return block().also { report("$label took ${(System.nanoTime() - start) / 1_000_000} ms") }
    }

    private companion object {
        const val TAG = "PvotCloudSaf"
        const val STATUS = 1001
        const val PHRASE =
            "abandon ability able about above absent absorb abstract absurd abuse access accident"
    }
}
