// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.backup

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.text.format.Formatter
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlin.coroutines.cancellation.CancellationException
import org.prauga.cloud.R
import org.prauga.cloud.core.data.BackupRunner
import org.prauga.cloud.core.model.RunProgress
import org.prauga.cloud.core.model.RunResult

@HiltWorker
class BackupWorker
@AssistedInject
constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val runner: BackupRunner,
) : CoroutineWorker(context, params) {

    private var lastUpdate = 0L

    override suspend fun doWork(): Result {
        setForeground(foregroundInfo(null))
        val result = runner.run { progress ->
            if (isStopped) throw CancellationException("backup stopped")
            val now = System.currentTimeMillis()
            if (now - lastUpdate >= UPDATE_INTERVAL_MS) {
                lastUpdate = now
                setProgressAsync(
                    workDataOf(
                        FILES to progress.files,
                        TOTAL_FILES to progress.totalFiles,
                        BYTES to progress.bytes,
                        TOTAL_BYTES to progress.totalBytes,
                    )
                )
                setForegroundAsync(foregroundInfo(progress))
            }
        }
        return when (result) {
            is RunResult.Success -> Result.success()
            is RunResult.Failure -> Result.failure(workDataOf(FAILURE to result.reason.name))
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo = foregroundInfo(null)

    private fun foregroundInfo(progress: RunProgress?): ForegroundInfo {
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                applicationContext.getString(R.string.notification_channel_backups),
                NotificationManager.IMPORTANCE_LOW,
            )
        )
        val builder =
            NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_t_shield_check)
                .setContentTitle(applicationContext.getString(R.string.notification_backing_up))
                .setOngoing(true)
                .setOnlyAlertOnce(true)
        if (progress == null || progress.totalBytes <= 0) {
            builder.setProgress(0, 0, true)
        } else {
            builder
                .setProgress(
                    PROGRESS_MAX,
                    (progress.bytes * PROGRESS_MAX / progress.totalBytes).toInt(),
                    false,
                )
                .setContentText(
                    applicationContext.resources.getQuantityString(
                        R.plurals.notification_progress,
                        progress.totalFiles,
                        progress.files,
                        progress.totalFiles,
                        Formatter.formatShortFileSize(applicationContext, progress.bytes),
                        Formatter.formatShortFileSize(applicationContext, progress.totalBytes),
                    )
                )
        }
        val notification = builder.build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        const val FILES = "files"
        const val TOTAL_FILES = "total_files"
        const val BYTES = "bytes"
        const val TOTAL_BYTES = "total_bytes"
        const val FAILURE = "failure"
        private const val CHANNEL_ID = "backups"
        private const val NOTIFICATION_ID = 1
        private const val PROGRESS_MAX = 1000
        private const val UPDATE_INTERVAL_MS = 500L
    }
}
