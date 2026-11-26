// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.core.data

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.prauga.cloud.core.datastore.BackupSettingsDataSource
import org.prauga.cloud.core.keys.RecoveryPhraseStore
import org.prauga.cloud.core.model.AppRestoreOutcome
import org.prauga.cloud.core.model.AppRestoreResult
import org.prauga.cloud.core.model.ConnectedApp
import org.prauga.cloud.core.model.IncludedApp
import org.prauga.cloud.repo.Id
import org.prauga.cloud.repo.Repository
import org.prauga.cloud.repo.RepositoryException
import org.prauga.cloud.repo.ZstdCompressor
import org.prauga.cloud.repo.index.MasterIndex
import org.prauga.cloud.repo.restore.Restore
import org.prauga.cloud.repo.snapshot.Snapshots
import org.prauga.cloud.repo.tree.NodeType
import org.prauga.cloud.sdk.ResultCode
import org.prauga.cloud.sdk.host.AgentDiscovery
import org.prauga.cloud.sdk.host.AppBackup
import org.prauga.cloud.storage.SafBackend
import org.prauga.cloud.storage.SafDocumentTree

@Singleton
class DefaultConnectedAppsRepository
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val settings: BackupSettingsDataSource,
    private val phraseStore: RecoveryPhraseStore,
    private val discovery: AgentDiscovery,
    private val appBackup: AppBackup,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ConnectedAppsRepository {

    private val refreshes = MutableStateFlow(0)

    override val apps: Flow<List<ConnectedApp>> =
        combine(settings.settings, refreshes) { current, _ ->
                val included = current.includedApps.map { it.packageName }.toSet()
                discovery.agents().map { app ->
                    ConnectedApp(
                        packageName = app.packageName,
                        label = app.label,
                        included = app.packageName in included,
                        lastBackup = current.appRecords[app.packageName],
                    )
                }
            }
            .flowOn(ioDispatcher)

    override fun refresh() {
        refreshes.value++
    }

    override suspend fun isApproved(packageName: String): Boolean =
        withContext(ioDispatcher) { appBackup.isApproved(packageName) }

    override suspend fun include(packageName: String) {
        val certificate =
            withContext(ioDispatcher) { discovery.app(packageName)?.certificates?.minOrNull() }
        if (certificate != null) settings.include(IncludedApp(packageName, certificate))
    }

    override suspend fun exclude(packageName: String) {
        settings.exclude(packageName)
    }

    override suspend fun restore(packageName: String): AppRestoreResult =
        withContext(ioDispatcher) {
            val current = settings.settings.first()
            val location = current.location
            val words = phraseStore.load()
            if (location == null || words == null) {
                AppRestoreResult(AppRestoreOutcome.NOT_SET_UP)
            } else {
                try {
                    restoreFrom(Uri.parse(location.uri), words.joinToString(" "), packageName)
                } catch (e: IOException) {
                    AppRestoreResult(AppRestoreOutcome.FAILED, e.message)
                } catch (e: SecurityException) {
                    AppRestoreResult(AppRestoreOutcome.FAILED, e.message)
                } catch (e: RepositoryException) {
                    AppRestoreResult(AppRestoreOutcome.FAILED, e.message)
                }
            }
        }

    private suspend fun restoreFrom(
        treeUri: Uri,
        password: String,
        packageName: String,
    ): AppRestoreResult {
        val backend = SafBackend(SafDocumentTree(context.contentResolver, treeUri))
        val repository = Repository.open(backend, ZstdCompressor(), password)
        val restore = Restore(repository, MasterIndex.load(repository))
        val files =
            Snapshots(repository).list().asReversed().firstNotNullOfOrNull { entry ->
                appFiles(restore, entry.snapshot.tree, packageName)
            } ?: return AppRestoreResult(AppRestoreOutcome.NO_BACKUP)
        val result = appBackup.restore(packageName, files, File(context.cacheDir, RESTORE_DIR))
        val outcome =
            when (result.code) {
                ResultCode.OK -> AppRestoreOutcome.RESTORED
                ResultCode.NOT_CONSENTED,
                ResultCode.UNTRUSTED_PEER -> AppRestoreOutcome.NOT_APPROVED
                else -> AppRestoreOutcome.FAILED
            }
        return AppRestoreResult(outcome, result.detail)
    }

    private fun appFiles(
        restore: Restore,
        tree: Id,
        packageName: String,
    ): Map<String, () -> InputStream>? {
        val apps = restore.entries(tree).firstOrNull { it.path == AppBackup.APPS }?.node?.subtree
        val files = apps?.let { id ->
            restore
                .entries(id)
                .filter { it.path.startsWith("$packageName/") && it.node.type == NodeType.FILE }
                .associate { entry -> entry.node.name to { restore.open(entry.node) } }
        }
        return files?.takeIf { it.isNotEmpty() }
    }

    private companion object {
        const val RESTORE_DIR = "sdk-restore"
    }
}
