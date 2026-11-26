// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.core.keys

import cash.z.ecc.android.bip39.Mnemonics

object RecoveryPhrase {

    const val WORD_COUNT = 12

    fun generate(): List<String> =
        Mnemonics.MnemonicCode(Mnemonics.WordCount.COUNT_12).use { code ->
            code.words.map { String(it) }
        }

    fun isValid(words: List<String>): Boolean {
        if (words.size != WORD_COUNT) return false
        return try {
            Mnemonics.MnemonicCode(words.joinToString(" ")).use { it.validate() }
            true
        } catch (_: Mnemonics.ChecksumException) {
            false
        } catch (_: Mnemonics.InvalidWordException) {
            false
        } catch (_: Mnemonics.WordCountException) {
            false
        }
    }
}
