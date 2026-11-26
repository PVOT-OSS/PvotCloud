// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.repo

import java.security.SecureRandom
import java.time.Clock
import java.time.OffsetDateTime
import kotlinx.serialization.SerializationException
import org.prauga.cloud.repo.chunker.Polynomial
import org.prauga.cloud.repo.crypto.AuthenticationException
import org.prauga.cloud.repo.crypto.CryptoKey
import org.prauga.cloud.repo.crypto.Kdf
import org.prauga.cloud.repo.crypto.KdfParams

class Repository private constructor(val config: RepoConfig) {

    val chunkerPolynomial: Polynomial
        get() = Polynomial.parse(config.chunkerPolynomial)

    companion object {
        const val VERSION = 2
        private val SUPPORTED_VERSIONS = 1..2
        private const val ID_SIZE = 32

        fun create(
            backend: Backend,
            password: String,
            hostname: String,
            username: String,
            params: KdfParams = KdfParams.DEFAULT,
            random: SecureRandom = SecureRandom(),
            clock: Clock = Clock.systemUTC(),
        ): Repository {
            if (backend.exists(FileType.CONFIG, RepositoryLayout.CONFIG_NAME)) {
                throw RepositoryException.AlreadyExists()
            }
            val masterKey = CryptoKey.random(random)
            val config =
                RepoConfig(
                    version = VERSION,
                    id = ByteArray(ID_SIZE).also(random::nextBytes).toHex(),
                    chunkerPolynomial = Polynomial.random(random).toHex(),
                )
            saveKey(backend, masterKey, password, hostname, username, params, random, clock)
            val configJson =
                ResticJson.encodeToString(RepoConfig.serializer(), config).toByteArray()
            backend.save(
                FileType.CONFIG,
                RepositoryLayout.CONFIG_NAME,
                masterKey.seal(configJson, random),
            )
            return Repository(config)
        }

        fun open(backend: Backend, password: String): Repository {
            val masterKey =
                backend.list(FileType.KEY).firstNotNullOfOrNull { name ->
                    unlockKey(backend.load(FileType.KEY, name), password)
                } ?: throw RepositoryException.WrongPassword()
            val configBytes =
                masterKey.open(backend.load(FileType.CONFIG, RepositoryLayout.CONFIG_NAME))
            val config =
                ResticJson.decodeFromString(RepoConfig.serializer(), configBytes.decodeToString())
            if (config.version !in SUPPORTED_VERSIONS)
                throw RepositoryException.Unsupported(config.version)
            return Repository(config)
        }

        private fun unlockKey(keyFileBytes: ByteArray, password: String): CryptoKey? {
            val keyFile =
                try {
                    ResticJson.decodeFromString(KeyFile.serializer(), keyFileBytes.decodeToString())
                } catch (_: SerializationException) {
                    return null
                }
            if (keyFile.kdf != Kdf.NAME) return null
            val userKey =
                Kdf.deriveUserKey(
                    password,
                    keyFile.salt.decodeBase64(),
                    KdfParams(keyFile.n, keyFile.r, keyFile.p),
                )
            val masterJson =
                try {
                    userKey.open(keyFile.data.decodeBase64())
                } catch (_: AuthenticationException) {
                    return null
                }
            val master =
                ResticJson.decodeFromString(MasterKeyJson.serializer(), masterJson.decodeToString())
            return CryptoKey(
                encryptionKey = master.encrypt.decodeBase64(),
                macK = master.mac.k.decodeBase64(),
                macR = master.mac.r.decodeBase64(),
            )
        }

        private fun saveKey(
            backend: Backend,
            masterKey: CryptoKey,
            password: String,
            hostname: String,
            username: String,
            params: KdfParams,
            random: SecureRandom,
            clock: Clock,
        ) {
            val salt = ByteArray(Kdf.SALT_SIZE).also(random::nextBytes)
            val userKey = Kdf.deriveUserKey(password, salt, params)
            val masterJson =
                MasterKeyJson(
                    mac =
                        MasterKeyJson.Mac(k = masterKey.macK.base64(), r = masterKey.macR.base64()),
                    encrypt = masterKey.encryptionKeyCopy().base64(),
                )
            val keyFile =
                KeyFile(
                    created = OffsetDateTime.now(clock).toString(),
                    username = username,
                    hostname = hostname,
                    kdf = Kdf.NAME,
                    n = params.n,
                    r = params.r,
                    p = params.p,
                    salt = salt.base64(),
                    data =
                        userKey
                            .seal(
                                ResticJson.encodeToString(MasterKeyJson.serializer(), masterJson)
                                    .toByteArray(),
                                random,
                            )
                            .base64(),
                )
            val bytes = ResticJson.encodeToString(KeyFile.serializer(), keyFile).toByteArray()
            backend.save(FileType.KEY, sha256(bytes).toHex(), bytes)
        }
    }
}
