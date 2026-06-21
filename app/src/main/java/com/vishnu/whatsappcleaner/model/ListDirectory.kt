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

package com.vishnu.whatsappcleaner.model

import com.vishnu.whatsappcleaner.R
import java.io.Serializable

data class ListDirectory(
    val name: String,
    val path: String,
    val icon: Int,
    val hasSent: Boolean = true,
    val hasPrivate: Boolean = true,
    var size: String = "0 B"
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = -5435756175248173106L

        /**
         * Returns the WhatsApp media folders as paths relative to the selected WhatsApp tree
         * root (e.g. "Media/WhatsApp Images"). The repository resolves these against the
         * persisted SAF tree Uri.
         *
         * [pathPrefix] is normally empty; it is only used to inject the loading-indication
         * sentinel so shimmer placeholders can be detected in the UI.
         */
        fun getDirectoryList(pathPrefix: String): List<ListDirectory> {
            val prefix = if (pathPrefix.isEmpty()) "" else "$pathPrefix/"
            return listOf(
                ListDirectory(
                    "Images",
                    "${prefix}Media/WhatsApp Images",
                    R.drawable.image
                ),
                ListDirectory(
                    "Videos",
                    "${prefix}Media/WhatsApp Video",
                    R.drawable.video
                ),
                ListDirectory(
                    "Documents",
                    "${prefix}Media/WhatsApp Documents",
                    R.drawable.document
                ),

                ListDirectory(
                    "Audios",
                    "${prefix}Media/WhatsApp Audio",
                    R.drawable.audio
                ),
                ListDirectory(
                    "Statuses",
                    "${prefix}Media/.Statuses",
                    R.drawable.status,
                    false,
                    false
                ),

                ListDirectory(
                    "Voice Notes",
                    "${prefix}Media/WhatsApp Voice Notes",
                    R.drawable.voice,
                    false,
                    false
                ),
                ListDirectory(
                    "Video Notes",
                    "${prefix}Media/WhatsApp Video Notes",
                    R.drawable.video_notes,
                    false,
                    false
                ),

                ListDirectory(
                    "GIFs",
                    "${prefix}Media/WhatsApp Animated Gifs",
                    R.drawable.gif
                ),
                ListDirectory(
                    "Wallpapers",
                    "${prefix}Media/WallPaper",
                    R.drawable.wallpaper,
                    false,
                    false
                ),
                ListDirectory(
                    "Stickers",
                    "${prefix}Media/WhatsApp Stickers",
                    R.drawable.sticker,
                    false,
                    false
                ),
                ListDirectory(
                    "Profile Photos",
                    "${prefix}Media/WhatsApp Profile Photos",
                    R.drawable.profile,
                    false,
                    false
                ),
            )
        }
    }
}
