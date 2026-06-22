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
import android.text.format.Formatter.formatFileSize
import com.vishnu.whatsappcleaner.model.ListFile

/**
 * Facade over the active [StorageBackend]. Callers address folders by a path relative to the
 * WhatsApp directory root (e.g. "Media/WhatsApp Images"); [Storage] picks the File API or SAF
 * backend per build variant and API level, and each backend resolves its own root.
 */
object FileRepository {

    suspend fun folderSize(context: Context, relativePath: String): Long = Storage.backend().folderSize(context, relativePath)

    fun formatSize(context: Context, bytes: Long): String = formatFileSize(context, bytes)

    suspend fun getFileList(
        context: Context,
        relativePath: String,
        onProgress: ((List<ListFile>) -> Unit)? = null
    ): ArrayList<ListFile> = Storage.backend().getFileList(context, relativePath, onProgress)

    fun deleteFiles(context: Context, fileList: List<ListFile>): Boolean = Storage.backend().deleteFiles(context, fileList)
}
