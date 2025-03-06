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

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.FileProvider
import androidx.navigation.NavHostController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.valentinilk.shimmer.shimmer
import com.vishnu.whatsappcleaner.Constants
import com.vishnu.whatsappcleaner.R
import com.vishnu.whatsappcleaner.ViewState
import com.vishnu.whatsappcleaner.model.ListDirectory
import com.vishnu.whatsappcleaner.model.ListFile

@Composable
fun Title(modifier: Modifier, text: String) {
    Text(
        modifier = modifier.padding(8.dp),
        text = text,
        fontSize = 24.sp,
        textAlign = TextAlign.Start,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
fun Banner(modifier: Modifier, directoryItem: ViewState<Pair<String, List<ListDirectory>>>) {
    val bgColor = MaterialTheme.colorScheme.primaryContainer
    val textColor = MaterialTheme.colorScheme.onPrimaryContainer

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier
                .padding(12.dp)
                .fillMaxWidth(0.4f)
                .aspectRatio(1f)
                .shadow(elevation = 16.dp, shape = CircleShape)
                .background(bgColor, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = buildAnnotatedString {
                    when (directoryItem) {
                        is ViewState.Success -> {
                            var size = directoryItem.data.first

                            if (size.contains(" ")) {
                                val split = size.split(" ")
                                withStyle(SpanStyle(fontSize = 32.sp)) {
                                    append(split.get(0))
                                }
                                withStyle(SpanStyle(fontSize = 18.sp)) {
                                    append(" ${split.get(1)}")
                                }
                            } else {
                                withStyle(SpanStyle(fontSize = 28.sp)) {
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
                },
                style = MaterialTheme.typography.titleLarge,
                color = textColor,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun Banner(modifier: Modifier, text: AnnotatedString) {
    val bgColor = MaterialTheme.colorScheme.primaryContainer
    val textColor = MaterialTheme.colorScheme.onPrimaryContainer

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .padding(12.dp)
                .fillMaxWidth(0.4f)
                .aspectRatio(1f)
                .shadow(elevation = 16.dp, shape = CircleShape)
                .background(bgColor, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleLarge,
                color = textColor,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun SingleCard(
    listDirectory: ListDirectory,
    navController: NavHostController,
) {
    val bgColor = MaterialTheme.colorScheme.secondaryContainer
    val textColor = MaterialTheme.colorScheme.onSecondaryContainer

    var onClick: () -> Unit
    var modifier: Modifier

    if (listDirectory.path.contains(Constants.LIST_LOADING_INDICATION)) {
        modifier = Modifier.shimmer()
        onClick = { }
    } else {
        modifier = Modifier
        onClick = {
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

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun ItemCard(
    listFile: ListFile,
    navController: NavHostController,
    isSelected: Boolean = false,
    selectionEnabled: Boolean = true,
    toggleSelection: () -> Unit,
) {
    key(listFile) {
        // only for keeping track of the UI
        var selected by remember { mutableStateOf(isSelected) }

        var modifier = if (listFile.filePath.toString()
                .contains(Constants.LIST_LOADING_INDICATION)
        ) Modifier.shimmer()
        else Modifier

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
                            if (!selectionEnabled) return@detectTapGestures

                            selected = !selected

                            if (!listFile.filePath.toString()
                                    .contains(Constants.LIST_LOADING_INDICATION)
                            ) toggleSelection()
                        }, onTap = {
                            if (selectionEnabled &&
                                !listFile.filePath.toString()
                                    .contains(Constants.LIST_LOADING_INDICATION)
                            ) openFile(
                                navController.context,
                                listFile
                            )
                        })
                    }
            ) {
                if (selectionEnabled) Box(
                    Modifier
                        .padding(8.dp)
                        .size(24.dp)
                        .align(Alignment.TopStart)
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
                        .clickable {
                            selected = !selected

                            if (!listFile.filePath.toString()
                                    .contains(Constants.LIST_LOADING_INDICATION)
                            ) toggleSelection()
                        }
                ) {
                    if (selected) Icon(
                        modifier = Modifier.clip(CircleShape),
                        painter = painterResource(id = R.drawable.check_circle_filled),
                        tint = MaterialTheme.colorScheme.primaryContainer,
                        contentDescription = "checkbox",
                    )
                }

                if (listFile.extension.lowercase() in Constants.EXTENSIONS_IMAGE) GlideImage(
                    model = listFile,
                    contentScale = ContentScale.Crop,
                    loading = placeholder(R.drawable.image),
                    failure = placeholder(R.drawable.error),
                    contentDescription = "details list item"
                )
                else if (listFile.extension.lowercase() in Constants.EXTENSIONS_VIDEO) {
                    GlideImage(
                        model = listFile,
                        contentScale = ContentScale.Crop,
                        loading = placeholder(R.drawable.image),
                        failure = placeholder(R.drawable.error),
                        contentDescription = "details list item"
                    )

                    Icon(
                        modifier = Modifier
                            .size(32.dp)
                            .align(Alignment.Center)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.6f))
                            .padding(8.dp)
                            .aspectRatio(1f)
                            .zIndex(2f),
                        painter = painterResource(id = R.drawable.video),
                        contentDescription = "video",
                    )
                } else if (listFile.extension.lowercase() in Constants.EXTENSIONS_DOCS) {
                    Column {
                        Icon(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.6f))
                                .padding(8.dp),
                            painter = painterResource(id = R.drawable.document),
                            contentDescription = "doc",
                        )

                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .padding(8.dp),
                            text = listFile.name,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            minLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else if (listFile.extension.lowercase() in Constants.EXTENSIONS_AUDIO) {
                    Column {
                        Icon(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.6f))
                                .padding(8.dp),
                            painter = painterResource(id = R.drawable.audio),
                            contentDescription = "doc",
                        )

                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .padding(8.dp),
                            text = listFile.name,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            minLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Column {
                        Icon(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.6f))
                                .padding(8.dp),
                            painter = painterResource(id = R.drawable.unknown),
                            contentDescription = "doc",
                        )

                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .padding(8.dp),
                            text = listFile.name,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            minLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

fun openFile(context: Context, listFile: ListFile) {
    try {
        startActivity(
            context,
            Intent(
                Intent.ACTION_VIEW,
                FileProvider.getUriForFile(
                    context,
                    context.packageName + ".provider",
                    listFile
                )
            ).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
            null
        )
    } catch (e: ActivityNotFoundException) {
        e.printStackTrace()
        Toast.makeText(
            context,
            "No application found to open this file.",
            Toast.LENGTH_SHORT
        ).show()
    } catch (e: IllegalArgumentException) {
        e.printStackTrace()
        Toast.makeText(
            context,
            "Something went wrong...",
            Toast.LENGTH_SHORT
        ).show()
    }
}
