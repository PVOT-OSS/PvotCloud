// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.core.keys

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.security.KeyStore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KeystoreRecoveryPhraseStoreTest {

    private val file =
        File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "phrase-test")
    private val store = KeystoreRecoveryPhraseStore(file, ALIAS)

    @After
    fun cleanUp() {
        file.delete()
        KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(ALIAS)
    }

    @Test
    fun storesThePhraseEncrypted() = runBlocking {
        val words = RecoveryPhrase.generate()
        assertNull(store.load())

        store.save(words)

        assertTrue(store.exists())
        assertEquals(words, store.load())
        val stored = file.readBytes().toString(Charsets.ISO_8859_1)
        assertFalse(words.any { stored.contains(it) })
    }

    private companion object {
        const val ALIAS = "pvot_cloud_phrase_test"
    }
}
