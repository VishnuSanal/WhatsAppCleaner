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

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import com.vishnu.whatsappcleaner.BuildConfig

/**
 * Decides how WhatsApp media is accessed and reports whether the required access is granted.
 *
 * The direct File API is dramatically faster than SAF (no per-directory IPC; a folder that takes
 * tens of seconds over SAF reads in a couple of seconds), so it is preferred everywhere it is
 * allowed:
 *
 * - API <= 29 — File API via [Manifest.permission.READ_EXTERNAL_STORAGE] (+ legacy external
 *   storage), for every build variant.
 * - API >= 30, non-Play — File API via [Manifest.permission.MANAGE_EXTERNAL_STORAGE] (All Files
 *   Access). `Android/media/<pkg>` is readable with All Files Access (unlike `Android/data`).
 * - API >= 30, Play — SAF, because Google Play forbids All Files Access for this use case.
 */
object Storage {

    enum class Mode { FILE_LEGACY, FILE_MANAGE, SAF }

    fun mode(): Mode = when {
        Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q -> Mode.FILE_LEGACY
        !BuildConfig.IS_PLAY -> Mode.FILE_MANAGE
        else -> Mode.SAF
    }

    fun useFileApi(): Boolean = mode() != Mode.SAF

    fun backend(): StorageBackend = if (useFileApi()) FileBackend else SafBackend

    /** Whether the access required by the current [mode] has been granted. */
    fun isReady(context: Context): Boolean = when (mode()) {
        Mode.FILE_LEGACY ->
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED

        Mode.FILE_MANAGE ->
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                Environment.isExternalStorageManager()

        Mode.SAF ->
            context.contentResolver.persistedUriPermissions.any {
                it.isReadPermission && it.isWritePermission
            }
    }
}
