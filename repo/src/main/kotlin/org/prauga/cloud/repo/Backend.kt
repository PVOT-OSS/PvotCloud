// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.repo

interface Backend {

    fun save(type: FileType, name: String, data: ByteArray)

    fun load(type: FileType, name: String): ByteArray

    fun exists(type: FileType, name: String): Boolean

    fun list(type: FileType): List<String>

    fun delete(type: FileType, name: String)
}
