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
import com.vishnu.whatsappcleaner.data.FileRepository
import com.vishnu.whatsappcleaner.data.StoreData
import com.vishnu.whatsappcleaner.model.ListDirectory
import com.vishnu.whatsappcleaner.model.ListFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Date

class MainViewModel(private val application: Application) : AndroidViewModel(application) {
    private val _fileList = MutableStateFlow<List<ListFile>>(emptyList())
    val fileList: StateFlow<List<ListFile>> = _fileList.asStateFlow()

    private val _sentList = MutableStateFlow<List<ListFile>>(emptyList())
    val sentList: StateFlow<List<ListFile>> = _sentList.asStateFlow()

    private val _privateList = MutableStateFlow<List<ListFile>>(emptyList())
    val privateList: StateFlow<List<ListFile>> = _privateList.asStateFlow()

    private val _isInProgress = MutableStateFlow(false)
    val isInProgress: StateFlow<Boolean> = _isInProgress.asStateFlow()

    private val _directories = MutableStateFlow<List<String>>(emptyList())
    val directories: StateFlow<List<String>> = _directories

    private val _homeUri = MutableStateFlow<String?>("")
    val homeUri: StateFlow<String?> = _homeUri.asStateFlow()

    private val _fileReloadTrigger = MutableStateFlow(false)
    val fileReloadTrigger: StateFlow<Boolean> = _fileReloadTrigger.asStateFlow()

    private val storeData = StoreData(application.applicationContext)

    private val _isGridView = MutableStateFlow(false)
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    private val _directoryItem =
        MutableStateFlow<ViewState<Pair<String, List<ListDirectory>>>>(ViewState.Loading)
    val directoryItem: StateFlow<ViewState<Pair<String, List<ListDirectory>>>> =
        _directoryItem.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.Default) {
            storeData.isGridViewFlow.collect {
                _isGridView.value = it
            }
            getDirectoryList()
        }
    }

    fun toggleViewType() {
        viewModelScope.launch {
            val current = storeData.isGridViewFlow.first()
            val toggled = !current
            storeData.setGridViewPreference(toggled)
        }
    }

    fun saveHomeUri(homePath: String) {
        Log.i("vishnu", "saveHomeUri: $homePath")
        viewModelScope.launch(Dispatchers.Default) {
            storeData.set(
                Constants.WHATSAPP_HOME_URI,
                homePath
            )
        }
    }

    fun getHomeUri() {
        viewModelScope.launch(Dispatchers.Default) {
            _homeUri.value = storeData.get(Constants.WHATSAPP_HOME_URI)
        }
    }

    fun getDirectoryList() {
        Log.i("vishnu", "getDirectoryList() called")

        viewModelScope.launch(Dispatchers.Default) {
            storeData.get(Constants.WHATSAPP_HOME_URI)
                ?.let { homeUri ->
                    val pair = FileRepository.getDirectoryList(
                        application,
                        homeUri
                    )
                    Log.e("vishnu", "getDirectoryList: $pair")
                    _directoryItem.value = ViewState.Success(pair)
                }
        }
    }

    fun getFileList(
        target: Target,
        path: String,
        sortBy: String,
        isSortDescending: Boolean,
        filterStartDate: Long?,
        filterEndDate: Long?
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            val fileList = FileRepository.getFileList(application, path)

            fileList.sortWith(
                when {
                    sortBy.contains("Name") -> compareBy { it.name }
                    sortBy.contains("Size") -> compareBy { it.length() }
                    else -> compareBy { it.lastModified() }
                }
            )

            if (sortBy.contains("Date") && filterStartDate != null && filterEndDate != null) {
                val filteredList = fileList.filter {
                    val lastModified = Date(it.lastModified())
                    lastModified.after(Date(filterStartDate)) && lastModified.before(
                        Date(
                            filterEndDate
                        )
                    )
                }
                fileList.clear()
                fileList.addAll(filteredList)
            }

            if (isSortDescending) fileList.reverse()

            when (target) {
                Target.Received -> _fileList.value = fileList
                Target.Sent -> _sentList.value = fileList
                Target.Private -> _privateList.value = fileList
            }
        }
    }

    fun listDirectories(path: String) {
        Log.i("vishnu", "listDirectories: $path")

        viewModelScope.launch(Dispatchers.Default) {
            val dirList = FileRepository.getDirectoryList(path)
            _directories.value = dirList
        }
    }

    fun delete(fileList: List<ListFile>) {
        Log.i("vishnu", "delete() called with: fileList = $fileList")

        _isInProgress.value = true
        viewModelScope.launch(Dispatchers.IO) {
            FileRepository.deleteFiles(fileList)
            _isInProgress.value = false
            _fileReloadTrigger.value = !_fileReloadTrigger.value
        }
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
