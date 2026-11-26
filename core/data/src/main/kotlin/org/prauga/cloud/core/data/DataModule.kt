// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.cloud.core.data

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataModule {
    @Binds
    abstract fun backupStatusRepository(
        repository: SettingsBackupStatusRepository
    ): BackupStatusRepository

    @Binds
    abstract fun backupSetupRepository(
        repository: DefaultBackupSetupRepository
    ): BackupSetupRepository

    @Binds
    abstract fun connectedAppsRepository(
        repository: DefaultConnectedAppsRepository
    ): ConnectedAppsRepository
}

@Module
@InstallIn(SingletonComponent::class)
internal object DispatchersModule {
    @Provides @IoDispatcher fun io(): CoroutineDispatcher = Dispatchers.IO
}
