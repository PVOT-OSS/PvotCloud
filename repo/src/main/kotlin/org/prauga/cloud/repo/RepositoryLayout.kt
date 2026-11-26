// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.repo

object RepositoryLayout {

    const val CONFIG_NAME = "config"
    private const val PACK_FOLDERS = 256

    fun path(type: FileType, name: String): String =
        when (type) {
            FileType.CONFIG -> CONFIG_NAME
            FileType.PACK -> "${type.directory}/${name.take(2)}/$name"
            else -> "${type.directory}/$name"
        }

    fun directory(type: FileType): String = type.directory

    fun directories(): List<String> =
        listOf(FileType.KEY, FileType.INDEX, FileType.SNAPSHOT, FileType.LOCK).map {
            it.directory
        } + (0 until PACK_FOLDERS).map { "${FileType.PACK.directory}/%02x".format(it) }
}
