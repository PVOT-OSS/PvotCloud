// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.sources

import android.Manifest
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.FileNotFoundException
import java.io.InputStream
import java.time.Instant
import org.prauga.cloud.repo.source.SourceDirectory

class MediaStoreSource(private val context: Context) {

    private val resolver: ContentResolver = context.contentResolver

    fun items(): List<MediaItem> = query(IMAGES) + query(VIDEOS)

    fun tree(items: List<MediaItem>): SourceDirectory = MediaTree.build(ROOT_NAME, items, ::open)

    private fun open(item: MediaItem): InputStream {
        val collection = if (item.video) VIDEOS else IMAGES
        var uri = ContentUris.withAppendedId(collection, item.id)
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                context.checkSelfPermission(Manifest.permission.ACCESS_MEDIA_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
        ) {
            uri = MediaStore.setRequireOriginal(uri)
        }
        return resolver.openInputStream(uri) ?: throw FileNotFoundException(uri.toString())
    }

    private fun query(collection: Uri): List<MediaItem> {
        val relative = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        val pathColumn = if (relative) MediaStore.MediaColumns.RELATIVE_PATH else DATA_COLUMN
        val columns =
            arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                pathColumn,
                MediaStore.MediaColumns.SIZE,
                MediaStore.MediaColumns.DATE_MODIFIED,
            )
        val cursor = resolver.query(collection, columns, null, null, null) ?: return emptyList()
        return cursor.use {
            buildList {
                while (it.moveToNext()) {
                    val id = it.getLong(ID_COLUMN)
                    val name = it.getString(NAME_COLUMN) ?: continue
                    val path = it.getString(PATH_COLUMN).orEmpty()
                    add(
                        MediaItem(
                            id = id,
                            relativePath = if (relative) path else relativeFromData(path),
                            name = name,
                            size = it.getLong(SIZE_COLUMN),
                            modified = Instant.ofEpochSecond(it.getLong(MODIFIED_COLUMN)),
                            video = collection == VIDEOS,
                        )
                    )
                }
            }
        }
    }

    private fun relativeFromData(path: String): String {
        val root = Environment.getExternalStorageDirectory().path
        return path
            .removePrefix(root)
            .trimStart('/')
            .substringBeforeLast('/', missingDelimiterValue = "")
    }

    private companion object {
        const val ROOT_NAME = "media"
        const val ID_COLUMN = 0
        const val NAME_COLUMN = 1
        const val PATH_COLUMN = 2
        const val SIZE_COLUMN = 3
        const val MODIFIED_COLUMN = 4
        @Suppress("DEPRECATION") const val DATA_COLUMN = MediaStore.MediaColumns.DATA
        val IMAGES: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val VIDEOS: Uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    }
}
