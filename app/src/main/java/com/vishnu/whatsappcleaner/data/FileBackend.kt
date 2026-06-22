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
import android.os.Environment
import android.text.format.Formatter.formatFileSize
import android.util.Log
import android.webkit.MimeTypeMap
import com.vishnu.whatsappcleaner.Constants.TAG
import com.vishnu.whatsappcleaner.model.ListFile
import kotlinx.coroutines.ensureActive
import java.io.File
import kotlin.coroutines.coroutineContext

/**
 * [StorageBackend] backed by the direct File API. Used on API <= 29 (legacy external storage) and
 * on API >= 30 for non-Play builds holding All Files Access.
 *
 * `Android/media/com.whatsapp` is reachable this way (unlike `Android/data`) and, crucially, there
 * is no per-directory IPC, so the heavy folders (Voice/Video Notes) that take tens of seconds over
 * SAF read in a couple of seconds here. Files are addressed by absolute path; [ListFile.uri] holds
 * that path (Glide loads paths directly, and openFile wraps it with FileProvider for ACTION_VIEW).
 */
object FileBackend : StorageBackend {

    /** Modern scoped location first, then the pre-scoped legacy location for ancient installs. */
    private fun whatsAppRoot(): File? {
        val ext = Environment.getExternalStorageDirectory()
        File(ext, "Android/media/com.whatsapp/WhatsApp").let { if (it.isDirectory) return it }
        File(ext, "WhatsApp").let { if (it.isDirectory) return it }
        return null
    }

    private fun resolve(relativePath: String): File? {
        val root = whatsAppRoot() ?: return null
        val rel = relativePath.trim('/')
        return if (rel.isEmpty()) root else File(root, rel)
    }

    override suspend fun folderSize(context: Context, relativePath: String): Long {
        val dir = resolve(relativePath) ?: return 0L
        if (!dir.exists()) return 0L

        var total = 0L
        dir.walkTopDown().forEach { if (it.isFile && it.name != ".nomedia") total += it.length() }
        return total
    }

    override suspend fun getFileList(
        context: Context,
        relativePath: String,
        onProgress: ((List<ListFile>) -> Unit)?
    ): ArrayList<ListFile> {
        Log.i(TAG, "FileBackend#getFileList: $relativePath")

        val list = ArrayList<ListFile>()
        val dir = resolve(relativePath) ?: return list

        // Voice Notes / Video Notes are nested one level deep (per-chat folders); flatten them.
        val recursive = relativePath.contains("WhatsApp Voice Notes") ||
            relativePath.contains("WhatsApp Video Notes")

        var lastEmittedAt = 0

        val stack = ArrayDeque<File>()
        stack.addLast(dir)

        while (stack.isNotEmpty()) {
            // Bail out promptly if the caller canceled (e.g. navigated away mid-walk).
            coroutineContext.ensureActive()

            val children = stack.removeLast().listFiles() ?: continue

            for (child in children) {
                if (child.isDirectory) {
                    if (recursive) stack.addLast(child)
                } else if (child.name != ".nomedia") {
                    list.add(child.toListFile(context))
                }
            }

            // stream partial results once enough new files have accumulated
            if (onProgress != null && list.size - lastEmittedAt >= PROGRESS_BATCH) {
                lastEmittedAt = list.size
                onProgress(ArrayList(list))
            }
        }

        return list
    }

    private fun File.toListFile(context: Context): ListFile {
        val sizeBytes = length()
        val ext = name.substringAfterLast('.', "").lowercase()
        return ListFile(
            uri = absolutePath,
            name = name,
            sizeBytes = sizeBytes,
            dateModified = lastModified(),
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "",
            size = formatFileSize(context, sizeBytes),
        )
    }

    override fun deleteFiles(context: Context, fileList: List<ListFile>): Boolean {
        Log.i(TAG, "FileBackend#deleteFiles: $fileList")

        var allDeleted = true
        fileList.forEach { file ->
            try {
                if (!File(file.uri).delete()) allDeleted = false
            } catch (e: Exception) {
                Log.w(TAG, "deleteFiles: ${file.uri} -> $e")
                allDeleted = false
            }
        }

        return allDeleted
    }
}
