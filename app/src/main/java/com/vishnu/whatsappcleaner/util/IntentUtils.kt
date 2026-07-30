/*
 * Copyright (C) 2025 Vishnu Sanal T
 *
 * This file is part of WhatsAppCleaner.
 *
 * WhatsAppCleaner is free software: you can redistribute it and/or modify
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

package com.vishnu.whatsappcleaner.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.vishnu.whatsappcleaner.Constants
import com.vishnu.whatsappcleaner.R
import com.vishnu.whatsappcleaner.model.ListFile
import java.io.File

/** Hands a file off to whichever app can view it. */
fun openFile(context: Context, listFile: ListFile) {
    try {
        // SAF entries are already content:// Uris; File-API entries are absolute paths that must be
        // shared through FileProvider (a raw file:// Uri would throw FileUriExposedException).
        val uri = if (listFile.uri.startsWith("content://")) {
            listFile.uri.toUri()
        } else {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                File(listFile.uri)
            )
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            if (listFile.mimeType.isNotEmpty()) setDataAndType(uri, listFile.mimeType)
            else data = uri
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        e.printStackTrace()
        Toast.makeText(
            context,
            "No application found to open this file.",
            Toast.LENGTH_SHORT
        ).show()
    } catch (e: IllegalArgumentException) {
        e.printStackTrace()
        Toast.makeText(
            context,
            "Something went wrong...",
            Toast.LENGTH_SHORT
        ).show()
    }
}

/**
 * Opens a web link in the user's browser (or whichever app handles it, e.g. Telegram, Play Store).
 * The app declares no INTERNET permission; every link is handed off to an external handler, so a
 * missing handler is the only failure mode we guard against.
 */
fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    } catch (e: ActivityNotFoundException) {
        e.printStackTrace()
        Toast.makeText(context, context.getString(R.string.link_open_failed), Toast.LENGTH_SHORT)
            .show()
    }
}

/** Fires the system share sheet with a pre-filled message pointing at the Play Store listing. */
fun shareApp(context: Context) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.share_subject))
        putExtra(
            Intent.EXTRA_TEXT,
            context.getString(R.string.share_text, Constants.ABOUT_URL_PLAY_STORE)
        )
    }
    try {
        context.startActivity(
            Intent.createChooser(intent, context.getString(R.string.share_title))
        )
    } catch (e: ActivityNotFoundException) {
        e.printStackTrace()
        Toast.makeText(context, context.getString(R.string.link_open_failed), Toast.LENGTH_SHORT)
            .show()
    }
}
