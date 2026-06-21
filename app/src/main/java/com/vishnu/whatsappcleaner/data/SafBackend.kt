/*
 * Copyright (C) 2025 Vishnu Sanal T
 *
 * This file is part of WhatsAppCleaner.
 *
 * Quotes Status Creator is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.vishnu.whatsappcleaner.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.text.format.Formatter.formatFileSize
import android.util.Log
import androidx.core.net.toUri
import com.vishnu.whatsappcleaner.Constants
import com.vishnu.whatsappcleaner.Constants.TAG
import com.vishnu.whatsappcleaner.data.SafBackend.folderSize
import com.vishnu.whatsappcleaner.data.SafBackend.getFileList
import com.vishnu.whatsappcleaner.model.ListFile
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

/**
 * [StorageBackend] backed by the Storage Access Framework, used by the Play build on API 30+.
 *
 * The user grants access to the WhatsApp directory once via an ACTION_OPEN_DOCUMENT_TREE picker;
 * the resulting tree Uri is persisted and resolved here. Because the external-storage documents
 * provider uses path-based document ids, a relative path can be appended directly to the tree's
 * root document id, which works for both the legacy "/WhatsApp" location and the scoped
 * "/Android/media/com.whatsapp/WhatsApp" location.
 *
 * Every directory read is a separate IPC to the documents provider (~50ms) and the provider
 * serializes them, so a folder like "Voice Notes" (hundreds of date sub-folders) can take tens of
 * seconds. We can't make the queries faster, so [getFileList] and the per-folder [folderSize]
 * stream/iterate so callers never block the UI on the full walk.
 */
object SafBackend : StorageBackend {

    private val PROJECTION = arrayOf(
        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        DocumentsContract.Document.COLUMN_MIME_TYPE,
        DocumentsContract.Document.COLUMN_SIZE,
        DocumentsContract.Document.COLUMN_LAST_MODIFIED,
    )

    /**
     * Resolves the WhatsApp tree Uri. Only read+write permissions qualify, since deletion goes
     * through [DocumentsContract.deleteDocument]. Prefers the persisted permission that matches the
     * tree the user actually picked (saved in DataStore) so an unrelated tree granted elsewhere
     * can't resolve the wrong root; otherwise falls back to any persisted read+write tree.
     */
    private suspend fun homeTreeUri(context: Context): String? {
        val readWrite = context.contentResolver.persistedUriPermissions
            .filter { it.isReadPermission && it.isWritePermission }

        val saved = StoreData(context).get(Constants.WHATSAPP_HOME_URI)
            ?.takeIf { it.startsWith("content://") }

        return readWrite.firstOrNull { it.uri.toString() == saved }?.uri?.toString()
            ?: readWrite.firstOrNull()?.uri?.toString()
    }

    private fun childDocumentId(rootDocId: String, relativePath: String): String {
        val rel = relativePath.trim('/')
        return if (rel.isEmpty()) rootDocId else "$rootDocId/$rel"
    }

    override suspend fun folderSize(context: Context, relativePath: String): Long {
        val treeUri = (homeTreeUri(context) ?: return 0L).toUri()
        val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
        return computeSize(context, treeUri, childDocumentId(rootDocId, relativePath))
    }

    override suspend fun getFileList(
        context: Context,
        relativePath: String,
        onProgress: ((List<ListFile>) -> Unit)?
    ): ArrayList<ListFile> {
        Log.i(TAG, "SafBackend#getFileList: $relativePath")

        val list = ArrayList<ListFile>()

        val treeUri = (homeTreeUri(context) ?: return list).toUri()
        val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
        val resolver = context.contentResolver

        // Voice Notes / Video Notes are nested one level deep (per-chat folders); flatten them.
        val recursive = relativePath.contains("WhatsApp Voice Notes") ||
            relativePath.contains("WhatsApp Video Notes")

        var lastEmittedAt = 0

        val stack = ArrayDeque<String>()
        stack.addLast(childDocumentId(rootDocId, relativePath))

        while (stack.isNotEmpty()) {
            // Bail out promptly if the caller canceled (e.g. navigated away mid-walk); the SAF
            // walk can run for tens of seconds, so honoring cancellation matters here.
            coroutineContext.ensureActive()

            val parentDocId = stack.removeLast()
            val childrenUri =
                DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)

            try {
                resolver.query(childrenUri, PROJECTION, null, null, null)?.use { cursor ->
                    val idIndex =
                        cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val nameIndex =
                        cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val mimeIndex =
                        cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                    val sizeIndex =
                        cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)
                    val dateIndex =
                        cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

                    while (cursor.moveToNext()) {
                        val name = cursor.getString(nameIndex) ?: continue
                        val mime = cursor.getString(mimeIndex)
                        val childId = cursor.getString(idIndex)

                        if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                            if (recursive) stack.addLast(childId)
                        } else if (name != ".nomedia") {
                            val sizeBytes =
                                if (!cursor.isNull(sizeIndex)) cursor.getLong(sizeIndex) else 0L
                            val dateModified =
                                if (!cursor.isNull(dateIndex)) cursor.getLong(dateIndex) else 0L

                            list.add(
                                ListFile(
                                    uri = DocumentsContract.buildDocumentUriUsingTree(
                                        treeUri,
                                        childId
                                    ).toString(),
                                    name = name,
                                    sizeBytes = sizeBytes,
                                    dateModified = dateModified,
                                    mimeType = mime ?: "",
                                    size = formatFileSize(context, sizeBytes),
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                // a missing folder simply yields nothing
                Log.w(TAG, "getFileList: $parentDocId -> $e")
            }

            // stream partial results once enough new files have accumulated
            if (onProgress != null && list.size - lastEmittedAt >= PROGRESS_BATCH) {
                lastEmittedAt = list.size
                onProgress(ArrayList(list))
            }
        }

        return list
    }

    /** Recursively totals the size of all files under [docId] (serial; the provider serializes). */
    private fun computeSize(context: Context, treeUri: Uri, docId: String): Long {
        val resolver = context.contentResolver
        var total = 0L

        val stack = ArrayDeque<String>()
        stack.addLast(docId)

        while (stack.isNotEmpty()) {
            val parentDocId = stack.removeLast()
            val childrenUri =
                DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)

            try {
                resolver.query(
                    childrenUri,
                    arrayOf(
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                        DocumentsContract.Document.COLUMN_MIME_TYPE,
                        DocumentsContract.Document.COLUMN_SIZE,
                    ),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    val idIndex =
                        cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    val nameIndex =
                        cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                    val mimeIndex =
                        cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                    val sizeIndex =
                        cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)

                    while (cursor.moveToNext()) {
                        val mime = cursor.getString(mimeIndex)
                        if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                            stack.addLast(cursor.getString(idIndex))
                        } else if (cursor.getString(nameIndex) != ".nomedia" &&
                            !cursor.isNull(sizeIndex)
                        ) {
                            total += cursor.getLong(sizeIndex)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "computeSize: $parentDocId -> $e")
            }
        }

        return total
    }

    override fun deleteFiles(context: Context, fileList: List<ListFile>): Boolean {
        Log.i(TAG, "SafBackend#deleteFiles: $fileList")

        val resolver = context.contentResolver

        var allDeleted = true
        fileList.forEach { file ->
            try {
                if (!DocumentsContract.deleteDocument(resolver, file.uri.toUri())) allDeleted =
                    false
            } catch (e: Exception) {
                Log.w(TAG, "deleteFiles: ${file.uri} -> $e")
                allDeleted = false
            }
        }

        return allDeleted
    }
}
