// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.repo.crypto

import org.bouncycastle.crypto.generators.SCrypt

internal object Kdf {

    const val NAME = "scrypt"
    const val SALT_SIZE = 64

    fun deriveUserKey(password: String, salt: ByteArray, params: KdfParams): CryptoKey {
        val size = CryptoKey.ENCRYPTION_KEY_SIZE + 2 * CryptoKey.MAC_PART_SIZE
        val derived =
            SCrypt.generate(
                password.toByteArray(Charsets.UTF_8),
                salt,
                params.n,
                params.r,
                params.p,
                size,
            )
        val macStart = CryptoKey.ENCRYPTION_KEY_SIZE
        return CryptoKey(
            encryptionKey = derived.copyOfRange(0, macStart),
            macK = derived.copyOfRange(macStart, macStart + CryptoKey.MAC_PART_SIZE),
            macR = derived.copyOfRange(macStart + CryptoKey.MAC_PART_SIZE, size),
        )
    }
}
