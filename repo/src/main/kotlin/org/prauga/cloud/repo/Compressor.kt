// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.repo

interface Compressor {

    fun compress(data: ByteArray): ByteArray

    fun decompress(data: ByteArray, maxSize: Int): ByteArray
}
