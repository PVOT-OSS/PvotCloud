// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.cloud.sdk.agent

class BackupAgentConfig
internal constructor(
    val schemaVersion: Int,
    val needsRestart: Boolean,
    val reEnrolOnRestore: List<String>,
    internal val appData: AppDataSpec?,
    internal val customTypes: Map<String, CustomType>,
    internal val credentials: CredentialsType?,
    internal val onRestoreFinished: () -> Unit,
    internal val skippedReason: String? = null,
) {
    internal companion object {
        fun everything(): BackupAgentConfig =
            BackupAgentConfig(
                schemaVersion = 1,
                needsRestart = true,
                reEnrolOnRestore = emptyList(),
                appData = AppDataSpec.Everything,
                customTypes = emptyMap(),
                credentials = null,
                onRestoreFinished = {},
            )

        fun skipped(reason: String): BackupAgentConfig =
            BackupAgentConfig(
                schemaVersion = 1,
                needsRestart = false,
                reEnrolOnRestore = emptyList(),
                appData = null,
                customTypes = emptyMap(),
                credentials = null,
                onRestoreFinished = {},
                skippedReason = reason,
            )
    }
}
