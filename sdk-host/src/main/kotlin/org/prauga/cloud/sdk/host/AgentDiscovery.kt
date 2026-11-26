// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.sdk.host

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import javax.inject.Inject
import org.prauga.cloud.sdk.BackupContract

class AgentDiscovery @Inject constructor(@ApplicationContext private val context: Context) {

    fun agents(): List<AgentApp> =
        context.packageManager
            .queryIntentServices(Intent(BackupContract.ACTION_AGENT), 0)
            .mapNotNull { it.serviceInfo?.packageName }
            .filter { it != context.packageName }
            .distinct()
            .mapNotNull(::app)
            .sortedBy { it.label.lowercase() }

    fun app(packageName: String): AgentApp? =
        try {
            val pm = context.packageManager
            val info = pm.getPackageInfo(packageName, signingFlag())
            AgentApp(
                packageName = packageName,
                label = info.applicationInfo?.loadLabel(pm)?.toString() ?: packageName,
                certificates = signatures(info).map { sha256(it.toByteArray()) }.toSet(),
                versionCode = PackageInfoCompat.getLongVersionCode(info),
            )
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }

    private fun signingFlag(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) PackageManager.GET_SIGNING_CERTIFICATES
        else @Suppress("DEPRECATION") PackageManager.GET_SIGNATURES

    private fun signatures(info: android.content.pm.PackageInfo): List<Signature> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signing = info.signingInfo
            when {
                signing == null -> emptyList()
                signing.hasMultipleSigners() -> signing.apkContentsSigners.toList()
                else -> signing.signingCertificateHistory.toList()
            }
        } else {
            @Suppress("DEPRECATION") info.signatures.orEmpty().toList()
        }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}
