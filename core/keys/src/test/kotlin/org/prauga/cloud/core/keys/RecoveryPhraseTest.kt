// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.core.keys

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isFalse
import assertk.assertions.isNotEqualTo
import assertk.assertions.isTrue
import org.junit.Test

class RecoveryPhraseTest {

    @Test
    fun generatesTwelveValidWords() {
        val words = RecoveryPhrase.generate()

        assertThat(words).hasSize(12)
        assertThat(RecoveryPhrase.isValid(words)).isTrue()
    }

    @Test
    fun everyPhraseIsNew() {
        assertThat(RecoveryPhrase.generate()).isNotEqualTo(RecoveryPhrase.generate())
    }

    @Test
    fun acceptsTheBip39TestVector() {
        val words =
            "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"

        assertThat(RecoveryPhrase.isValid(words.split(" "))).isTrue()
    }

    @Test
    fun rejectsABadChecksumAndUnknownWords() {
        val badChecksum = List(12) { "abandon" }
        val unknownWord = List(11) { "abandon" } + "pvotcloud"

        assertThat(RecoveryPhrase.isValid(badChecksum)).isFalse()
        assertThat(RecoveryPhrase.isValid(unknownWord)).isFalse()
        assertThat(RecoveryPhrase.isValid(listOf("abandon"))).isFalse()
    }
}
