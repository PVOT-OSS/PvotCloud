// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.repo

import com.github.luben.zstd.Zstd

class ZstdCompressor : Compressor {

    override fun compress(data: ByteArray): ByteArray = Zstd.compress(data)

    override fun decompress(data: ByteArray): ByteArray =
        Zstd.decompress(data, Zstd.getFrameContentSize(data).toInt())
}
