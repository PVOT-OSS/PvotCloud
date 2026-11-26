// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.cloud.sdk.agent

import java.io.InputStream
import java.io.OutputStream
import org.prauga.cloud.sdk.BackupContract

@DslMarker annotation class BackupAgentDsl

@BackupAgentDsl
class BackupAgentBuilder internal constructor() {
    var schemaVersion: Int = 1
    var needsRestart: Boolean = true

    private var everything = false
    private val databases = mutableListOf<String>()
    private val sharedPreferences = mutableListOf<String>()
    private val files = mutableListOf<String>()
    private val customTypes = linkedMapOf<String, CustomType>()
    private val reEnrol = mutableListOf<String>()
    private var credentials: CredentialsType? = null
    private var onRestoreFinished: () -> Unit = {}

    fun everything() {
        everything = true
    }

    fun database(name: String) {
        databases += name
    }

    fun sharedPreferences(name: String) {
        sharedPreferences += name
    }

    fun dataStore(name: String) {
        files += "datastore/$name.preferences_pb"
    }

    fun files(path: String) {
        files += path.trim('/')
    }

    fun custom(
        type: String,
        export: (OutputStream) -> Unit,
        import: (source: InputStream, fromSchema: Int) -> Unit,
    ) {
        require(type != BackupContract.TYPE_APP_DATA && type != BackupContract.TYPE_CREDENTIALS) {
            "$type is reserved"
        }
        customTypes[type] = CustomType(export, import)
    }

    fun credentials(export: () -> List<Credential>, import: (List<Credential>) -> Unit) {
        credentials = CredentialsType(export, import)
    }

    fun reEnrolOnRestore(note: String) {
        reEnrol += note
    }

    fun onRestoreFinished(block: () -> Unit) {
        onRestoreFinished = block
    }

    internal fun build(): BackupAgentConfig {
        require(schemaVersion >= 1) { "schemaVersion starts at 1" }
        val selected =
            databases.isNotEmpty() || sharedPreferences.isNotEmpty() || files.isNotEmpty()
        val appData =
            when {
                everything -> AppDataSpec.Everything
                selected ->
                    AppDataSpec.Selected(
                        databases.toList(),
                        sharedPreferences.toList(),
                        files.toList(),
                    )
                else -> null
            }
        return BackupAgentConfig(
            schemaVersion = schemaVersion,
            needsRestart = needsRestart,
            reEnrolOnRestore = reEnrol.toList(),
            appData = appData,
            customTypes = customTypes.toMap(),
            credentials = credentials,
            onRestoreFinished = onRestoreFinished,
        )
    }
}

fun backupAgent(block: BackupAgentBuilder.() -> Unit): BackupAgentConfig =
    BackupAgentBuilder().apply(block).build()
