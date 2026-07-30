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

package com.vishnu.whatsappcleaner.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavHostController
import com.valentinilk.shimmer.shimmer
import com.vishnu.whatsappcleaner.Constants
import com.vishnu.whatsappcleaner.R
import com.vishnu.whatsappcleaner.model.ListDirectory
import com.vishnu.whatsappcleaner.model.ListFile
import com.vishnu.whatsappcleaner.util.openFile
import java.text.DateFormat

/** Home screen row for a WhatsApp media directory, navigating to its details on click. */
@Composable
fun SingleCard(
    listDirectory: ListDirectory,
    navController: NavHostController,
) {
    val bgColor = MaterialTheme.colorScheme.secondaryContainer
    val textColor = MaterialTheme.colorScheme.onSecondaryContainer

    val isPlaceholder = listDirectory.path.contains(Constants.LIST_LOADING_INDICATION)
    val modifier = if (isPlaceholder) Modifier.shimmer() else Modifier
    val onClick = {
        if (!isPlaceholder) {
            navController.currentBackStackEntry?.savedStateHandle?.apply {
                set(Constants.DETAILS_LIST_ITEM, listDirectory)
            }
            navController.navigate(Constants.SCREEN_DETAILS)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .padding(16.dp)
                    .fillMaxWidth(0.2f)
                    .aspectRatio(1f)
                    .shadow(elevation = 8.dp, shape = CircleShape)
                    .background(bgColor, shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier.padding(8.dp),
                    imageVector = ImageVector.vectorResource(id = listDirectory.icon),
                    contentDescription = "icon",
                    tint = textColor
                )
            }

            Column(
                Modifier
                    .align(Alignment.CenterVertically)
                    .fillMaxWidth(0.75f),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Start)
                        .padding(2.dp),
                    text = listDirectory.name,
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.titleLarge,
                    color = textColor,
                )

                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Start)
                        .padding(2.dp),
                    text = listDirectory.size,
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.titleSmall,
                    color = textColor,
                )
            }
        }
    }
}

/** Square tile in the details grid: tap opens the file, long press toggles the selection. */
@Composable
fun ItemGridCard(
    listFile: ListFile,
    navController: NavHostController,
    isSelected: Boolean = false,
    toggleSelection: () -> Unit,
) {
    key(listFile.uri) {
        // only for keeping track of the UI
        var selected by remember { mutableStateOf(isSelected) }

        val thumbnailSizePx = gridThumbnailSizePx()

        val isPlaceholder = listFile.uri.contains(Constants.LIST_LOADING_INDICATION)
        val modifier = if (isPlaceholder) Modifier.shimmer() else Modifier

        LaunchedEffect(isSelected) {
            selected = isSelected
        }

        Card(
            modifier = modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(
                    if (selected) 16.dp else 8.dp
                ),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(shape = RoundedCornerShape(8.dp))
                    .pointerInput(Unit) {
                        detectTapGestures(onLongPress = {
                            selected = !selected

                            if (!isPlaceholder) toggleSelection()
                        }, onTap = {
                            if (!isPlaceholder) openFile(navController.context, listFile)
                        })
                    }
            ) {
                SelectionIndicator(
                    selected = selected,
                    modifier = Modifier.align(Alignment.TopStart),
                    onClick = {
                        selected = !selected

                        if (!isPlaceholder) toggleSelection()
                    }
                )

                FilePreview(
                    listFile = listFile,
                    contentDescription = "details list item",
                    thumbnailSizePx = thumbnailSizePx
                )
            }
        }
    }
}

/** Row in the details list: tap opens the file, tapping the thumbnail toggles the selection. */
@Composable
fun ItemListCard(
    listFile: ListFile,
    navController: NavHostController,
    isSelected: Boolean = false,
    toggleSelection: () -> Unit,
) {
    var selected by remember { mutableStateOf(isSelected) }

    val thumbnailSizePx = with(LocalDensity.current) { Constants.DETAILS_LIST_THUMBNAIL_SIZE_DP.dp.roundToPx() }

    val isPlaceholder = listFile.uri.contains(Constants.LIST_LOADING_INDICATION)
    val modifier = if (isPlaceholder) Modifier.shimmer() else Modifier

    LaunchedEffect(isSelected) {
        selected = isSelected
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .pointerInput(Unit) {
                    detectTapGestures(onLongPress = {
                        selected = !selected

                        if (!isPlaceholder) toggleSelection()
                    }, onTap = {
                        if (!isPlaceholder) openFile(navController.context, listFile)
                    })
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(Constants.DETAILS_LIST_THUMBNAIL_SIZE_DP.dp)
                    .clip(CircleShape)
                    .clickable {
                        selected = !selected

                        if (!isPlaceholder) toggleSelection()
                    }
            ) {
                FileThumbnail(listFile, Constants.DETAILS_LIST_THUMBNAIL_SIZE_DP.dp, thumbnailSizePx)

                if (selected) {
                    CheckedIcon(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(24.dp)
                            .zIndex(3f),
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    modifier = Modifier
                        .padding(vertical = 4.dp, horizontal = 8.dp)
                        .fillMaxWidth()
                        .basicMarquee(),
                    text = listFile.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )

                Row(Modifier.padding(vertical = 4.dp, horizontal = 8.dp)) {
                    MetadataText(listFile.extension.uppercase())

                    MetadataDivider()

                    MetadataText(listFile.size)

                    MetadataDivider()

                    MetadataText(DateFormat.getDateInstance().format(listFile.dateModified))
                }
            }
        }
    }
}

/** Read-only tile in the cleanup confirmation grid, with a button to drop the file from it. */
@Composable
fun ConfirmationCard(
    listFile: ListFile,
    onRemove: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(8.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(shape = RoundedCornerShape(8.dp))
        ) {
            FilePreview(
                listFile = listFile,
                contentDescription = "confirmation list item",
                thumbnailSizePx = gridThumbnailSizePx()
            )

            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .size(22.dp)
                    .align(Alignment.TopEnd)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onPrimary)
                    .clickable(onClick = onRemove)
                    .zIndex(4f),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier.size(18.dp),
                    painter = painterResource(id = R.drawable.ic_delete),
                    tint = MaterialTheme.colorScheme.primary,
                    contentDescription = "remove item"
                )
            }
        }
    }
}

/** Circular tick shown over a selected file. */
@Composable
private fun CheckedIcon(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.onPrimary, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier.matchParentSize(),
            painter = painterResource(id = R.drawable.check_circle_filled),
            tint = MaterialTheme.colorScheme.primary,
            contentDescription = "checkbox"
        )
    }
}

/** Tappable selection circle drawn over a grid tile — a hollow ring until the tile is selected. */
@Composable
private fun SelectionIndicator(
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier
            .padding(8.dp)
            .size(24.dp)
            .clip(CircleShape)
            .border(
                BorderStroke(
                    2.dp,
                    if (selected) Color.Unspecified else Color.White,
                ),
                CircleShape
            )
            .aspectRatio(1f)
            .zIndex(4f)
            .clickable(onClick = onClick)
    ) {
        if (selected) {
            CheckedIcon(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(24.dp),
            )
        }
    }
}

@Composable
private fun MetadataText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun MetadataDivider() {
    VerticalDivider(
        modifier = Modifier.padding(2.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
