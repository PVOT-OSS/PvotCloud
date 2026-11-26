// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.sdk.host

import java.io.File
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject
import org.prauga.cloud.repo.source.SourceDirectory
import org.prauga.cloud.repo.source.SourceEntry
import org.prauga.cloud.sdk.BackupContract
import org.prauga.cloud.sdk.ResultCode

class AppBackup
@Inject
constructor(private val discovery: AgentDiscovery, private val client: AgentClient) {

    suspend fun export(approved: Map<String, String>, workDir: File): AppExport {
        workDir.deleteRecursively()
        val results =
            approved.entries
                .sortedBy { it.key }
                .map { (pkg, cert) -> exportApp(pkg, cert, workDir) }
        val exported =
            results.filter { it.code == ResultCode.OK }.map { File(workDir, it.packageName) }
        val root =
            if (exported.isEmpty()) null else AppsTree(workDir, exported.map { it.name }.toSet())
        return AppExport(root, results)
    }

    suspend fun restore(
        packageName: String,
        files: Map<String, () -> InputStream>,
        workDir: File,
    ): AppResult {
        workDir.deleteRecursively()
        workDir.mkdirs()
        return try {
            val local = files.mapValues { (name, open) ->
                File(workDir, name).also { target ->
                    open().use { input -> target.outputStream().use(input::copyTo) }
                }
            }
            val manifestFile =
                local[AppManifest.FILE_NAME] ?: throw IOException("the backup has no $MANIFEST")
            importApp(AppManifest.read(manifestFile), local)
        } catch (e: IOException) {
            AppResult(packageName, ResultCode.IO, e.message)
        } finally {
            workDir.deleteRecursively()
        }
    }

    suspend fun isApproved(packageName: String): Boolean =
        try {
            client.connect(packageName).use { it.describe()?.callerTrusted == true }
        } catch (_: IOException) {
            false
        }

    private suspend fun exportApp(
        packageName: String,
        certificate: String,
        workDir: File,
    ): AppResult {
        val app = discovery.app(packageName)
        val dir = File(workDir, packageName)
        return when {
            app == null -> AppResult(packageName, ResultCode.SKIPPED, NOT_INSTALLED)
            certificate !in app.certificates ->
                AppResult(packageName, ResultCode.SKIPPED, CERTIFICATE_CHANGED)
            else ->
                try {
                    client.connect(packageName).use { session ->
                        exportTypes(app, certificate, session, dir)
                    }
                } catch (e: IOException) {
                    AppResult(packageName, ResultCode.IO, e.message)
                }
        }.also { if (it.code != ResultCode.OK) dir.deleteRecursively() }
    }

    private suspend fun exportTypes(
        app: AgentApp,
        certificate: String,
        session: AgentSession,
        dir: File,
    ): AppResult {
        val info = session.describe()
        val skipped = info?.skippedReason
        return when {
            info == null -> AppResult(app.packageName, ResultCode.INTERNAL, STOPPED)
            !info.callerTrusted ->
                AppResult(app.packageName, ResultCode.NOT_CONSENTED, NOT_APPROVED)
            skipped != null -> AppResult(app.packageName, ResultCode.SKIPPED, skipped)
            info.types.isEmpty() -> AppResult(app.packageName, ResultCode.SKIPPED, NOTHING)
            else -> {
                dir.mkdirs()
                var bytes = 0L
                for (type in info.types) {
                    val result = session.export(type.name, File(dir, type.name))
                    if (result.code != ResultCode.OK) {
                        return AppResult(
                            app.packageName,
                            result.code,
                            "${type.name}: ${result.reason}",
                        )
                    }
                    bytes += result.bytes
                }
                AppManifest(
                        packageName = app.packageName,
                        label = app.label,
                        versionCode = app.versionCode,
                        certificate = certificate,
                        schemaVersion = info.schemaVersion,
                        types = info.types.map { it.name },
                    )
                    .write(File(dir, AppManifest.FILE_NAME))
                AppResult(app.packageName, ResultCode.OK, bytes = bytes)
            }
        }
    }

    private suspend fun importApp(manifest: AppManifest, files: Map<String, File>): AppResult {
        val pkg = manifest.packageName
        val app = discovery.app(pkg)
        return when {
            app == null -> AppResult(pkg, ResultCode.SKIPPED, NOT_INSTALLED)
            manifest.certificate !in app.certificates ->
                AppResult(pkg, ResultCode.SKIPPED, CERTIFICATE_CHANGED)
            else -> client.connect(pkg).use { session -> importTypes(manifest, files, session) }
        }
    }

    private suspend fun importTypes(
        manifest: AppManifest,
        files: Map<String, File>,
        session: AgentSession,
    ): AppResult {
        val info = session.describe()
        return if (info == null || !info.callerTrusted) {
            AppResult(manifest.packageName, ResultCode.NOT_CONSENTED, NOT_APPROVED)
        } else {
            importAll(manifest, files, session)
        }
    }

    private suspend fun importAll(
        manifest: AppManifest,
        files: Map<String, File>,
        session: AgentSession,
    ): AppResult {
        var bytes = 0L
        for (type in manifest.types.sortedBy(::restoreOrder)) {
            val file = files[type]
            val result =
                if (file == null) TransferResult(ResultCode.IO, "missing from the backup", 0)
                else session.import(type, manifest.schemaVersion, file)
            if (result.code != ResultCode.OK && result.code != ResultCode.SKIPPED) {
                return AppResult(manifest.packageName, result.code, "$type: ${result.reason}")
            }
            bytes += result.bytes
        }
        session.restoreFinished()
        return AppResult(manifest.packageName, ResultCode.OK, bytes = bytes)
    }

    private fun restoreOrder(type: String): Int =
        when (type) {
            BackupContract.TYPE_APP_DATA -> 0
            BackupContract.TYPE_CREDENTIALS -> 1
            else -> 2
        }

    private class AppsTree(private val workDir: File, private val included: Set<String>) :
        SourceDirectory by FileTree(workDir, APPS) {
        override fun children(): List<SourceEntry> =
            FileTree(workDir, APPS).children().filter { it.name in included }
    }

    companion object {
        const val APPS = "apps"
        private const val MANIFEST = AppManifest.FILE_NAME
        private const val NOT_INSTALLED = "The app is not installed."
        private const val CERTIFICATE_CHANGED = "The app's signing certificate changed."
        private const val NOT_APPROVED = "The app has not approved Pvot Cloud."
        private const val STOPPED = "The app stopped."
        private const val NOTHING = "The app has nothing to back up."
    }
}
