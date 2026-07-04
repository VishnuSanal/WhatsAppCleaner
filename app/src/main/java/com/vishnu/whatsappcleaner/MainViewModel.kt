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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

class MainViewModel(private val application: Application) : AndroidViewModel(application) {

    private val storeData = StoreData(application.applicationContext)

    // File listing — driven reactively from the active directory and the current filter.

    // The directory currently open in the details screen; null when no details screen is shown.
    // Setting it (or bumping [reloadSignal]) is the only thing that kicks off a disk walk.
    private val activeDirectory = MutableStateFlow<ListDirectory?>(null)
    private val reloadSignal = MutableStateFlow(0)

    // Raw, unsorted walk results per tab. The expensive walk only re-runs on a directory change or
    // a reload (after a deletion) — never on a sort/filter change, which is pure in-memory work.
    private val rawFiles = MutableStateFlow<Map<Target, List<ListFile>>>(emptyMap())

    // Sort + date-filter criteria. Owned by the ViewModel so it survives configuration changes and
    // so the UI never has to re-issue a query — it just collects [files].
    private val _filter = MutableStateFlow(FileFilter())
    val filter: StateFlow<FileFilter> = _filter.asStateFlow()

    private val _loadingTargets = MutableStateFlow<Set<Target>>(emptySet())
    val loadingTargets: StateFlow<Set<Target>> = _loadingTargets.asStateFlow()

    private val _isDeleting = MutableStateFlow(false)
    val isDeleting: StateFlow<Boolean> = _isDeleting.asStateFlow()

    // What the UI renders: the raw walk results run through the current sort/filter. Re-derives
    // instantly whenever the raw files stream in or the filter changes, without touching disk.
    val files: StateFlow<Map<Target, List<ListFile>>> =
        combine(rawFiles, _filter) { raw, filter ->
            raw.mapValues { (_, list) -> sortAndFilter(list, filter) }
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

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

    // The in-flight full walk, if any; a deletion joins it before re-measuring so the two can't
    // race
    private var directoryJob: Job? = null

    init {
        getDirectoryList()

        // A single pipeline owns file loading. Whenever the active directory or the reload signal
        // changes, collectLatest cancels the previous walk — which covers both navigating away
        // (directory set to null) and reloading after a delete — and starts a fresh one. This
        // replaces the hand-rolled per-tab job tracking and the boolean reload toggle.
        viewModelScope.launch {
            combine(activeDirectory, reloadSignal) { directory, _ -> directory }
                .collectLatest { directory ->
                    if (directory == null) {
                        rawFiles.value = emptyMap()
                        _loadingTargets.value = emptySet()
                    } else {
                        loadFiles(directory)
                    }
                }
        }
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

    /**
     * Opens [directory] in the details screen. Idempotent for the same path so recomposition
     * doesn't re-walk; switching to a different directory resets the filter to its defaults (the
     * filter is shown fresh per directory) and clears the previous results.
     */
    fun setActiveDirectory(directory: ListDirectory) {
        if (activeDirectory.value?.path == directory.path) return

        _filter.value = FileFilter()
        rawFiles.value = emptyMap()
        // Mark every tab loading up front so a slow tab never flashes its empty state first.
        _loadingTargets.value = targetsFor(directory).map { it.first }.toSet()
        activeDirectory.value = directory
    }

    /** Called when the details screen leaves; cancels any in-flight walk and clears the lists. */
    fun clearActiveDirectory() {
        activeDirectory.value = null
    }

    fun setSort(sortBy: SortBy, descending: Boolean) {
        _filter.update { it.copy(sortBy = sortBy, descending = descending) }
    }

    fun setDateFilter(startDate: Long?, endDate: Long?) {
        _filter.update { it.copy(startDate = startDate, endDate = endDate) }
    }

    /** The tabs to walk for [directory], each paired with its path relative to the WhatsApp root. */
    private fun targetsFor(directory: ListDirectory): List<Pair<Target, String>> = buildList {
        add(Target.Received to directory.path)
        if (directory.hasSent) add(Target.Sent to "${directory.path}/Sent")
        if (directory.hasPrivate) add(Target.Private to "${directory.path}/Private")
    }

    /**
     * Walks every tab of [directory] concurrently, streaming partial results into [rawFiles] as
     * they arrive. Runs inside the collectLatest pipeline, so navigating away or reloading cancels
     * the whole walk in one shot ([supervisorScope] keeps this suspended until all tabs finish
     * while isolating a single tab's failure so it can't abort the others or the pipeline).
     */
    private suspend fun loadFiles(directory: ListDirectory) = supervisorScope {
        val targets = targetsFor(directory)

        rawFiles.value = emptyMap()
        _loadingTargets.value = targets.map { it.first }.toSet()

        if (!Storage.isReady(application)) {
            _loadingTargets.value = emptySet()
            return@supervisorScope
        }

        targets.forEach { (target, path) ->
            launch(Dispatchers.Default) {
                // Voice Notes can take tens of seconds over SAF; stream partials so the list fills
                // in as the walk progresses. The isActive guards drop any write from a walk that
                // was canceled while still winding down.
                try {
                    val result = FileRepository.getFileList(application, path) { partial ->
                        if (isActive) setRawFiles(target, partial)
                    }
                    if (isActive) setRawFiles(target, result)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // One tab failing (e.g. a SAF error) must not sink the others; log and let this
                    // tab settle to whatever partial results it managed.
                    Log.e(TAG, "loadFiles: failed to walk $path", e)
                } finally {
                    if (isActive) _loadingTargets.update { it - target }
                }
            }
        }
    }

    private fun setRawFiles(target: Target, list: List<ListFile>) {
        rawFiles.update { it + (target to list) }
    }

    private fun sortAndFilter(source: List<ListFile>, filter: FileFilter): List<ListFile> {
        val baseComparator: Comparator<ListFile> = when (filter.sortBy) {
            SortBy.NAME -> compareBy { it.name }
            SortBy.SIZE -> compareBy { it.sizeBytes }
            SortBy.DATE -> compareBy { it.dateModified }
        }
        val comparator = if (filter.descending) baseComparator.reversed() else baseComparator

        val start = filter.startDate
        val end = filter.endDate
        val filtered =
            if (filter.sortBy == SortBy.DATE && start != null && end != null)
                source.filter { it.dateModified > start && it.dateModified < end }
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
            reloadSignal.update { it + 1 }
        }
    }

    /** Re-measures a single folder after a deletion and republishes the list and total. */
    private suspend fun recalculateDirectorySize(path: String) {
        // Wait for any in-flight full walk to finish first, so we don't race its size writes
        directoryJob?.join()

        directorySizeBytes[path] = FileRepository.folderSize(application, path)

        publishDirectoryItem()
    }
}

sealed class Target {
    data object Received : Target()
    data object Sent : Target()
    data object Private : Target()
}

/** What the file list is sorted by; [label] is the user-facing name shown in the sort dialog. */
enum class SortBy(val label: String) {
    DATE("Date"),
    SIZE("Size"),
    NAME("Name"),
}

/** The sort + date-range filter applied to the walked files before they reach the UI. */
data class FileFilter(
    val sortBy: SortBy = SortBy.DATE,
    val descending: Boolean = true,
    val startDate: Long? = null,
    val endDate: Long? = null,
)

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
