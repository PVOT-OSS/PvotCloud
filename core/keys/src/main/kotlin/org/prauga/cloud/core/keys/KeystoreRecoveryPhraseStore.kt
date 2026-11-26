// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.core.keys

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class KeystoreRecoveryPhraseStore
internal constructor(private val file: File, private val alias: String) : RecoveryPhraseStore {

    @Inject
    constructor(
        @ApplicationContext context: Context
    ) : this(File(context.noBackupFilesDir, FILE_NAME), KEY_ALIAS)

    override suspend fun save(words: List<String>) =
        withContext(Dispatchers.IO) {
            require(RecoveryPhrase.isValid(words)) { "not a valid recovery phrase" }
            val cipher =
                Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key()) }
            val sealed =
                cipher.iv + cipher.doFinal(words.joinToString(" ").toByteArray(Charsets.UTF_8))
            val temp = File(file.parentFile, "${file.name}.tmp")
            temp.writeBytes(sealed)
            check(temp.renameTo(file)) { "could not store the recovery phrase" }
        }

    override suspend fun load(): List<String>? =
        withContext(Dispatchers.IO) {
            if (!file.exists()) return@withContext null
            val sealed = file.readBytes()
            val cipher =
                Cipher.getInstance(TRANSFORMATION).apply {
                    init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, sealed, 0, IV_SIZE))
                }
            String(cipher.doFinal(sealed, IV_SIZE, sealed.size - IV_SIZE), Charsets.UTF_8)
                .split(" ")
        }

    override suspend fun exists(): Boolean = withContext(Dispatchers.IO) { file.exists() }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(alias, null) as? SecretKey)?.let {
            return it
        }
        val spec =
            KeyGenParameterSpec.Builder(
                    alias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_BITS)
                .build()
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            .apply { init(spec) }
            .generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "pvot_cloud_recovery_phrase"
        const val FILE_NAME = "recovery_phrase"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_BITS = 256
        const val TAG_BITS = 128
        const val IV_SIZE = 12
    }
}
