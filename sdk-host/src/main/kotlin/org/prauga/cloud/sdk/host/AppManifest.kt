// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.sdk.host

import java.io.File
import java.io.IOException
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

internal data class AppManifest(
    val packageName: String,
    val label: String,
    val versionCode: Long,
    val certificate: String,
    val schemaVersion: Int,
    val types: List<String>,
) {
    fun write(file: File) {
        val json =
            JSONObject()
                .put("format", FORMAT)
                .put("packageName", packageName)
                .put("label", label)
                .put("versionCode", versionCode)
                .put("certificate", certificate)
                .put("schemaVersion", schemaVersion)
                .put("types", JSONArray(types))
        file.writeText(json.toString(2))
    }

    companion object {
        const val FILE_NAME = "app.json"
        private const val FORMAT = "org.prauga.cloud.app"

        fun read(file: File): AppManifest =
            try {
                val json = JSONObject(file.readText())
                if (json.getString("format") != FORMAT) throw IOException("not an app backup")
                val types = json.getJSONArray("types")
                AppManifest(
                    packageName = json.getString("packageName"),
                    label = json.getString("label"),
                    versionCode = json.getLong("versionCode"),
                    certificate = json.getString("certificate"),
                    schemaVersion = json.getInt("schemaVersion"),
                    types = List(types.length()) { types.getString(it) },
                )
            } catch (e: JSONException) {
                throw IOException("damaged app backup", e)
            }
    }
}
