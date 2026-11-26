// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.repo

class InMemoryBackend : Backend {

    private val files = mutableMapOf<String, ByteArray>()

    override fun save(type: FileType, name: String, data: ByteArray) {
        files[RepositoryLayout.path(type, name)] = data.copyOf()
    }

    override fun load(type: FileType, name: String): ByteArray =
        checkNotNull(files[RepositoryLayout.path(type, name)]) { "missing $type $name" }.copyOf()

    override fun exists(type: FileType, name: String): Boolean =
        RepositoryLayout.path(type, name) in files

    override fun list(type: FileType): List<String> =
        files.keys.filter { it.startsWith("${type.directory}/") }.map { it.substringAfterLast('/') }

    override fun delete(type: FileType, name: String) {
        files.remove(RepositoryLayout.path(type, name))
    }
}
