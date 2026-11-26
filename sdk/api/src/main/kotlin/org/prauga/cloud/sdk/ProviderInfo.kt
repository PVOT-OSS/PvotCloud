// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.cloud.sdk

import android.os.Bundle

data class ProviderInfo(
    val contractVersion: Int,
    val displayName: String,
    val capabilities: Set<String>,
) {

    fun toBundle(): Bundle =
        Bundle().apply {
            putInt(CONTRACT_VERSION, contractVersion)
            putString(DISPLAY_NAME, displayName)
            putStringArrayList(CAPABILITIES, ArrayList(capabilities.sorted()))
        }

    companion object {
        private const val CONTRACT_VERSION = "contractVersion"
        private const val DISPLAY_NAME = "displayName"
        private const val CAPABILITIES = "capabilities"

        fun fromBundle(bundle: Bundle): ProviderInfo =
            ProviderInfo(
                contractVersion = bundle.getInt(CONTRACT_VERSION),
                displayName = bundle.getString(DISPLAY_NAME).orEmpty(),
                capabilities = bundle.getStringArrayList(CAPABILITIES).orEmpty().toSet(),
            )
    }
}
