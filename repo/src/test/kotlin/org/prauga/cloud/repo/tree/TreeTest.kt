// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.repo.tree

import java.time.OffsetDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.prauga.cloud.repo.Id

class TreeTest {

    private val time = OffsetDateTime.parse("2026-11-26T10:00:00Z")

    private fun file(name: String) = Node(name, NodeType.FILE, time)

    private fun json(tree: Tree) = tree.encode().decodeToString()

    @Test
    fun encodesTheDesignDocExampleLikeRestic() {
        val node =
            Node(
                name = "testdata",
                type = NodeType.DIR,
                mode = 493,
                modTime = OffsetDateTime.parse("2014-12-22T14:47:59.912418701+01:00"),
                accessTime = OffsetDateTime.parse("2014-12-06T17:49:21.748468803+01:00"),
                changeTime = OffsetDateTime.parse("2014-12-22T14:47:59.912418701+01:00"),
                uid = 1000,
                gid = 100,
                user = "fd0",
                inode = 409704562,
                subtree = Id("b26e315b0988ddcd1cee64c351d13a100fedbc9fdbb144a67d1b765ab280b4dc"),
            )

        val expected =
            """{"nodes":[{"name":"testdata","type":"dir","mode":493,""" +
                """"mtime":"2014-12-22T14:47:59.912418701+01:00","atime":"2014-12-06T17:49:21.748468803+01:00",""" +
                """"ctime":"2014-12-22T14:47:59.912418701+01:00","uid":1000,"gid":100,"user":"fd0",""" +
                """"inode":409704562,"content":null,""" +
                """"subtree":"b26e315b0988ddcd1cee64c351d13a100fedbc9fdbb144a67d1b765ab280b4dc"}]}""" +
                "\n"
        assertEquals(expected, json(Tree(listOf(node))))
    }

    @Test
    fun encodesFileContentAndOmitsZeroFields() {
        val blob = Id("50f77b3b4291e8411a027b9f9b9e64658181cc676ce6ba9958b95f268cb1109d")
        val node =
            file("photo.jpg").copy(mode = 420, size = 1234, links = 1, content = listOf(blob))

        assertEquals(
            """{"nodes":[{"name":"photo.jpg","type":"file","mode":420,"mtime":"2026-11-26T10:00:00Z",""" +
                """"atime":"2026-11-26T10:00:00Z","ctime":"2026-11-26T10:00:00Z","uid":0,"gid":0,""" +
                """"size":1234,"links":1,"content":["${blob.hex}"]}]}""" +
                "\n",
            json(Tree(listOf(node))),
        )
    }

    @Test
    fun sortsNamesByUtf8Bytes() {
        val names = listOf("😀", "Ａ", "é", "b", "a", "Z")

        assertEquals(
            listOf("Z", "a", "b", "é", "Ａ", "😀"),
            Tree(names.map(::file)).nodes.map { it.name },
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsDuplicateNames() {
        Tree(listOf(file("a"), file("a")))
    }

    @Test
    fun quotesNamesLikeGo() {
        val tree = Tree(listOf(file("say \"hi\"\tnow"), file("a<b>&c"), file("nbsp x")))

        val encoded = json(tree)

        assertContains(encoded, """"name":"say \\\"hi\\\"\\tnow"""")
        assertContains(encoded, """"name":"a\u003cb\u003e\u0026c"""")
        assertContains(encoded, """"name":"nbsp\\u00a0x"""")
        assertEquals(tree.nodes, Tree.decode(tree.encode()).nodes)
    }

    private fun assertContains(text: String, part: String) =
        assertTrue("missing $part", text.contains(part))

    @Test
    fun escapesLineAndParagraphSeparatorsLikeGo() {
        val tree = Tree(listOf(file("a\u2028b\u2029c")))

        assertContains(json(tree), "\\u2028")
        assertContains(json(tree), "\\u2029")
        assertEquals(tree.nodes, Tree.decode(tree.encode()).nodes)
    }

    @Test
    fun trimsTrailingZerosInNanoseconds() {
        val node = file("f").copy(modTime = OffsetDateTime.parse("2026-01-02T03:04:05.500+05:30"))

        assertContains(json(Tree(listOf(node))), """"mtime":"2026-01-02T03:04:05.5+05:30"""")
    }

    @Test
    fun decodesWhatItEncodes() {
        val tree =
            Tree(
                listOf(
                    file("notes.txt")
                        .copy(size = 5, content = listOf(Id.hash("hello".toByteArray()))),
                    Node(
                        "DCIM",
                        NodeType.DIR,
                        time,
                        mode = Node.MODE_DIR or 493,
                        subtree = Id.hash(ByteArray(1)),
                    ),
                    Node(
                        "link",
                        NodeType.SYMLINK,
                        time,
                        mode = Node.MODE_SYMLINK or 511,
                        linkTarget = "notes.txt",
                    ),
                )
            )

        val decoded = Tree.decode(tree.encode())

        assertEquals(tree.nodes, decoded.nodes)
        assertEquals(tree.encode().decodeToString(), decoded.encode().decodeToString())
    }
}
