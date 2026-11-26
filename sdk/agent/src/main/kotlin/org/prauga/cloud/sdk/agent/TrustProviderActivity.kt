// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.cloud.sdk.agent

import android.app.Activity
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle

class TrustProviderActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val caller = callingPackage
        val certificates = caller?.let { SigningCertificates.sha256(packageManager, it) }.orEmpty()
        if (caller == null || certificates.isEmpty()) {
            finish()
            return
        }
        val providerName = label(caller)
        val appName = label(packageName)
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle(getString(R.string.pvot_cloud_sdk_trust_title, providerName))
            .setMessage(
                getString(
                    R.string.pvot_cloud_sdk_trust_message,
                    providerName,
                    appName,
                    SigningCertificates.format(certificates.first()),
                )
            )
            .setPositiveButton(R.string.pvot_cloud_sdk_trust_allow) { _, _ ->
                TrustedProviders.approve(this, certificates)
                setResult(RESULT_OK)
                finish()
            }
            .setNegativeButton(R.string.pvot_cloud_sdk_trust_deny) { _, _ -> finish() }
            .setOnCancelListener { finish() }
            .show()
            .window
            ?.decorView
            ?.filterTouchesWhenObscured = true
    }

    private fun label(packageName: String): CharSequence =
        try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0))
        } catch (_: PackageManager.NameNotFoundException) {
            packageName
        }
}
