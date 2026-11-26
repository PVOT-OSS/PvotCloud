// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.core.keys

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecoveryPhraseTest {

    @Test
    fun generatesTwelveValidWords() {
        val words = RecoveryPhrase.generate()

        assertEquals(12, words.size)
        assertTrue(RecoveryPhrase.isValid(words))
    }

    @Test
    fun everyPhraseIsNew() {
        assertNotEquals(RecoveryPhrase.generate(), RecoveryPhrase.generate())
    }

    @Test
    fun acceptsTheBip39TestVector() {
        val words =
            "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about"

        assertTrue(RecoveryPhrase.isValid(words.split(" ")))
    }

    @Test
    fun rejectsABadChecksumAndUnknownWords() {
        val badChecksum = List(12) { "abandon" }
        val unknownWord = List(11) { "abandon" } + "pvotcloud"

        assertFalse(RecoveryPhrase.isValid(badChecksum))
        assertFalse(RecoveryPhrase.isValid(unknownWord))
        assertFalse(RecoveryPhrase.isValid(listOf("abandon")))
    }
}
