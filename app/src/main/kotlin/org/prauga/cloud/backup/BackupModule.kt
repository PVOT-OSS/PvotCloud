// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.backup

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.prauga.cloud.core.data.BackupScheduler

@Module
@InstallIn(SingletonComponent::class)
internal abstract class BackupModule {
    @Binds abstract fun backupScheduler(scheduler: WorkManagerBackupScheduler): BackupScheduler
}
