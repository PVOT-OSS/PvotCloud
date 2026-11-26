// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.backup

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.prauga.cloud.core.data.BackupScheduler
import org.prauga.cloud.core.model.RunProgress

class WorkManagerBackupScheduler @Inject constructor(@ApplicationContext context: Context) :
    BackupScheduler {

    private val workManager = WorkManager.getInstance(context)

    override val running: Flow<RunProgress?> =
        workManager.getWorkInfosForUniqueWorkFlow(WORK_NAME).map { infos ->
            val info = infos.firstOrNull { !it.state.isFinished } ?: return@map null
            val data = info.progress
            if (info.state == WorkInfo.State.RUNNING || info.state == WorkInfo.State.ENQUEUED) {
                RunProgress(
                    files = data.getInt(BackupWorker.FILES, 0),
                    totalFiles = data.getInt(BackupWorker.TOTAL_FILES, 0),
                    bytes = data.getLong(BackupWorker.BYTES, 0),
                    totalBytes = data.getLong(BackupWorker.TOTAL_BYTES, 0),
                )
            } else {
                null
            }
        }

    override fun backUpNow() {
        val request =
            OneTimeWorkRequestBuilder<BackupWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
        workManager.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }

    private companion object {
        const val WORK_NAME = "backup_now"
    }
}
