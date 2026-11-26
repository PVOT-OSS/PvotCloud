// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.sources

import java.io.ByteArrayInputStream
import java.time.Instant
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.prauga.cloud.repo.source.SourceDirectory
import org.prauga.cloud.repo.source.SourceEntry
import org.prauga.cloud.repo.source.SourceFile

class MediaTreeTest {

    private fun item(id: Long, path: String, name: String) =
        MediaItem(id, path, name, size = 3, modified = Instant.ofEpochSecond(1_700_000_000L + id))

    private fun SourceEntry.dir() = this as SourceDirectory

    private fun SourceDirectory.child(name: String) = children().single { it.name == name }

    @Test
    fun foldersFollowTheRelativePaths() {
        val root =
            MediaTree.build(
                "media",
                listOf(
                    item(1, "DCIM/Camera/", "IMG_1.jpg"),
                    item(2, "DCIM/Camera/", "IMG_2.jpg"),
                    item(3, "Pictures/Screenshots/", "shot.png"),
                    item(4, "", "loose.jpg"),
                ),
            ) {
                ByteArrayInputStream(byteArrayOf(it.id.toByte()))
            }

        assertEquals("media", root.name)
        assertEquals(
            setOf("DCIM", "Pictures", "loose.jpg"),
            root.children().map { it.name }.toSet(),
        )
        val camera = root.child("DCIM").dir().child("Camera").dir()
        assertEquals(listOf("IMG_1.jpg", "IMG_2.jpg"), camera.children().map { it.name })
        val shot =
            root.child("Pictures").dir().child("Screenshots").dir().child("shot.png") as SourceFile
        assertArrayEquals(byteArrayOf(3), shot.open().readBytes())
    }

    @Test
    fun duplicateNamesInAFolderAreKeptApart() {
        val root =
            MediaTree.build(
                "media",
                listOf(
                    item(1, "DCIM/", "a.jpg"),
                    item(2, "DCIM/", "a.jpg"),
                    item(3, "DCIM/", "a.jpg"),
                ),
            ) {
                ByteArrayInputStream(ByteArray(0))
            }

        assertEquals(
            listOf("a.jpg", "a (2).jpg", "a (3).jpg"),
            root.child("DCIM").dir().children().map { it.name },
        )
    }

    @Test
    fun aFolderTakesTheTimeOfItsNewestFile() {
        val root =
            MediaTree.build("media", listOf(item(1, "A/", "x"), item(9, "A/", "y"))) {
                ByteArrayInputStream(ByteArray(0))
            }

        assertEquals(Instant.ofEpochSecond(1_700_000_009L), root.child("A").modTime.toInstant())
    }
}
