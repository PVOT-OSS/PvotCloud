// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.repo.crypto

import org.junit.Assert.assertArrayEquals
import org.junit.Test
import org.prauga.cloud.repo.hex

class Poly1305AesTest {

    private class Vector(
        val message: String,
        val r: String,
        val k: String,
        val nonce: String,
        val mac: String,
    )

    private val vectors =
        listOf(
            Vector(
                "f3f6",
                "851fc40c3467ac0be05cc20404f3f700",
                "ec074c835580741701425b623235add6",
                "fb447350c4e868c52ac3275cf9d4327e",
                "f4c633c3044fc145f84f335cb81953de",
            ),
            Vector(
                "",
                "a0f3080000f46400d0c7e9076c834403",
                "75deaa25c09f208e1dc4ce6b5cad3fbf",
                "61ee09218d29b0aaed7e154a2c5509cc",
                "dd3fab2251f11ac759f0887129cc2ee7",
            ),
            Vector(
                "663cea190ffb83d89593f3f476b6bc24d7e679107ea26adb8caf6652d0656136",
                "48443d0bb0d21109c89a100b5ce2c208",
                "6acb5f61a7176dd320c5c1eb2edcdc74",
                "ae212a55399729595dea458bc621ff0e",
                "0ee1c16bb73f0f4fd19881753c01cdbe",
            ),
            Vector(
                "ab0812724a7f1e342742cbed374d94d136c6b8795d45b3819830f2c04491faf0" +
                    "990c62e48b8018b2c3e4a0fa3134cb67fa83e158c994d961c4cb21095c1bf9",
                "12976a08c4426d0ce8a82407c4f48207",
                "e1a5668a4d5b66a5f68cc5424ed5982d",
                "9ae831e743978d3a23527c7128149e3a",
                "5154ad0d2cb26e01274fc51148491f1b",
            ),
        )

    @Test
    fun matchesBernsteinVectors() {
        for (vector in vectors) {
            val message = hex(vector.message)
            val mac =
                Poly1305Aes.mac(
                    message,
                    0,
                    message.size,
                    hex(vector.nonce),
                    hex(vector.k),
                    hex(vector.r),
                )

            assertArrayEquals(hex(vector.mac), mac)
        }
    }
}
