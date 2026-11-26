// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.storage

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import java.io.FileNotFoundException
import java.io.IOException

class SafDocumentTree(
    private val resolver: ContentResolver,
    private val treeUri: Uri,
    override val rootId: String = DocumentsContract.getTreeDocumentId(treeUri),
) : DocumentTree {

    override fun children(directoryId: String): List<DocumentEntry> {
        val uri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, directoryId)
        val columns =
            arrayOf(
                Document.COLUMN_DOCUMENT_ID,
                Document.COLUMN_DISPLAY_NAME,
                Document.COLUMN_MIME_TYPE,
            )
        val cursor =
            resolver.query(uri, columns, null, null, null)
                ?: throw IOException("cannot list $directoryId")
        return cursor.use {
            buildList {
                while (it.moveToNext()) {
                    add(
                        DocumentEntry(
                            it.getString(0),
                            it.getString(1),
                            it.getString(2) == Document.MIME_TYPE_DIR,
                        )
                    )
                }
            }
        }
    }

    override fun createDirectory(parentId: String, name: String): String =
        create(parentId, Document.MIME_TYPE_DIR, name)

    override fun createFile(parentId: String, name: String): String = create(parentId, BINARY, name)

    override fun write(documentId: String, data: ByteArray) {
        val stream =
            resolver.openOutputStream(document(documentId), "wt")
                ?: throw IOException("cannot write $documentId")
        stream.use { it.write(data) }
    }

    override fun read(documentId: String): ByteArray {
        val stream =
            resolver.openInputStream(document(documentId))
                ?: throw FileNotFoundException(documentId)
        return stream.use { it.readBytes() }
    }

    override fun rename(documentId: String, name: String): String {
        val renamed = DocumentsContract.renameDocument(resolver, document(documentId), name)
        return renamed?.let(DocumentsContract::getDocumentId) ?: documentId
    }

    override fun delete(documentId: String) {
        if (!DocumentsContract.deleteDocument(resolver, document(documentId))) {
            throw IOException("cannot delete $documentId")
        }
    }

    private fun create(parentId: String, mimeType: String, name: String): String {
        val created =
            DocumentsContract.createDocument(resolver, document(parentId), mimeType, name)
                ?: throw IOException("cannot create $name")
        return DocumentsContract.getDocumentId(created)
    }

    private fun document(documentId: String): Uri =
        DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)

    private companion object {
        const val BINARY = "application/octet-stream"
    }
}
