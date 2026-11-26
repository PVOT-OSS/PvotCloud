// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.cloud.sample.vault

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import org.json.JSONObject
import org.prauga.cloud.sdk.agent.Credential

internal class VaultStore(context: Context) {
    private val file = File(context.filesDir, "vault.txt")

    fun all(): List<Credential> =
        if (!file.isFile) emptyList()
        else file.readLines().filter(String::isNotBlank).map(::decrypt)

    fun add(credential: Credential) {
        file.appendText(encrypt(credential) + "\n")
    }

    fun replaceAll(credentials: List<Credential>) {
        file.writeText(credentials.joinToString("") { encrypt(it) + "\n" })
    }

    private fun encrypt(credential: Credential): String {
        val json =
            JSONObject()
                .put("id", credential.id)
                .put("label", credential.label)
                .put("username", credential.username)
                .put("secret", credential.secret)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key()) }
        val sealed = cipher.iv + cipher.doFinal(json.toString().toByteArray())
        return Base64.encodeToString(sealed, Base64.NO_WRAP)
    }

    private fun decrypt(line: String): Credential {
        val sealed = Base64.decode(line, Base64.NO_WRAP)
        val cipher =
            Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, sealed, 0, IV_BYTES))
            }
        val json = JSONObject(String(cipher.doFinal(sealed, IV_BYTES, sealed.size - IV_BYTES)))
        return Credential(
            json.getString("id"),
            json.getString("label"),
            json.getString("username"),
            json.getString("secret"),
        )
    }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        return (keyStore.getKey(ALIAS, null) as? SecretKey)
            ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
                .apply {
                    init(
                        KeyGenParameterSpec.Builder(
                                ALIAS,
                                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                            )
                            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                            .build()
                    )
                }
                .generateKey()
    }

    private companion object {
        const val KEYSTORE = "AndroidKeyStore"
        const val ALIAS = "vault"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val TAG_BITS = 128
        const val IV_BYTES = 12
    }
}
