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

package com.vishnu.whatsappcleaner

object Constants {
    const val WHATSAPP_HOME_URI = "whatsapp_home_uri"

    const val SCREEN_PERMISSION = "permission"
    const val SCREEN_HOME = "home"
    const val SCREEN_DETAILS = "details"

    const val DETAILS_LIST_ITEM = "details_list_item"

    const val LIST_LOADING_INDICATION: String = "com.vishnu.whatsappcleaner.loading"

    const val TAG = "vishnu"

    const val CALCULATING = "Calculating…"

    const val DETAILS_GRID_COLUMN_COUNT = 3

    const val DETAILS_LIST_THUMBNAIL_SIZE_DP = 48

    final val EXTENSIONS_IMAGE = listOf(
        "jpg",
        "jpeg",
        "bmp",
        "raw",
        "png_a",
        "png",
        "webp_a",
        "webp",
        "animated_webp",
        "avif",
        "animated_avif",
    )

    final val EXTENSIONS_VIDEO = listOf(
        "mp4",
        "mpeg4",
        "webm",
        "avi",
        "gif",
        "3gp",
        "avi",
    )

    final val EXTENSIONS_DOCS = listOf(
        "txt",
        "pdf",
        "doc",
        "odt",
        "ppt",
        "pptx",
        "odp",
        "xls",
        "xlsx",
        "ods",
    )

    final val EXTENSIONS_AUDIO = listOf(
        "aac",
        "mp3",
        "flac",
        "opus",
        "midi",
        "wav",
        "ogg",
    )
}
