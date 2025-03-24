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
import androidx.lifecycle.MutableLiveData
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
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

class MainViewModel(private val application: Application) : AndroidViewModel(application) {

    private val contentResolver = application.contentResolver

    private val storeData = StoreData(application.applicationContext)

    private val _directoryItem =
        MutableStateFlow<ViewState<Pair<String, List<ListDirectory>>>>(ViewState.Loading)
    val directoryItem: StateFlow<ViewState<Pair<String, List<ListDirectory>>>> =
        _directoryItem.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.Default) {
            getDirectoryList()
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

    fun getHomeUri(): MutableLiveData<String> {
        val mutableLiveData = MutableLiveData<String>()
        viewModelScope.launch(Dispatchers.Default) {
            mutableLiveData.postValue(storeData.get(Constants.WHATSAPP_HOME_URI))
        }
        return mutableLiveData
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
        path: String,
        sortBy: String,
        isSortDescending: Boolean,
        filterStartDate: Long?,
        filterEndDate: Long?,
    ): MutableLiveData<ArrayList<ListFile>> {
        Log.i("vishnu", "getFileList: $path")

        val mutableLiveData = MutableLiveData<ArrayList<ListFile>>(
            FileRepository.getLoadingList()
        )

        viewModelScope.launch(Dispatchers.Default) {
            val fileList = FileRepository.getFileList(application, path)

            fileList.sortWith(
                if (sortBy.contains("Name"))
                    compareBy { it.name }
                else if (sortBy.contains("Size"))
                    compareBy { it.length() }
                else
                    compareBy { it.lastModified() }
            )

            if (filterStartDate != null && filterEndDate != null) {
                val filteredList = fileList.filter {
                    Log.e(
                        "vishnu",
                        "${DateFormat.getDateInstance().format(filterStartDate)} ::" +
                            "${DateFormat.getDateInstance().format(it.lastModified())} ::" +
                            "${DateFormat.getDateInstance().format(filterEndDate)} ::"
                    )

                    val lastModified = Date(it.lastModified())

                    lastModified.after(Date(filterStartDate)) && lastModified.before(Date(filterEndDate))
                }

                fileList.clear()
                fileList.addAll(filteredList)
            }

            if (isSortDescending)
                fileList.reverse()

            mutableLiveData.postValue(fileList)
        }

        return mutableLiveData
    }

    fun listDirectories(path: String): MutableLiveData<ArrayList<String>> {
        Log.i("vishnu", "listDirectories: $path")

        val mutableLiveData = MutableLiveData<ArrayList<String>>()

        viewModelScope.launch(Dispatchers.Default) {
            mutableLiveData.postValue(
                FileRepository.getDirectoryList(path)
            )
        }

        return mutableLiveData
    }

    fun delete(fileList: List<ListFile>): MutableLiveData<Boolean> {
        Log.i("vishnu", "delete() called with: fileList = $fileList")

        val mutableLiveData = MutableLiveData<Boolean>(true)

        viewModelScope.launch(Dispatchers.IO) {
            mutableLiveData.postValue(
                FileRepository.deleteFiles(fileList)
            )
        }

        return mutableLiveData
    }
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
