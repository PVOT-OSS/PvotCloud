// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.cloud.sdk.agent

import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import java.security.MessageDigest

internal object SigningCertificates {

    fun sha256(packageManager: PackageManager, packageName: String): Set<String> =
        try {
            signatures(packageManager, packageName).map { digest(it.toByteArray()) }.toSet()
        } catch (_: PackageManager.NameNotFoundException) {
            emptySet()
        }

    fun format(sha256: String): String = sha256.uppercase().chunked(2).joinToString(":")

    private fun signatures(packageManager: PackageManager, packageName: String): List<Signature> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val info =
                packageManager
                    .getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                    .signingInfo
            when {
                info == null -> emptyList()
                info.hasMultipleSigners() -> info.apkContentsSigners.toList()
                else -> info.signingCertificateHistory.toList()
            }
        } else {
            @Suppress("DEPRECATION")
            packageManager
                .getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
                .signatures
                .orEmpty()
                .toList()
        }

    private fun digest(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}
