/*
 * Copyright (C) 2025 Vishnu Sanal T
 *
 * This file is part of WhatsApp Cleaner.
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

package com.vishnu.whatsappcleaner.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.valentinilk.shimmer.shimmer
import com.vishnu.whatsappcleaner.BuildConfig
import com.vishnu.whatsappcleaner.Constants
import com.vishnu.whatsappcleaner.MainViewModel
import com.vishnu.whatsappcleaner.R
import com.vishnu.whatsappcleaner.ViewState
import com.vishnu.whatsappcleaner.model.ListDirectory

@Composable
fun HomeScreen(navController: NavHostController, viewModel: MainViewModel) {
    var forceReload by remember { mutableStateOf(false) }

    forceReload = navController.currentBackStackEntry?.savedStateHandle?.get<Boolean>(
        Constants.FORCE_RELOAD_FILE_LIST
    ) ?: false

    val directoryItem: State<ViewState<Pair<String, List<ListDirectory>>>> =
        viewModel.directoryItem.collectAsState()

    if (forceReload) {
        viewModel.getDirectoryList()
    }

    // trigger when moving from permission screen to home screen on Android 15
    LaunchedEffect(key1 = null) {
        viewModel.getDirectoryList()
    }

    Surface(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            Modifier.padding(top = 16.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Title(
                Modifier
                    .padding(0.dp)
                    .align(Alignment.Start),

                stringResource(R.string.app_name)
            )

            val modifier = if (directoryItem.value is ViewState.Success) Modifier
            else Modifier.shimmer()

            Banner(
                modifier.padding(16.dp),
                buildAnnotatedString {
                    when (directoryItem.value) {
                        is ViewState.Success -> {
                            var size = (directoryItem.value as ViewState.Success).data.first

                            if (size.contains(" ")) {
                                val split = size.split(" ")
                                withStyle(SpanStyle(fontSize = 24.sp)) {
                                    append(split.get(0))
                                }
                                withStyle(SpanStyle(fontSize = 18.sp)) {
                                    append(" ${split.get(1)}")
                                }
                            } else {
                                withStyle(SpanStyle(fontSize = 24.sp)) {
                                    append(size)
                                }
                            }
                        }

                        is ViewState.Loading -> withStyle(SpanStyle(fontSize = 18.sp)) {
                            append("Loading...")
                        }

                        is ViewState.Error -> withStyle(SpanStyle(fontSize = 18.sp)) {
                            append("Error")
                        }
                    }
                }
            )

            Text(
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(16.dp, 16.dp, 16.dp, 8.dp),
                text = "Explore",
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleLarge,
            )

            when (directoryItem.value) {
                is ViewState.Success -> {
                    LazyColumn(Modifier.weight(1f)) {
                        items((directoryItem.value as ViewState.Success<Pair<String, List<ListDirectory>>>).data.second) {
                            SingleCard(it, navController)
                        }
                    }
                }

                is ViewState.Loading -> LazyColumn(Modifier.weight(1f)) {
                    items(ListDirectory.getDirectoryList(Constants.LIST_LOADING_INDICATION)) {
                        SingleCard(it, navController)
                    }
                }

                is ViewState.Error -> Text(
                    modifier = Modifier
                        .fillMaxSize()
                        .align(Alignment.CenterHorizontally)
                        .padding(16.dp),
                    text = "Error loading directory list\n${(directoryItem.value as ViewState.Error).message}",
                    style = MaterialTheme.typography.labelSmall,
                )
            }

            if (BuildConfig.DEBUG)
                Text(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(4.dp),
                    text = "Debug Build ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.labelSmall,
                )
        }
    }
}
