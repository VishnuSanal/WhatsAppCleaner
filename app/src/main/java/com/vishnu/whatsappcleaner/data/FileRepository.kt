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
import android.util.Log
import com.vishnu.whatsappcleaner.Constants
import com.vishnu.whatsappcleaner.model.ListDirectory
import com.vishnu.whatsappcleaner.model.ListFile
import java.io.File

class FileRepository {
    companion object {

        @JvmStatic
        public suspend fun getDirectoryList(
            context: Context,
            homePath: String
        ): Pair<String, List<ListDirectory>> {
            Log.i(
                "vishnu",
                "FileRepository#getDirectoryList: $homePath"
            )

            var totalSize = 0L

            val directoryList = ListDirectory.getDirectoryList(homePath)

            directoryList.forEach { directoryItem ->

                // since this contains expensive operations :)
                var size = File(directoryItem.path)
                    .walkBottomUp()
                    .filter { f -> f.isFile() }
                    .map {
                        if (it.name == ".nomedia")
                            0
                        else
                            it.length()
                    }.sum()

                directoryItem.size = formatFileSize(context, size)

                totalSize += size
            }

            return Pair(
                formatFileSize(context, totalSize),
                directoryList
            )
        }

        @JvmStatic
        public suspend fun getFileList(context: Context, path: String): ArrayList<ListFile> {
            Log.i("vishnu", "FileRepository#getFileList: $path")

            val list = ArrayList<ListFile>()

            val appFolder = if (path.contains("WhatsApp Business", ignoreCase = true)) "WhatsApp Business" else "WhatsApp"
            
            // flattening...
            if (path.contains("Media/$appFolder Voice Notes") or path.contains("Media/$appFolder Video Notes")) File(
                path
            ).walkTopDown().forEach { f ->
                if (!f.isDirectory && f.name != ".nomedia") list.add(
                    ListFile(
                        f.path,
                        formatFileSize(context, getSize(f.path))
                    )
                )
            }
            else File(path).listFiles { dir, name ->

                val f = File("$dir/$name")

                if (!f.isDirectory && f.name != ".nomedia") list.add(
                    ListFile(
                        f.path,
                        formatFileSize(context, getSize(f.path))
                    )
                )

                true
            }

            return list
        }

        @JvmStatic
        public suspend fun getDirectoryList(path: String): ArrayList<String> {
            Log.i("vishnu", "FileRepository#getDirectoryList: $path")

            val list = ArrayList<String>()

            File(path).listFiles { dir, name ->

                val f = File("$dir/$name")

                if (f.isDirectory) list.add(f.path)

                true
            }

            return list
        }

        @JvmStatic
        public fun getLoadingList(): ArrayList<ListFile> {
            val loadingList = ArrayList<ListFile>()

            for (i in 0 until 10) {
                loadingList.add(
                    ListFile(
                        Constants.LIST_LOADING_INDICATION,
                        "0 B"
                    )
                )
            }

            return loadingList
        }

        @JvmStatic
        public fun deleteFiles(fileList: List<ListFile>): Boolean {
            Log.i("vishnu", "FileRepository#deleteFiles: $fileList")

            fileList.forEach { file ->
                file.delete()
            }

            return false
        }

        private fun getSize(path: String): Long {
//            Log.i("vishnu", "getSize() called with: path = $path")
            return File(path).walkTopDown().map { it.length() }.sum()
        }
    }
}
