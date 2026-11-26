// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.repo

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.readBytes
import kotlin.io.path.writeText
import kotlin.random.Random
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.prauga.cloud.repo.backup.Backup
import org.prauga.cloud.repo.crypto.KdfParams
import org.prauga.cloud.repo.index.MasterIndex
import org.prauga.cloud.repo.pack.BlobType
import org.prauga.cloud.repo.snapshot.Snapshots
import org.prauga.cloud.repo.source.LocalDirectory
import org.prauga.cloud.repo.tree.Tree

class ResticInteropTest {

    @get:Rule val temp = TemporaryFolder()

    private val restic: String? = System.getenv("RESTIC_BIN")
    private val compressor = ZstdCompressor()

    @Before
    fun requireRestic() {
        assumeTrue(
            "set RESTIC_BIN to run restic interop tests",
            restic != null && File(restic).canExecute(),
        )
    }

    @Test
    fun opensARepositoryResticCreated() {
        val repo = temp.newFolder("restic-made").toPath()
        restic(repo, "init")
        val source =
            temp.newFolder("source").toPath().also {
                it.resolve("hello.txt").writeText("hello from restic")
            }
        restic(repo, "backup", source.toString())

        val repository = Repository.open(FileSystemBackend(repo), compressor, PASSWORD)

        val resticConfig = Json.parseToJsonElement(restic(repo, "cat", "config")).jsonObject
        assertEquals(resticConfig.getValue("id").jsonPrimitive.content, repository.config.id)
        val snapshot = repository.list(FileType.SNAPSHOT).single()
        val json =
            Json.parseToJsonElement(
                repository.loadUnpacked(FileType.SNAPSHOT, snapshot).decodeToString()
            )
        assertEquals(
            source.toString(),
            json.jsonObject.getValue("paths").jsonArray.single().jsonPrimitive.content,
        )
    }

    @Test
    fun readsEveryBlobResticWrote() {
        val repo = temp.newFolder("restic-blobs").toPath()
        restic(repo, "init")
        val source = temp.newFolder("blobs").toPath()
        source
            .resolve("random.bin")
            .toFile()
            .writeBytes(ByteArray(3 * 1024 * 1024).also(Random(7)::nextBytes))
        source.resolve("text.txt").writeText("pvot ".repeat(100_000))
        restic(repo, "backup", source.toString())

        val repository = Repository.open(FileSystemBackend(repo), compressor, PASSWORD)
        val index = MasterIndex.load(repository)

        var blobs = 0
        for (pack in repository.list(FileType.PACK).map(::Id)) {
            for (blob in repository.readPackHeader(pack)) {
                assertEquals(pack, index.lookup(blob.type, blob.id)?.packId)
                assertEquals(blob.id, Id.hash(repository.readBlob(pack, blob)))
                blobs++
            }
        }
        assertEquals(index.size, blobs)
        assertTrue(index.size > 2)
    }

    @Test
    fun reencodesResticTreesByteForByte() {
        val repo = temp.newFolder("restic-trees").toPath()
        restic(repo, "init")
        val source = temp.newFolder("tree source").toPath()
        source.resolve("plain.txt").writeText("plain")
        source.resolve("say \"hi\" <&> é 😀.txt").writeText("tricky name")
        source.resolve("empty").writeText("")
        Files.createDirectories(source.resolve("DCIM/Camera"))
            .resolve("IMG_0001.jpg")
            .writeText("jpeg")
        Files.createSymbolicLink(source.resolve("link"), Path.of("plain.txt"))
        restic(repo, "backup", source.toString())

        val repository = Repository.open(FileSystemBackend(repo), compressor, PASSWORD)
        val index = MasterIndex.load(repository)
        val snapshot = Snapshots(repository).list().single().snapshot

        val pending = ArrayDeque(listOf(snapshot.tree))
        var trees = 0
        while (pending.isNotEmpty()) {
            val id = pending.removeFirst()
            val location = checkNotNull(index.lookup(BlobType.TREE, id)) { "tree $id missing" }
            val tree = Tree.decode(repository.readBlob(location.packId, location.blob))
            assertEquals(id, Id.hash(tree.encode()))
            tree.nodes.mapNotNullTo(pending) { it.subtree }
            trees++
        }
        assertTrue(trees >= 3)
    }

    @Test
    fun resticOpensARepositoryWeCreated() {
        val repo = temp.newFolder("pvot-made").toPath()
        val repository =
            Repository.create(
                FileSystemBackend(repo),
                compressor,
                PASSWORD,
                hostname = "phone",
                username = "user",
                params = KdfParams(n = 16384, r = 8, p = 1),
            )
        RepositoryLayout.directories().forEach { Files.createDirectories(repo.resolve(it)) }

        val resticConfig = Json.parseToJsonElement(restic(repo, "cat", "config")).jsonObject

        assertEquals(repository.config.id, resticConfig.getValue("id").jsonPrimitive.content)
        assertEquals(2, resticConfig.getValue("version").jsonPrimitive.content.toInt())
        assertTrue(restic(repo, "key", "list").contains("phone"))
        restic(repo, "check")
    }

    @Test
    fun resticVerifiesAndRestoresOurBackup() {
        val repo = temp.newFolder("pvot-backup").toPath()
        val repository =
            Repository.create(
                FileSystemBackend(repo),
                compressor,
                PASSWORD,
                "phone",
                "user",
                KdfParams(16384, 8, 1),
            )
        RepositoryLayout.directories().forEach { Files.createDirectories(repo.resolve(it)) }
        val source = temp.newFolder("phone").toPath()
        val photos = Files.createDirectories(source.resolve("DCIM/Camera"))
        photos
            .resolve("IMG_0001.jpg")
            .toFile()
            .writeBytes(ByteArray(6 * 1024 * 1024).also(Random(11)::nextBytes))
        source.resolve("say \"hi\" <&> é.txt").writeText("tricky name")
        source.resolve("empty").writeText("")
        Files.createSymbolicLink(source.resolve("latest"), Path.of("DCIM/Camera/IMG_0001.jpg"))

        Backup(repository, MasterIndex.empty()).run(LocalDirectory(source), "phone", listOf("/"))

        restic(repo, "check", "--read-data")
        assertTrue(restic(repo, "snapshots").contains("phone"))
        val target = temp.newFolder("restored").toPath()
        restic(repo, "restore", "latest", "--target", target.toString())
        for (relative in listOf("DCIM/Camera/IMG_0001.jpg", "say \"hi\" <&> é.txt", "empty")) {
            assertArrayEquals(
                relative,
                source.resolve(relative).readBytes(),
                target.resolve(relative).readBytes(),
            )
        }
        assertEquals(
            Path.of("DCIM/Camera/IMG_0001.jpg"),
            Files.readSymbolicLink(target.resolve("latest")),
        )
    }

    private fun restic(repo: Path, vararg args: String): String {
        val process =
            ProcessBuilder(
                    listOf(checkNotNull(restic), "--repo", repo.toString(), "--no-cache") + args
                )
                .redirectErrorStream(true)
                .apply { environment()["RESTIC_PASSWORD"] = PASSWORD }
                .start()
        val output = process.inputStream.bufferedReader().readText()
        check(process.waitFor(2, TimeUnit.MINUTES)) { "restic ${args.first()} timed out" }
        check(process.exitValue() == 0) { "restic ${args.joinToString(" ")} failed:\n$output" }
        return output
    }

    private companion object {
        const val PASSWORD =
            "abandon ability able about above absent absorb abstract absurd abuse access accident"
    }
}
