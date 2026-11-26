// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.repo

@JvmInline
value class Id(val hex: String) {

    init {
        require(isValid(hex)) { "invalid id $hex" }
    }

    internal fun bytes(): ByteArray =
        ByteArray(LENGTH / 2) { hex.substring(it * 2, it * 2 + 2).toInt(HEX_RADIX).toByte() }

    override fun toString(): String = hex

    companion object {
        private const val LENGTH = 64
        private const val HEX_RADIX = 16

        fun hash(data: ByteArray): Id = Id(sha256(data).toHex())

        fun parseOrNull(hex: String): Id? = if (isValid(hex)) Id(hex) else null

        private fun isValid(hex: String): Boolean =
            hex.length == LENGTH && hex.all { it in '0'..'9' || it in 'a'..'f' }

        internal fun of(digest: ByteArray): Id = Id(digest.toHex())
    }
}
