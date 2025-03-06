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

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavHostController
import com.vishnu.whatsappcleaner.Constants
import com.vishnu.whatsappcleaner.MainViewModel
import com.vishnu.whatsappcleaner.R
import com.vishnu.whatsappcleaner.model.ListDirectory
import com.vishnu.whatsappcleaner.model.ListFile
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Composable
fun DetailsScreen(navController: NavHostController, viewModel: MainViewModel) {
    val listDirectory = navController.previousBackStackEntry?.savedStateHandle?.get<ListDirectory>(
        Constants.DETAILS_LIST_ITEM
    )

    if (listDirectory == null) return Surface {}

    var fileList = remember { mutableStateListOf<ListFile>() }
    var sentList = remember { mutableStateListOf<ListFile>() }
    var privateList = remember { mutableStateListOf<ListFile>() }

    var selectedItems = remember { mutableStateListOf<ListFile>() }

    var sortBy = remember { mutableStateOf("Date") }
    var isSortDescending = remember { mutableStateOf(true) }

    var isInProgress by remember { mutableStateOf(false) }
    var showConfirmationDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }

    var isAllSelected by remember { mutableStateOf(false) }

    LaunchedEffect(isInProgress, sortBy.value, isSortDescending.value) {
        viewModel.getFileList(listDirectory.path, sortBy.value, isSortDescending.value)
            .observeForever {
                fileList.clear()
                fileList.addAll(it)
            }

        if (listDirectory.hasSent) viewModel.getFileList(
            "${listDirectory.path}/Sent",
            sortBy.value,
            isSortDescending.value
        )
            .observeForever {
                sentList.clear()
                sentList.addAll(it)
            }

        if (listDirectory.hasPrivate) viewModel.getFileList(
            "${listDirectory.path}/Private",
            sortBy.value,
            isSortDescending.value
        )
            .observeForever {
                privateList.clear()
                privateList.addAll(it)
            }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            Modifier.padding(top = 16.dp, bottom = 8.dp, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Title(
                    Modifier
                        .padding(0.dp),
                    listDirectory.name
                )

                Spacer(Modifier.weight(1f))

                IconButton(
                    modifier = Modifier
                        .size(32.dp)
                        .padding(horizontal = 4.dp),
                    onClick = {
                        showSortDialog = true
                    }
                ) {
                    Icon(
                        modifier = Modifier
                            .size(32.dp),
                        painter = painterResource(id = R.drawable.ic_sort),
                        tint = MaterialTheme.colorScheme.primary,
                        contentDescription = "sort",
                    )
                }
            }

            Banner(
                Modifier.padding(16.dp),
                buildAnnotatedString {
                    var size = listDirectory.size

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
            )

            val pagerState = rememberPagerState(pageCount = {
                if (listDirectory.hasSent)
                    if (listDirectory.hasPrivate) 3
                    else 2
                else 1
            })

            var currentList: SnapshotStateList<ListFile> = fileList
            val coroutineScope = rememberCoroutineScope()

            if (listDirectory.hasSent || listDirectory.hasPrivate)
                Row(
                    modifier = Modifier.padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val arr = arrayListOf("Received")

                    if (listDirectory.hasSent)
                        arr.add("Sent")

                    if (listDirectory.hasPrivate)
                        arr.add("Private")

                    for (s in arr) {
                        TextButton(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(4.dp, 8.dp, 4.dp, 0.dp)
                                .border(
                                    BorderStroke(
                                        2.dp,
                                        if (arr[pagerState.settledPage] != s) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.background,
                                    ),
                                    RoundedCornerShape(64.dp),
                                ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (arr[pagerState.settledPage] == s) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.background
                            ),
                            shape = RoundedCornerShape(64.dp),
                            contentPadding = PaddingValues(vertical = 12.dp),
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.scrollToPage(
                                        arr.indexOf(s)
                                    )
                                }
                            }
                        ) {
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.onPrimaryContainer)) {
                                        append(s)
                                    }
                                },
                                style = MaterialTheme.typography.headlineSmall,
                            )
                        }
                    }
                }

            IconButton(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(8.dp)
                    .size(32.dp)
                    .padding(4.dp),
                onClick = {
                    isAllSelected = !isAllSelected

                    if (isAllSelected)
                        selectedItems.addAll(currentList.toList())
                    else
                        selectedItems.clear()
                }
            ) {
                Icon(
                    modifier = Modifier
                        .size(32.dp),
                    painter = painterResource(id = if (isAllSelected) R.drawable.check_circle_filled else R.drawable.check_circle),
                    tint = MaterialTheme.colorScheme.primary,
                    contentDescription = "select all",
                )
            }

            LaunchedEffect(pagerState) {
                snapshotFlow {
                    pagerState.currentPage
                }.distinctUntilChanged().collect { _ ->
                    selectedItems.clear()
                    isAllSelected = false
                }
            }

            if (isInProgress) LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(8.dp),
            )

            key(isAllSelected) {
                HorizontalPager(
                    modifier = Modifier.weight(1f),
                    state = pagerState
                ) { page ->

                    if (pagerState.currentPage == 0) {
                        currentList = fileList
                    } else if (pagerState.currentPage == 1) {
                        currentList = sentList
                    } else {
                        currentList = privateList
                    }

                    if (currentList.isNotEmpty()) {
                        LazyVerticalGrid(
                            modifier = Modifier.fillMaxSize(),
                            columns = GridCells.Fixed(3),
                        ) {
                            items(currentList) {
                                ItemCard(
                                    it,
                                    navController,
                                    isSelected = selectedItems.contains(it)
                                ) {
                                    if (selectedItems.contains(it))
                                        selectedItems.remove(it)
                                    else
                                        selectedItems.add(it)
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                modifier = Modifier
                                    .fillMaxSize(0.4f)
                                    .padding(8.dp),
                                painter = painterResource(id = R.drawable.clean),
                                contentDescription = "empty",
                                tint = MaterialTheme.colorScheme.secondaryContainer
                            )

                            Text(
                                modifier = Modifier,
                                text = "Nothing to clean",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }

            TextButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(64.dp),
                contentPadding = PaddingValues(12.dp),
                onClick = {
                    if (selectedItems.isNotEmpty())
                        showConfirmationDialog = true
                    else
                        Toast.makeText(
                            navController.context,
                            "Select files to cleanup!",
                            Toast.LENGTH_SHORT
                        )
                            .show()
                }
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                letterSpacing = 1.sp
                            )
                        ) {
                            append("Cleanup")
                        }
                    },
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }
    }

    if (showSortDialog) {
        SortDialog(
            navController,
            onDismissRequest = {
                showSortDialog = false
            },
            sortBy,
            isSortDescending
        )
    }

    if (showConfirmationDialog) {
        ConfirmationDialog(
            onDismissRequest = {
                showConfirmationDialog = false
            },
            onConfirmation = {
                viewModel.delete(selectedItems.toList())
                    .observeForever {
                        isInProgress = it
                        navController.previousBackStackEntry?.savedStateHandle?.apply {
                            set(Constants.FORCE_RELOAD_FILE_LIST, true)
                        }
                    }

                showConfirmationDialog = false
                selectedItems.clear()
            },
            selectedItems,
            navController
        )
    }
}

@Composable
fun SortDialog(
    navController: NavHostController,
    onDismissRequest: () -> Unit,
    sortBy: MutableState<String>,
    isSortDescending: MutableState<Boolean>
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = true,
            dismissOnBackPress = true,
            decorFitsSystemWindows = true
        ),
    ) {
        var isDescending by remember { mutableStateOf(isSortDescending) }
        var selectedItem by remember { mutableStateOf(sortBy) }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(vertical = 64.dp, horizontal = 32.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(16.dp),
            ) {
                Text(
                    modifier = Modifier
                        .wrapContentHeight()
                        .padding(8.dp),
                    text = "Sort Criteria",
                    style = MaterialTheme.typography.headlineLarge,
                )

                listOf(
                    "Date",
                    "Size",
                    "Name",
                ).forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedItem.value = item
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = sortBy.value == item,
                            onClick = {
                                selectedItem.value = item
                            },
                            enabled = true,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Text(text = item, modifier = Modifier.padding(start = 8.dp))
                    }
                }

                Row(
                    modifier = Modifier
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = isDescending.value,
                        onCheckedChange = { isDescending.value = it }
                    )

                    Text(text = "Descending", modifier = Modifier.padding(start = 8.dp))
                }

                TextButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(4.dp),
                    onClick = {
                        sortBy.value = selectedItem.value
                        isSortDescending.value = isDescending.value
                        onDismissRequest()
                    }
                ) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(
                                SpanStyle(
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Medium,
                                    letterSpacing = 1.sp
                                )
                            ) {
                                append("Apply")
                            }
                        },
                        fontWeight = FontWeight.Medium,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
fun ConfirmationDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
    list: List<ListFile>,
    navController: NavHostController
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = true,
            dismissOnBackPress = true,
            decorFitsSystemWindows = true
        ),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(vertical = 64.dp, horizontal = 32.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(16.dp),
            ) {
                Row(
                    modifier = Modifier.wrapContentHeight(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Column(
                        Modifier
                            .weight(0.6f)
                            .fillMaxWidth()
                    ) {
                        Text(
                            modifier = Modifier
                                .wrapContentHeight()
                                .padding(vertical = 4.dp)
                                .align(Alignment.Start),
                            text = "Confirm Cleanup",
                            style = MaterialTheme.typography.titleLarge,
                        )

                        Text(
                            modifier = Modifier
                                .wrapContentHeight()
                                .padding(vertical = 2.dp)
                                .align(Alignment.Start),
                            text = "The following files will be deleted.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                    TextButton(
                        modifier = Modifier
                            .weight(0.4f)
                            .fillMaxWidth()
                            .padding(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(vertical = 16.dp, horizontal = 16.dp),
                        onClick = onConfirmation,
                        content = {
                            Text(
                                text = "Confirm",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                        },
                    )
                }

                // todo: no preview & replace it with count + red colored CTA
                LazyVerticalGrid(
                    modifier = Modifier
                        .wrapContentHeight(),
                    columns = GridCells.Fixed(3),
                ) {
                    items(list) { ItemCard(it, navController, selectionEnabled = false) {} }
                }
            }
        }
    }
}
