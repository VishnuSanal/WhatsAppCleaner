/*
 * Copyright (C) 2025 Vishnu Sanal T
 *
 * This file is part of WhatsAppCleaner.
 *
 * WhatsAppCleaner is free software: you can redistribute it and/or modify
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

package com.vishnu.whatsappcleaner.ui.components

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vishnu.whatsappcleaner.Constants
import com.vishnu.whatsappcleaner.data.FileRepository
import com.vishnu.whatsappcleaner.model.ListFile

/**
 * Generic confirmation dialog: a title and message on the left, the confirm button on the right,
 * and an optional [content] block underneath.
 */
@Composable
fun ConfirmationDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
    title: String,
    message: String,
    confirmText: String = "Confirm",
    content: @Composable () -> Unit = {}
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
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                        )

                        Text(
                            modifier = Modifier
                                .wrapContentHeight()
                                .padding(vertical = 2.dp)
                                .align(Alignment.Start),
                            text = message,
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
                                text = confirmText,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                        },
                    )
                }

                content()
            }
        }
    }
}

/**
 * Confirmation for a batch delete: lists everything that is about to go, and lets the user drop
 * individual files from the batch. Dismisses itself once nothing is left to delete.
 */
@Composable
fun CleanupConfirmationDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
    selectedItems: List<ListFile>,
    context: Context,
    onRemoveItem: (ListFile) -> Unit
) {
    val selectedItemsSnapshot = selectedItems.toList()

    LaunchedEffect(selectedItemsSnapshot.isEmpty()) {
        if (selectedItemsSnapshot.isEmpty()) onDismissRequest()
    }

    if (selectedItemsSnapshot.isEmpty()) return

    val totalSize = remember(context, selectedItemsSnapshot) {
        FileRepository.formatSize(context, selectedItemsSnapshot.sumOf { it.sizeBytes })
    }

    ConfirmationDialog(
        onDismissRequest = onDismissRequest,
        onConfirmation = onConfirmation,
        title = "Confirm Cleanup",
        message = "${selectedItemsSnapshot.size} files ($totalSize) will be deleted. " +
            "Tap the delete icon to remove an item.",
    ) {
        LazyVerticalGrid(
            modifier = Modifier
                .wrapContentHeight(),
            columns = GridCells.Fixed(Constants.DETAILS_GRID_COLUMN_COUNT),
        ) {
            items(
                items = selectedItemsSnapshot,
                key = { it.uri }
            ) { listFile ->
                ConfirmationCard(listFile) {
                    onRemoveItem(listFile)
                }
            }
        }
    }
}
