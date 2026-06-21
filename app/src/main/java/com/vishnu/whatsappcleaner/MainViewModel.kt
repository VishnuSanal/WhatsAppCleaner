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

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vishnu.whatsappcleaner.Constants.CALCULATING
import com.vishnu.whatsappcleaner.Constants.TAG
import com.vishnu.whatsappcleaner.data.FileRepository
import com.vishnu.whatsappcleaner.data.Storage
import com.vishnu.whatsappcleaner.data.StoreData
import com.vishnu.whatsappcleaner.model.ListDirectory
import com.vishnu.whatsappcleaner.model.ListFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainViewModel(private val application: Application) : AndroidViewModel(application) {

    private val storeData = StoreData(application.applicationContext)

    // File lists, one per tab

    private val _fileList = MutableStateFlow<List<ListFile>>(emptyList())
    val fileList: StateFlow<List<ListFile>> = _fileList.asStateFlow()

    private val _sentList = MutableStateFlow<List<ListFile>>(emptyList())
    val sentList: StateFlow<List<ListFile>> = _sentList.asStateFlow()

    private val _privateList = MutableStateFlow<List<ListFile>>(emptyList())
    val privateList: StateFlow<List<ListFile>> = _privateList.asStateFlow()

    private val _loadingTargets = MutableStateFlow<Set<Target>>(emptySet())
    val loadingTargets: StateFlow<Set<Target>> = _loadingTargets.asStateFlow()

    private val _isDeleting = MutableStateFlow(false)
    val isDeleting: StateFlow<Boolean> = _isDeleting.asStateFlow()

    private val _fileReloadTrigger = MutableStateFlow(false)
    val fileReloadTrigger: StateFlow<Boolean> = _fileReloadTrigger.asStateFlow()

    // Home directory sizes

    private val _isTotalSizeLoading = MutableStateFlow(false)
    val isTotalSizeLoading: StateFlow<Boolean> = _isTotalSizeLoading.asStateFlow()

    private val _directoryItem =
        MutableStateFlow<ViewState<Pair<String, List<ListDirectory>>>>(ViewState.Loading)
    val directoryItem: StateFlow<ViewState<Pair<String, List<ListDirectory>>>> =
        _directoryItem.asStateFlow()

    // Byte size of each folder — the single source of truth the home list and total are derived
    // from. Lets a folder be re-measured after a deletion without re-walking every folder.
    private val directorySizeBytes = mutableMapOf<String, Long>()

    // View preference

    val isGridView: StateFlow<Boolean> =
        storeData.isGridViewFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )

    // In-flight work

    // One in-flight file walk per tab; tracked so a walk can be canceled when the user navigates
    // away or reloads
    private val fileListJobs = mutableMapOf<Target, Job>()

    // The in-flight full walk, if any; a deletion joins it before re-measuring so the two can't
    // race
    private var directoryJob: Job? = null

    init {
        getDirectoryList()
    }

    fun toggleViewType() {
        viewModelScope.launch {
            storeData.setGridViewPreference(!storeData.isGridViewFlow.first())
        }
    }

    fun saveHomeUri(homePath: String) {
        Log.i(TAG, "saveHomeUri: $homePath")
        viewModelScope.launch(Dispatchers.Default) {
            storeData.set(Constants.WHATSAPP_HOME_URI, homePath)
        }
    }

    fun getDirectoryList() {
        Log.i(TAG, "getDirectoryList() called")

        // Sizes are cached after the first full walk; skip the expensive re-walk on subsequent
        // visits (deletions re-measure only the affected folder instead)
        if (_isTotalSizeLoading.value || _directoryItem.value is ViewState.Success) return

        _isTotalSizeLoading.value = true

        directoryJob = viewModelScope.launch(Dispatchers.Default) {
            if (!Storage.isReady(application)) {
                _isTotalSizeLoading.value = false
                return@launch
            }

            directorySizeBytes.clear()
            publishDirectoryItem()

            // Compute cheap folders first so most sizes appear within ~1s
            val base = ListDirectory.getDirectoryList("")
            val order = base.indices.sortedBy { directorySizeWeight(base[it].path) }

            for (index in order) {
                val path = base[index].path
                directorySizeBytes[path] = FileRepository.folderSize(application, path)
                publishDirectoryItem()
            }

            _isTotalSizeLoading.value = false
        }
    }

    /**
     * Publishes the home directory list derived from [directorySizeBytes], the single source of
     * truth: each card shows its measured size (or a placeholder if not yet walked) and the banner
     * shows the running total of everything measured so far.
     */
    private fun publishDirectoryItem() {
        val items = ListDirectory.getDirectoryList("").map { directory ->
            val bytes = directorySizeBytes[directory.path]
            directory.copy(
                size = if (bytes == null) CALCULATING
                else FileRepository.formatSize(application, bytes)
            )
        }

        val total = if (directorySizeBytes.isEmpty())
            CALCULATING
        else
            FileRepository.formatSize(application, directorySizeBytes.values.sum())

        _directoryItem.value = ViewState.Success(Pair(total, items))
    }

    /** Folders that recurse or hold thousands of files are slowest; compute them last. */
    private fun directorySizeWeight(path: String): Int = when {
        path.contains("Voice Notes") || path.contains("Video Notes") -> 2
        path.contains("Stickers") -> 1
        else -> 0
    }

    fun getFileList(
        target: Target,
        path: String,
        sortBy: String,
        isSortDescending: Boolean,
        filterStartDate: Long?,
        filterEndDate: Long?
    ) {
        Log.i(TAG, "getFileList: $path")

        // Cancel any walk already running for this tab so its results can't land in the new
        // directory
        fileListJobs.remove(target)?.cancel()

        _loadingTargets.update { it + target }

        val job = viewModelScope.launch(Dispatchers.Default) {
            if (!Storage.isReady(application)) {
                _loadingTargets.update { it - target }
                return@launch
            }

            // Stream partial results into the list as the walk progresses (Voice Notes can take
            // tens of seconds over SAF), then publish the final, fully-sorted list. The isActive
            // guards drop any write from a walk that was canceled while still winding down.
            val fileList = FileRepository.getFileList(application, path) { partial ->
                if (isActive)
                    setFileList(
                        target,
                        sortAndFilter(
                            partial,
                            sortBy,
                            isSortDescending,
                            filterStartDate,
                            filterEndDate
                        )
                    )
            }

            if (isActive) {
                setFileList(
                    target,
                    sortAndFilter(
                        fileList,
                        sortBy,
                        isSortDescending,
                        filterStartDate,
                        filterEndDate
                    )
                )

                _loadingTargets.update { it - target }
            }
        }

        fileListJobs[target] = job
    }

    private fun setFileList(target: Target, list: List<ListFile>) {
        when (target) {
            Target.Received -> _fileList.value = list
            Target.Sent -> _sentList.value = list
            Target.Private -> _privateList.value = list
        }
    }

    private fun sortAndFilter(
        source: List<ListFile>,
        sortBy: String,
        isSortDescending: Boolean,
        filterStartDate: Long?,
        filterEndDate: Long?
    ): List<ListFile> {
        val baseComparator: Comparator<ListFile> = when {
            sortBy.contains("Name") -> compareBy { it.name }
            sortBy.contains("Size") -> compareBy { it.sizeBytes }
            else -> compareBy { it.dateModified }
        }
        val comparator = if (isSortDescending) baseComparator.reversed() else baseComparator

        val filtered =
            if (sortBy.contains("Date") && filterStartDate != null && filterEndDate != null)
                source.filter { it.dateModified > filterStartDate && it.dateModified < filterEndDate }
            else source

        return filtered.sortedWith(comparator)
    }

    fun delete(directory: ListDirectory, fileList: List<ListFile>) {
        Log.i(TAG, "delete() called with: fileList = $fileList")

        _isDeleting.value = true

        viewModelScope.launch(Dispatchers.IO) {
            FileRepository.deleteFiles(application, fileList)

            // Only the folder we deleted from changes
            recalculateDirectorySize(directory.path)

            _isDeleting.value = false

            // Re-walk the directory so the lists reflect what's actually on disk — a deletion can
            // partially fail, so we can't just drop the requested files from the lists
            _fileReloadTrigger.value = !_fileReloadTrigger.value
        }
    }

    /** Re-measures a single folder after a deletion and republishes the list and total. */
    private suspend fun recalculateDirectorySize(path: String) {
        // Wait for any in-flight full walk to finish first, so we don't race its size writes
        directoryJob?.join()

        directorySizeBytes[path] = FileRepository.folderSize(application, path)

        publishDirectoryItem()
    }

    fun clearFileListStates() {
        fileListJobs.values.forEach { it.cancel() }
        fileListJobs.clear()
        _loadingTargets.value = emptySet()
        _fileList.value = emptyList()
        _sentList.value = emptyList()
        _privateList.value = emptyList()
    }
}

sealed class Target {
    data object Received : Target()
    data object Sent : Target()
    data object Private : Target()
}

sealed class ViewState<out T> {
    data object Loading : ViewState<Nothing>()
    data class Success<T>(val data: T) : ViewState<T>()
    data class Error(val message: String) : ViewState<Nothing>()
}

class MainViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
