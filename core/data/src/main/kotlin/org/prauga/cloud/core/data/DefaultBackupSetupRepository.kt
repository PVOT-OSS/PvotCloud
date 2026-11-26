// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.core.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.prauga.cloud.core.datastore.BackupSettingsDataSource
import org.prauga.cloud.core.model.BackupCategory
import org.prauga.cloud.core.model.BackupSettings
import org.prauga.cloud.core.model.SavedLocation

class DefaultBackupSetupRepository
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val dataSource: BackupSettingsDataSource,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : BackupSetupRepository {

    override val settings: Flow<BackupSettings> = dataSource.settings

    override suspend fun chooseLocation(treeUri: String): SavedLocation? =
        withContext(ioDispatcher) {
            val uri = Uri.parse(treeUri)
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            } catch (_: SecurityException) {
                return@withContext null
            }
            val location = SavedLocation(treeUri, displayName(uri))
            dataSource.setLocation(location)
            location
        }

    override suspend fun setCategories(categories: Set<BackupCategory>) =
        dataSource.setCategories(categories)

    override suspend fun confirmPhrase() = dataSource.setPhraseConfirmed(true)

    private fun displayName(tree: Uri): String {
        val documentId = DocumentsContract.getTreeDocumentId(tree)
        val document = DocumentsContract.buildDocumentUriUsingTree(tree, documentId)
        val queried =
            context.contentResolver
                .query(
                    document,
                    arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                    null,
                    null,
                    null,
                )
                ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
        return queried ?: documentId.substringAfterLast(':').ifEmpty { documentId }
    }
}
