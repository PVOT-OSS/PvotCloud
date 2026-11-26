// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.repo.crypto

import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import org.bouncycastle.crypto.macs.Poly1305
import org.bouncycastle.crypto.params.KeyParameter

internal object Poly1305Aes {

    fun mac(
        data: ByteArray,
        offset: Int,
        length: Int,
        nonce: ByteArray,
        k: ByteArray,
        r: ByteArray,
    ): ByteArray {
        val s =
            Cipher.getInstance("AES/ECB/NoPadding").run {
                init(Cipher.ENCRYPT_MODE, SecretKeySpec(k, "AES"))
                doFinal(nonce)
            }
        val poly = Poly1305()
        poly.init(KeyParameter(r + s))
        poly.update(data, offset, length)
        return ByteArray(CryptoKey.MAC_SIZE).also { poly.doFinal(it, 0) }
    }
}
