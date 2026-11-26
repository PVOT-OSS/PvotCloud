// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.cloud.sdk.agent

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

internal object CredentialsFormat {
    private const val FORMAT = "org.prauga.cloud.sdk.credentials"
    private const val VERSION = 1

    fun write(credentials: List<Credential>, out: OutputStream) {
        val list = JSONArray()
        credentials.forEach { credential ->
            list.put(
                JSONObject()
                    .put("id", credential.id)
                    .put("label", credential.label)
                    .put("username", credential.username)
                    .put("secret", credential.secret)
                    .putOpt("uri", credential.uri)
                    .putOpt("notes", credential.notes)
            )
        }
        val document =
            JSONObject().put("format", FORMAT).put("version", VERSION).put("credentials", list)
        out.write(document.toString().toByteArray(Charsets.UTF_8))
        out.flush()
    }

    fun read(input: InputStream): List<Credential> =
        try {
            val document = JSONObject(input.readBytes().toString(Charsets.UTF_8))
            if (document.getString("format") != FORMAT)
                throw IOException("not a credentials export")
            if (document.getInt("version") > VERSION)
                throw IOException("credentials export is too new")
            val list = document.getJSONArray("credentials")
            List(list.length()) { i ->
                val item = list.getJSONObject(i)
                Credential(
                    id = item.getString("id"),
                    label = item.getString("label"),
                    username = item.getString("username"),
                    secret = item.getString("secret"),
                    uri = item.optString("uri").ifEmpty { null },
                    notes = item.optString("notes").ifEmpty { null },
                )
            }
        } catch (e: JSONException) {
            throw IOException("damaged credentials export", e)
        }
}
