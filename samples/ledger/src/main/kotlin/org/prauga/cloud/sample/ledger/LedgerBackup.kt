// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.cloud.sample.ledger

import android.content.Context
import org.prauga.cloud.sdk.agent.BackupAgentConfig
import org.prauga.cloud.sdk.agent.BackupAgentConfigProvider
import org.prauga.cloud.sdk.agent.backupAgent

class LedgerBackup : BackupAgentConfigProvider {
    override fun backupAgentConfig(context: Context): BackupAgentConfig {
        val ledger = Ledger(context)
        return backupAgent {
            schemaVersion = 2
            needsRestart = false
            database(Ledger.DATABASE)
            sharedPreferences(Ledger.SETTINGS)
            custom(
                "budget",
                export = { out -> if (ledger.budget.isFile) out.write(ledger.budget.readBytes()) },
                import = { source, fromSchema ->
                    val text = source.readBytes().decodeToString()
                    ledger.budget.writeText(if (fromSchema < 2) "$text EUR" else text)
                },
            )
        }
    }
}
