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

package com.vishnu.whatsappcleaner.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DateRangePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
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
import com.vishnu.whatsappcleaner.SortBy
import com.vishnu.whatsappcleaner.Target
import com.vishnu.whatsappcleaner.ViewState
import com.vishnu.whatsappcleaner.model.ListDirectory
import com.vishnu.whatsappcleaner.model.ListFile
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(navController: NavHostController, viewModel: MainViewModel) {
    val listDirectory = navController.previousBackStackEntry?.savedStateHandle?.get<ListDirectory>(
        Constants.DETAILS_LIST_ITEM
    )

    if (listDirectory == null) return Surface {}

    val coroutineScope = rememberCoroutineScope()

    val files by viewModel.files.collectAsState()
    val filter by viewModel.filter.collectAsState()

    val loadingTargets by viewModel.loadingTargets.collectAsState()
    val isDeleting by viewModel.isDeleting.collectAsState()
    val isGridView by viewModel.isGridView.collectAsState()
    val directoryItem by viewModel.directoryItem.collectAsState()

    val directorySize = (directoryItem as? ViewState.Success)
        ?.data?.second?.firstOrNull { it.path == listDirectory.path }?.size
        ?: listDirectory.size

    var selectedItems = remember { mutableStateListOf<ListFile>() }

    val dateRangePickerState = rememberDateRangePickerState()

    var showConfirmationDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    var isAllSelected by remember { mutableStateOf(false) }

    val tabs = listOf("Received", "Sent", "Private")

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = {
            if (listDirectory.hasSent) {
                if (listDirectory.hasPrivate) 3
                else 2
            } else 1
        }
    )

    fun targetForPage(page: Int) = when (page) {
        0 -> Target.Received
        1 -> Target.Sent
        else -> Target.Private
    }

    val isCurrentTabLoading = targetForPage(pagerState.currentPage) in loadingTargets
    val isInProgress = isDeleting || isCurrentTabLoading

    val gridStates = remember {
        List(3) { LazyGridState() }
    }
    val listStates = remember {
        List(3) { LazyListState() }
    }

    val showHeader by remember {
        derivedStateOf {
            if (isGridView) {
                val state = gridStates[pagerState.currentPage]
                state.firstVisibleItemIndex == 0 && state.firstVisibleItemScrollOffset == 0
            } else {
                val state = listStates[pagerState.currentPage]
                state.firstVisibleItemIndex == 0 && state.firstVisibleItemScrollOffset == 0
            }
        }
    }

    // The ViewModel owns the walk: tell it which directory is open and it streams the files,
    // re-walking only on a directory change or a reload (after a delete). Sorting and date
    // filtering are applied reactively to the already-walked files, so they never re-walk.
    LaunchedEffect(listDirectory.path) {
        viewModel.setActiveDirectory(listDirectory)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearActiveDirectory()
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        if (selectedItems.isNotEmpty() || isAllSelected) {
            selectedItems.clear()
            isAllSelected = false
        }
    }

    Scaffold(
        modifier = Modifier.background(MaterialTheme.colorScheme.background),
        topBar = {
            DetailScreenTopBar(
                title = listDirectory.name,
                toggleGridView = {
                    viewModel.toggleViewType()
                },
                isGridView = isGridView,
                onSortClick = {
                    showSortDialog = true
                    dateRangePickerState.setSelection(filter.startDate, filter.endDate)
                    selectedItems.clear()
                    isAllSelected = false
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isInProgress) LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(8.dp),
            )

            AnimatedVisibility(
                visible = showHeader,
                enter = fadeIn(animationSpec = tween(200)) +
                    slideInVertically(
                        animationSpec = tween(200),
                        initialOffsetY = { it / 8 }
                    ),
                exit = fadeOut(animationSpec = tween(150)) +
                    slideOutVertically(
                        animationSpec = tween(150),
                        targetOffsetY = { it / 8 }
                    )
            ) {
                Banner(
                    Modifier.padding(16.dp),
                    buildAnnotatedString {
                        val size = directorySize
                        val parts = size.split(" ")
                        if (parts.size == 2) {
                            withStyle(SpanStyle(fontSize = 24.sp)) { append(parts[0]) }
                            withStyle(SpanStyle(fontSize = 18.sp)) { append(" ${parts[1]}") }
                        } else {
                            withStyle(SpanStyle(fontSize = 24.sp)) { append(size) }
                        }
                    }
                )
            }

            if (listDirectory.hasSent || listDirectory.hasPrivate) {
                CustomTabLayout(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    selectedItemIndex = pagerState.currentPage,
                    items = tabs,
                    onTabSelected = { index ->
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    }
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                val list = files[targetForPage(page)].orEmpty()

                Column(
                    Modifier
                        .fillMaxSize()
                ) {
                    if (list.isNotEmpty()) {
                        IconButton(
                            modifier = Modifier
                                .align(Alignment.End)
                                .padding(8.dp)
                                .size(32.dp),
                            onClick = {
                                isAllSelected = !isAllSelected
                                selectedItems.clear()
                                if (isAllSelected)
                                    selectedItems.addAll(list)
                            }
                        ) {
                            Icon(
                                modifier = Modifier.size(32.dp),
                                painter = painterResource(id = if (isAllSelected) R.drawable.check_circle_filled else R.drawable.check_circle),
                                tint = MaterialTheme.colorScheme.primary,
                                contentDescription = "select all"
                            )
                        }
                    }

                    if (list.isNotEmpty()) {
                        if (isGridView) {
                            LazyVerticalGrid(
                                state = gridStates[page],
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp),
                                columns = GridCells.Fixed(Constants.DETAILS_GRID_COLUMN_COUNT)
                            ) {
                                items(
                                    items = list,
                                    key = { it.uri }
                                ) {
                                    ItemGridCard(it, navController, selectedItems.contains(it)) {
                                        if (selectedItems.contains(it)) selectedItems.remove(it)
                                        else selectedItems.add(it)
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                state = listStates[page],
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                            ) {
                                items(
                                    items = list,
                                    key = { it.uri }
                                ) {
                                    ItemListCard(it, navController, selectedItems.contains(it)) {
                                        if (selectedItems.contains(it)) selectedItems.remove(it)
                                        else selectedItems.add(it)
                                    }
                                }
                            }
                        }
                    } else if (targetForPage(page) !in loadingTargets && !isDeleting) {
                        // Only call a tab empty once its walk has finished — otherwise a slow tab
                        // would flash "nothing to clean" while still loading
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
                                text = "Nothing to clean",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            CleanUpButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                selectedItems = selectedItems,
                onShowDialog = { showConfirmationDialog = true }
            )
        }
    }

    if (showSortDialog) {
        SortDialog(
            onDismissRequest = {
                showSortDialog = false
            },
            currentSortBy = filter.sortBy,
            currentDescending = filter.descending,
            dateRangePickerState = dateRangePickerState,
            onApply = { sortBy, descending ->
                viewModel.setSort(sortBy, descending)
                viewModel.setDateFilter(
                    dateRangePickerState.selectedStartDateMillis,
                    dateRangePickerState.selectedEndDateMillis
                )
            }
        )
    }

    if (showConfirmationDialog) {
        CleanupConfirmationDialog(
            onDismissRequest = {
                showConfirmationDialog = false
            },
            onConfirmation = {
                viewModel.delete(listDirectory, selectedItems.toList())
                showConfirmationDialog = false
                isAllSelected = false
                selectedItems.clear()
            },
            selectedItems = selectedItems,
            context = navController.context,
            onRemoveItem = {
                selectedItems.remove(it)
                isAllSelected = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreenTopBar(
    modifier: Modifier = Modifier,
    title: String = "",
    toggleGridView: () -> Unit,
    isGridView: Boolean,
    onSortClick: () -> Unit
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Title(text = title, modifier = Modifier)
        },
        actions = {
            IconButton(
                modifier = Modifier
                    .size(32.dp),
                onClick = {
                    toggleGridView()
                }
            ) {
                Icon(
                    modifier = Modifier
                        .size(32.dp),
                    painter =
                    if (isGridView)
                        painterResource(id = R.drawable.ic_view_list)
                    else
                        painterResource(id = R.drawable.ic_grid_view),
                    tint = MaterialTheme.colorScheme.primary,
                    contentDescription = "grid list view",
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            IconButton(
                modifier = Modifier
                    .size(32.dp),
                onClick = { onSortClick() }
            ) {
                Icon(
                    modifier = Modifier.size(32.dp),
                    painter = painterResource(id = R.drawable.ic_sort),
                    tint = MaterialTheme.colorScheme.primary,
                    contentDescription = "sort",
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortDialog(
    onDismissRequest: () -> Unit,
    currentSortBy: SortBy,
    currentDescending: Boolean,
    dateRangePickerState: DateRangePickerState,
    onApply: (SortBy, Boolean) -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = true,
            dismissOnBackPress = true,
            decorFitsSystemWindows = true
        ),
    ) {
        var descending by remember { mutableStateOf(currentDescending) }
        var selectedSort by remember { mutableStateOf(currentSortBy) }

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

                SortBy.entries.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedSort = item
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selectedSort == item,
                            onClick = {
                                selectedSort = item
                            },
                            enabled = true,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Text(text = item.label, modifier = Modifier.padding(start = 8.dp))
                    }
                }

                if (showDatePicker) DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showDatePicker = false
                            }
                        ) { Text("OK") }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showDatePicker = false
                            }
                        ) { Text("Cancel") }
                    }
                ) {
                    DateRangePicker(state = dateRangePickerState)
                }

                Row(
                    modifier = Modifier
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (selectedSort) {
                        SortBy.DATE -> {
                            OutlinedTextField(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .pointerInput(Unit) {
                                        awaitEachGesture {
                                            awaitFirstDown(pass = PointerEventPass.Initial)
                                            val upEvent =
                                                waitForUpOrCancellation(pass = PointerEventPass.Initial)
                                            if (upEvent != null) {
                                                showDatePicker = true
                                            }
                                        }
                                    },
                                readOnly = true,
                                value = dateRangePickerState.selectedStartDateMillis?.let {
                                    DateFormat.getDateInstance().format(Date(it))
                                } ?: "N/A",
                                onValueChange = {},
                                label = { Text("From Date") },
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            OutlinedTextField(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .pointerInput(Unit) {
                                        awaitEachGesture {
                                            awaitFirstDown(pass = PointerEventPass.Initial)
                                            val upEvent =
                                                waitForUpOrCancellation(pass = PointerEventPass.Initial)
                                            if (upEvent != null) {
                                                showDatePicker = true
                                            }
                                        }
                                    },
                                readOnly = true,
                                value = dateRangePickerState.selectedEndDateMillis?.let {
                                    DateFormat.getDateInstance().format(Date(it))
                                } ?: "N/A",
                                onValueChange = {},
                                label = { Text("To Date") },
                            )
                        }

                        SortBy.SIZE, SortBy.NAME -> {}
                    }
                }

                Row(
                    modifier = Modifier
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = descending,
                        onCheckedChange = { descending = it }
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
                        onApply(selectedSort, descending)
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
