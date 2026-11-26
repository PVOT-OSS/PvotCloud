// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.core.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.time.Instant
import java.time.OffsetDateTime
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.prauga.cloud.core.datastore.BackupSettingsDataSource
import org.prauga.cloud.core.keys.RecoveryPhraseStore
import org.prauga.cloud.core.model.BackupCategory
import org.prauga.cloud.core.model.BackupFailure
import org.prauga.cloud.core.model.CategoryCount
import org.prauga.cloud.core.model.RunProgress
import org.prauga.cloud.core.model.RunResult
import org.prauga.cloud.repo.FileType
import org.prauga.cloud.repo.Repository
import org.prauga.cloud.repo.RepositoryException
import org.prauga.cloud.repo.RepositoryLayout
import org.prauga.cloud.repo.ZstdCompressor
import org.prauga.cloud.repo.backup.Backup
import org.prauga.cloud.repo.index.MasterIndex
import org.prauga.cloud.repo.source.SourceDirectory
import org.prauga.cloud.repo.source.SourceEntry
import org.prauga.cloud.sources.MediaStoreSource
import org.prauga.cloud.storage.SafBackend
import org.prauga.cloud.storage.SafDocumentTree

class BackupRunner
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val settings: BackupSettingsDataSource,
    private val phraseStore: RecoveryPhraseStore,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    suspend fun run(onProgress: (RunProgress) -> Unit): RunResult =
        withContext(ioDispatcher) {
            val current = settings.settings.first()
            val location = current.location
            val words = phraseStore.load()
            if (location == null || words == null || !current.phraseConfirmed) {
                return@withContext RunResult.Failure(BackupFailure.NOT_SET_UP)
            }
            val failure =
                try {
                    return@withContext backUp(
                        Uri.parse(location.uri),
                        words.joinToString(" "),
                        current.categories,
                        onProgress,
                    )
                } catch (_: SecurityException) {
                    BackupFailure.LOCATION_UNAVAILABLE
                } catch (_: IOException) {
                    BackupFailure.LOCATION_UNAVAILABLE
                } catch (_: RepositoryException.WrongPassword) {
                    BackupFailure.WRONG_PHRASE
                } catch (_: RepositoryException) {
                    BackupFailure.DAMAGED_REPOSITORY
                }
            settings.recordFailure(Instant.now())
            RunResult.Failure(failure)
        }

    private suspend fun backUp(
        treeUri: Uri,
        password: String,
        categories: Set<BackupCategory>,
        onProgress: (RunProgress) -> Unit,
    ): RunResult {
        val backend = SafBackend(SafDocumentTree(context.contentResolver, treeUri))
        val compressor = ZstdCompressor()
        val repository =
            if (backend.exists(FileType.CONFIG, RepositoryLayout.CONFIG_NAME)) {
                Repository.open(backend, compressor, password)
            } else {
                Repository.create(
                    backend,
                    compressor,
                    password,
                    hostname = Build.MODEL,
                    username = USERNAME,
                )
            }
        val roots = mutableListOf<SourceEntry>()
        val counts = mutableMapOf<BackupCategory, CategoryCount>()
        if (BackupCategory.PHOTOS in categories && canReadMedia()) {
            val media = MediaStoreSource(context)
            val items = media.items()
            roots += media.tree(items)
            counts[BackupCategory.PHOTOS] = CategoryCount(items.size, items.sumOf { it.size })
        }
        val totalFiles = counts.values.sumOf { it.items }
        val totalBytes = counts.values.sumOf { it.bytes }
        val result =
            Backup(repository, MasterIndex.load(repository))
                .run(
                    root = Root(roots),
                    hostname = Build.MODEL,
                    paths = listOf("/"),
                    tags = listOf(TAG),
                    programVersion = TAG,
                    progress = {
                        onProgress(RunProgress(it.files, totalFiles, it.bytes, totalBytes))
                    },
                )
        settings.recordSuccess(Instant.now(), counts)
        return RunResult.Success(result.files, result.bytes)
    }

    private fun canReadMedia(): Boolean {
        fun granted(permission: String) =
            context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
                granted(Manifest.permission.READ_MEDIA_IMAGES) ||
                    granted(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                granted(Manifest.permission.READ_MEDIA_IMAGES)
            else -> granted(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private class Root(private val entries: List<SourceEntry>) : SourceDirectory {
        override val name: String = ""
        override val modTime: OffsetDateTime = OffsetDateTime.now()

        override fun children(): List<SourceEntry> = entries
    }

    private companion object {
        const val TAG = "pvot-cloud"
        const val USERNAME = "pvot-cloud"
    }
}
