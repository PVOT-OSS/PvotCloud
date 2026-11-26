// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.sdk.host

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import org.prauga.cloud.sdk.BackupContract
import org.prauga.cloud.sdk.IBackupProvider
import org.prauga.cloud.sdk.ProviderInfo

class BackupProviderService : Service() {

    private val binder =
        object : IBackupProvider.Stub() {
            override fun contractVersion(): Int = BackupContract.VERSION

            override fun describe(): Bundle =
                ProviderInfo(
                        contractVersion = BackupContract.VERSION,
                        displayName = applicationInfo.loadLabel(packageManager).toString(),
                        capabilities = setOf(BackupContract.CAPABILITY_BACKUP),
                    )
                    .toBundle()
        }

    override fun onBind(intent: Intent): IBinder? = binder.takeIf {
        intent.action == BackupContract.ACTION_PROVIDER
    }
}
