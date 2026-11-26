// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.repo.pack

import java.io.ByteArrayOutputStream
import java.security.SecureRandom
import org.prauga.cloud.repo.Compressor
import org.prauga.cloud.repo.Id
import org.prauga.cloud.repo.crypto.CryptoKey

class PackWriter(
    val type: BlobType,
    private val key: CryptoKey,
    private val compressor: Compressor,
    private val random: SecureRandom,
) {

    private val out = ByteArrayOutputStream()
    private val blobs = mutableListOf<PackedBlob>()
    private var finished = false

    val size: Int
        get() = out.size()

    val count: Int
        get() = blobs.size

    fun add(plaintext: ByteArray): PackedBlob {
        check(!finished) { "pack already finished" }
        val compressed = plaintext.isNotEmpty()
        val payload = if (compressed) compressor.compress(plaintext) else plaintext
        val sealed = key.seal(payload, random)
        val blob =
            PackedBlob(
                type = type,
                id = Id.hash(plaintext),
                offset = out.size().toLong(),
                length = sealed.size,
                uncompressedLength = if (compressed) plaintext.size else null,
            )
        out.write(sealed)
        blobs += blob
        return blob
    }

    fun finish(): Pack {
        check(!finished) { "pack already finished" }
        check(blobs.isNotEmpty()) { "pack has no blobs" }
        finished = true
        val header = key.seal(PackHeader.encode(blobs), random)
        check(header.size <= PackHeader.MAX_SIZE) { "pack header too large" }
        out.write(header)
        out.write(PackHeader.littleEndian(header.size))
        val data = out.toByteArray()
        return Pack(Id.hash(data), data, blobs.toList())
    }
}
