// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.storage

interface DocumentTree {
    val rootId: String

    fun children(directoryId: String): List<DocumentEntry>

    fun createDirectory(parentId: String, name: String): String

    fun createFile(parentId: String, name: String): String

    fun write(documentId: String, data: ByteArray)

    fun read(documentId: String): ByteArray

    fun rename(documentId: String, name: String): String

    fun delete(documentId: String)
}
