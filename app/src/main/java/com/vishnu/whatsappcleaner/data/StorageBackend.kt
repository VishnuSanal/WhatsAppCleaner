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
import com.vishnu.whatsappcleaner.model.ListFile

/**
 * Abstracts how WhatsApp media is read so the rest of the app is agnostic to whether access goes
 * through the Storage Access Framework or the direct File API (see [SafBackend], [FileBackend]).
 *
 * Folders are addressed by a path relative to the WhatsApp directory root (e.g.
 * "Media/WhatsApp Images"); each backend resolves that against its own root (a persisted tree Uri
 * for SAF, an absolute path for the File API).
 */
interface StorageBackend {

    // emit a snapshot to the UI roughly every this-many newly discovered files
    val PROGRESS_BATCH: Int
        get() = 500

    /** Totals the recursive size (in bytes) of the folder at [relativePath]. */
    suspend fun folderSize(context: Context, relativePath: String): Long

    /**
     * Lists files directly under [relativePath] (recursing for Voice/Video Notes, which nest one
     * level deep per chat). [onProgress] receives periodic snapshots so callers can stream results
     * into the UI instead of waiting for the whole walk.
     */
    suspend fun getFileList(
        context: Context,
        relativePath: String,
        onProgress: ((List<ListFile>) -> Unit)? = null
    ): ArrayList<ListFile>

    /**
     * Deletes the given files; failures are logged and skipped. Returns true only if every file was
     * deleted successfully, false if any deletion failed.
     */
    fun deleteFiles(context: Context, fileList: List<ListFile>): Boolean
}
