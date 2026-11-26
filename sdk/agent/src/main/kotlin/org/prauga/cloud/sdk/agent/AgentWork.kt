// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.cloud.sdk.agent

import android.content.Context
import java.io.InputStream
import java.io.OutputStream
import org.prauga.cloud.sdk.AgentInfo
import org.prauga.cloud.sdk.BackupContract
import org.prauga.cloud.sdk.DataKind
import org.prauga.cloud.sdk.DataTypeInfo
import org.prauga.cloud.sdk.ResultCode

internal class AgentWork(private val context: Context, private val config: BackupAgentConfig) {

    @Volatile
    var restartRequired: Boolean = config.needsRestart
        private set

    fun describe(callerTrusted: Boolean): AgentInfo =
        AgentInfo(
            contractVersion = BackupContract.VERSION,
            schemaVersion = config.schemaVersion,
            types = types(estimate = callerTrusted),
            needsRestart = config.needsRestart,
            reEnrolOnRestore = config.reEnrolOnRestore,
            skippedReason = config.skippedReason,
            callerTrusted = callerTrusted,
        )

    fun export(type: String, out: OutputStream): ResultCode {
        val custom = config.customTypes[type]
        val appData = config.appData
        val credentials = config.credentials
        return when {
            type == BackupContract.TYPE_APP_DATA && appData != null ->
                ResultCode.OK.also { AppDataArchive(context, appData).export(out) }
            type == BackupContract.TYPE_CREDENTIALS && credentials != null ->
                ResultCode.OK.also { CredentialsFormat.write(credentials.export(), out) }
            custom != null -> ResultCode.OK.also { custom.export(out) }
            else -> ResultCode.SKIPPED
        }
    }

    fun import(type: String, fromSchema: Int, input: InputStream): ResultCode {
        val custom = config.customTypes[type]
        val appData = config.appData
        val credentials = config.credentials
        return when {
            fromSchema > config.schemaVersion -> ResultCode.SCHEMA_TOO_NEW
            type == BackupContract.TYPE_APP_DATA && appData != null ->
                ResultCode.OK.also {
                    if (AppDataArchive(context, appData).import(input)) restartRequired = true
                }
            type == BackupContract.TYPE_CREDENTIALS && credentials != null ->
                ResultCode.OK.also { credentials.import(CredentialsFormat.read(input)) }
            custom != null -> ResultCode.OK.also { custom.import(input, fromSchema) }
            else -> ResultCode.SKIPPED
        }
    }

    fun restoreFinished() {
        config.onRestoreFinished()
    }

    private fun types(estimate: Boolean): List<DataTypeInfo> = buildList {
        config.appData?.let { spec ->
            val size = if (estimate) AppDataArchive(context, spec).estimateBytes() else 0
            add(DataTypeInfo(BackupContract.TYPE_APP_DATA, DataKind.APP_DATA, size))
        }
        if (config.credentials != null)
            add(DataTypeInfo(BackupContract.TYPE_CREDENTIALS, DataKind.CREDENTIALS, 0))
        config.customTypes.keys.forEach { add(DataTypeInfo(it, DataKind.CUSTOM, 0)) }
    }
}
