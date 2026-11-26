// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.cloud.sdk

import android.os.Bundle
import androidx.core.os.BundleCompat

data class AgentInfo(
    val contractVersion: Int,
    val schemaVersion: Int,
    val types: List<DataTypeInfo>,
    val needsRestart: Boolean,
    val reEnrolOnRestore: List<String> = emptyList(),
    val skippedReason: String? = null,
) {

    fun toBundle(): Bundle =
        Bundle().apply {
            putInt(CONTRACT_VERSION, contractVersion)
            putInt(SCHEMA_VERSION, schemaVersion)
            putParcelableArrayList(TYPES, ArrayList(types.map(DataTypeInfo::toBundle)))
            putBoolean(NEEDS_RESTART, needsRestart)
            putStringArrayList(RE_ENROL, ArrayList(reEnrolOnRestore))
            putString(SKIPPED_REASON, skippedReason)
        }

    companion object {
        private const val CONTRACT_VERSION = "contractVersion"
        private const val SCHEMA_VERSION = "schemaVersion"
        private const val TYPES = "types"
        private const val NEEDS_RESTART = "needsRestart"
        private const val RE_ENROL = "reEnrolOnRestore"
        private const val SKIPPED_REASON = "skippedReason"

        fun fromBundle(bundle: Bundle): AgentInfo =
            AgentInfo(
                contractVersion = bundle.getInt(CONTRACT_VERSION),
                schemaVersion = bundle.getInt(SCHEMA_VERSION),
                types =
                    BundleCompat.getParcelableArrayList(bundle, TYPES, Bundle::class.java)
                        .orEmpty()
                        .mapNotNull(DataTypeInfo::fromBundle),
                needsRestart = bundle.getBoolean(NEEDS_RESTART),
                reEnrolOnRestore = bundle.getStringArrayList(RE_ENROL).orEmpty(),
                skippedReason = bundle.getString(SKIPPED_REASON),
            )
    }
}
