// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.cloud.sdk.agent

import android.content.Context
import android.content.pm.PackageManager
import java.io.File
import org.prauga.cloud.sdk.BackupContract

object TrustedProviders {
    private const val DIR = "org.prauga.cloud.sdk"
    private const val FILE = "trusted_providers"

    fun approved(context: Context): Set<String> =
        file(context)
            .takeIf(File::isFile)
            ?.readLines()
            ?.filter(String::isNotBlank)
            ?.toSet()
            .orEmpty()

    fun revokeAll(context: Context) {
        file(context).delete()
    }

    internal fun approve(context: Context, certificates: Set<String>) {
        synchronized(this) {
            val file = file(context)
            file.parentFile?.mkdirs()
            val partial = File(file.parentFile, "$FILE.partial")
            partial.writeText((approved(context) + certificates).sorted().joinToString("\n"))
            partial.renameTo(file)
        }
    }

    internal fun pinned(context: Context): Set<String> {
        val info =
            context.packageManager.getApplicationInfo(
                context.packageName,
                PackageManager.GET_META_DATA,
            )
        return info.metaData
            ?.getString(BackupContract.META_TRUSTED_PROVIDERS)
            .orEmpty()
            .split(',', ' ', '\n')
            .map { it.replace(":", "").trim().lowercase() }
            .filter(String::isNotEmpty)
            .toSet()
    }

    internal fun isTrusted(context: Context, uid: Int): Boolean {
        val allowed = pinned(context) + approved(context)
        val packages = context.packageManager.getPackagesForUid(uid).orEmpty()
        return packages.any { pkg ->
            SigningCertificates.sha256(context.packageManager, pkg).any(allowed::contains)
        }
    }

    private fun file(context: Context): File = File(File(context.noBackupFilesDir, DIR), FILE)
}
