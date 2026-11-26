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
import org.prauga.cloud.repo.pack.BlobType
import org.prauga.cloud.repo.pack.Pack
import org.prauga.cloud.repo.pack.PackReader
import org.prauga.cloud.repo.pack.PackWriter
import org.prauga.cloud.repo.pack.PackedBlob

class Repository
private constructor(
    private val backend: Backend,
    private val masterKey: CryptoKey,
    private val compressor: Compressor,
    private val random: SecureRandom,
    val config: RepoConfig,
) {

    val chunkerPolynomial: Polynomial
        get() = Polynomial.parse(config.chunkerPolynomial)

    private val packReader = PackReader(masterKey, compressor)
    private var lastPack: Pair<Id, ByteArray>? = null

    fun newPackWriter(type: BlobType): PackWriter = PackWriter(type, masterKey, compressor, random)

    fun savePack(pack: Pack) {
        if (!backend.exists(FileType.PACK, pack.id.hex))
            backend.save(FileType.PACK, pack.id.hex, pack.data)
    }

    fun readPackHeader(packId: Id): List<PackedBlob> =
        packReader.readHeader(packId, loadPack(packId))

    fun readBlob(packId: Id, blob: PackedBlob): ByteArray =
        packReader.readBlob(packId, loadPack(packId), blob)

    fun verifyPack(packId: Id): List<PackedBlob> {
        val data = loadPack(packId)
        val blobs = packReader.readHeader(packId, data)
        blobs.forEach { packReader.readBlob(packId, data, it) }
        return blobs
    }

    fun saveUnpacked(type: FileType, plaintext: ByteArray): String {
        require(type != FileType.CONFIG && type != FileType.KEY && type != FileType.PACK) {
            "$type is not an unpacked file"
        }
        val encoded =
            if (config.version >= 2) byteArrayOf(UNPACKED_ZSTD) + compressor.compress(plaintext)
            else plaintext
        val sealed = masterKey.seal(encoded, random)
        val name = sha256(sealed).toHex()
        backend.save(type, name, sealed)
        return name
    }

    fun loadUnpacked(type: FileType, name: String): ByteArray {
        val sealed = backend.load(type, name)
        val plaintext = if (sha256(sealed).toHex() == name) masterKey.open(sealed) else null
        return plaintext?.let(::decodeUnpacked) ?: throw RepositoryException.Corrupted(type, name)
    }

    private fun decodeUnpacked(plaintext: ByteArray): ByteArray? {
        if (config.version < 2 || plaintext.isEmpty()) return plaintext
        return when (plaintext[0]) {
            JSON_OBJECT,
            JSON_ARRAY -> plaintext
            UNPACKED_ZSTD ->
                try {
                    compressor.decompress(
                        plaintext.copyOfRange(1, plaintext.size),
                        MAX_UNPACKED_SIZE,
                    )
                } catch (_: CompressionException) {
                    null
                }
            else -> null
        }
    }

    fun list(type: FileType): List<String> = backend.list(type)

    private fun loadPack(id: Id): ByteArray {
        lastPack?.let { (cachedId, data) -> if (cachedId == id) return data }
        val data = backend.load(FileType.PACK, id.hex)
        if (Id.hash(data) != id) throw RepositoryException.Corrupted(FileType.PACK, id.hex)
        lastPack = id to data
        return data
    }

    companion object {
        const val VERSION = 2
        private val SUPPORTED_VERSIONS = 1..2
        private const val ID_SIZE = 32
        private const val UNPACKED_ZSTD: Byte = 2
        private const val MAX_UNPACKED_SIZE = 128 * 1024 * 1024
        private const val JSON_OBJECT = '{'.code.toByte()
        private const val JSON_ARRAY = '['.code.toByte()

        fun create(
            backend: Backend,
            compressor: Compressor,
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
            return Repository(backend, masterKey, compressor, random, config)
        }

        fun open(
            backend: Backend,
            compressor: Compressor,
            password: String,
            random: SecureRandom = SecureRandom(),
        ): Repository {
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
            return Repository(backend, masterKey, compressor, random, config)
        }

        private fun unlockKey(keyFileBytes: ByteArray, password: String): CryptoKey? {
            val keyFile = decodeKeyFile(keyFileBytes)?.takeIf { it.kdf == Kdf.NAME } ?: return null
            val userKey =
                Kdf.deriveUserKey(
                    password,
                    keyFile.salt.decodeBase64(),
                    KdfParams(keyFile.n, keyFile.r, keyFile.p),
                )
            return openOrNull(userKey, keyFile.data.decodeBase64())?.let { masterJson ->
                val master =
                    ResticJson.decodeFromString(
                        MasterKeyJson.serializer(),
                        masterJson.decodeToString(),
                    )
                CryptoKey(
                    encryptionKey = master.encrypt.decodeBase64(),
                    macK = master.mac.k.decodeBase64(),
                    macR = master.mac.r.decodeBase64(),
                )
            }
        }

        private fun decodeKeyFile(bytes: ByteArray): KeyFile? =
            try {
                ResticJson.decodeFromString(KeyFile.serializer(), bytes.decodeToString())
            } catch (_: SerializationException) {
                null
            }

        private fun openOrNull(key: CryptoKey, sealed: ByteArray): ByteArray? =
            try {
                key.open(sealed)
            } catch (_: AuthenticationException) {
                null
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
