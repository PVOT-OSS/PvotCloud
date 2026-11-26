// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.cloud.sample.vault

import android.content.Context
import org.prauga.cloud.sdk.agent.BackupAgentConfig
import org.prauga.cloud.sdk.agent.BackupAgentConfigProvider
import org.prauga.cloud.sdk.agent.backupAgent

class VaultBackup : BackupAgentConfigProvider {
    override fun backupAgentConfig(context: Context): BackupAgentConfig {
        val store = VaultStore(context)
        return backupAgent {
            needsRestart = false
            credentials(export = store::all, import = store::replaceAll)
            reEnrolOnRestore(context.getString(R.string.re_enrol))
        }
    }
}
