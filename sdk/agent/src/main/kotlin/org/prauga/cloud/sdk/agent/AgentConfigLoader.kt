// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.cloud.sdk.agent

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import org.prauga.cloud.sdk.BackupContract

internal object AgentConfigLoader {
    const val OPTED_OUT = "The app turned off backups."

    fun load(context: Context): BackupAgentConfig {
        val info =
            context.packageManager.getApplicationInfo(
                context.packageName,
                PackageManager.GET_META_DATA,
            )
        val className = info.metaData?.getString(BackupContract.META_CONFIG)
        return when {
            className != null -> provider(className).backupAgentConfig(context)
            info.flags and ApplicationInfo.FLAG_ALLOW_BACKUP != 0 -> BackupAgentConfig.everything()
            else -> BackupAgentConfig.skipped(OPTED_OUT)
        }
    }

    private fun provider(className: String): BackupAgentConfigProvider =
        try {
            Class.forName(className).getDeclaredConstructor().newInstance()
                as BackupAgentConfigProvider
        } catch (e: ReflectiveOperationException) {
            throw IllegalStateException(
                "$className must be a BackupAgentConfigProvider with no arguments",
                e,
            )
        }
}
