// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.cloud.sdk.agent

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class TarTest {

    @get:Rule val folder = TemporaryFolder()

    private val files =
        mapOf(
            "file/notes/today.txt" to "milk, eggs".toByteArray(),
            "db/ledger.db" to ByteArray(1500) { it.toByte() },
            "sp/settings.xml" to ByteArray(0),
            "file/" + "a".repeat(120) + "/" + "b".repeat(90) to "long".toByteArray(),
        )

    @Test
    fun readsBackWhatItWrote() {
        val archive = write(files)

        assertThat(read(archive)).isEqualTo(files.mapValues { it.value.toList() })
    }

    @Test
    fun systemTarExtractsTheArchive() {
        assumeTrue(File("/usr/bin/tar").canExecute())
        val archive = folder.newFile("app.tar").apply { writeBytes(write(files)) }
        val out = folder.newFolder("out")

        val process = ProcessBuilder("/usr/bin/tar", "-xf", archive.path, "-C", out.path).start()

        assertThat(process.waitFor(30, TimeUnit.SECONDS) && process.exitValue() == 0)
            .isEqualTo(true)
        files.forEach { (path, data) ->
            assertThat(out.resolve(path).readBytes().toList()).isEqualTo(data.toList())
        }
    }

    @Test
    fun anEmptyArchiveHasNoEntries() {
        val testSubject = TarReader(ByteArrayInputStream(write(emptyMap())))

        assertThat(testSubject.next()).isNull()
    }

    @Test
    fun aDamagedHeaderIsRejected() {
        val archive = write(files).also { it[10] = 'X'.code.toByte() }

        assertFailure { read(archive) }.isInstanceOf<IOException>()
    }

    @Test
    fun aTruncatedArchiveIsRejected() {
        val archive = write(files).copyOf(700)

        assertFailure { read(archive) }.isInstanceOf<IOException>()
    }

    @Test
    fun aShortFileFailsTheWrite() {
        val testSubject = TarWriter(ByteArrayOutputStream())

        assertFailure { testSubject.file("file/a", 10, 0, ByteArrayInputStream(ByteArray(3))) }
            .isInstanceOf<IOException>()
    }

    private fun write(entries: Map<String, ByteArray>): ByteArray {
        val out = ByteArrayOutputStream()
        val testSubject = TarWriter(out)
        entries.forEach { (path, data) ->
            testSubject.file(path, data.size.toLong(), MODIFIED, ByteArrayInputStream(data))
        }
        testSubject.finish()
        return out.toByteArray()
    }

    private fun read(archive: ByteArray): Map<String, List<Byte>> {
        val testSubject = TarReader(ByteArrayInputStream(archive))
        return buildMap {
            while (true) {
                val entry = testSubject.next() ?: break
                put(entry.path, testSubject.content().readBytes().toList())
                assertThat(entry.modifiedSeconds).isEqualTo(MODIFIED)
            }
        }
    }

    private companion object {
        const val MODIFIED = 1_790_000_000L
    }
}
