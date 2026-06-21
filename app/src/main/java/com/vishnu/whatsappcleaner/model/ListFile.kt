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

import java.io.Serializable

/**
 * Represents a single file exposed through the Storage Access Framework.
 *
 * [uri] is the string form of a SAF document Uri (built from the persisted tree). All file
 * operations (open, delete, thumbnail loading) go through this Uri.
 */
data class ListFile(
    val uri: String,
    val name: String = "",
    val sizeBytes: Long = 0L,
    val dateModified: Long = 0L,
    val mimeType: String = "",
    var size: String = "0 B",
    var isSelected: Boolean = false,
) : Serializable {
    val extension: String
        get() = name.substringAfterLast('.', "")

    companion object {
        private const val serialVersionUID: Long = 8425722975465458623L
    }
}
