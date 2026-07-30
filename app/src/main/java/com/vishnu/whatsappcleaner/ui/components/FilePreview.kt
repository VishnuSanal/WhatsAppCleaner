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

import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.vishnu.whatsappcleaner.Constants
import com.vishnu.whatsappcleaner.R
import com.vishnu.whatsappcleaner.model.ListFile

/**
 * Broad kind of a file, derived from its extension, that decides how it gets previewed and which
 * fallback icon stands in when there is no bitmap to show.
 */
enum class FileKind(@DrawableRes val iconRes: Int, val label: String) {
    IMAGE(R.drawable.image, "image"),
    VIDEO(R.drawable.video, "video"),
    DOCUMENT(R.drawable.document, "document"),
    AUDIO(R.drawable.audio, "audio"),
    UNKNOWN(R.drawable.unknown, "unknown file"),
}

val ListFile.kind: FileKind
    get() = when (extension.lowercase()) {
        in Constants.EXTENSIONS_IMAGE -> FileKind.IMAGE
        in Constants.EXTENSIONS_VIDEO -> FileKind.VIDEO
        in Constants.EXTENSIONS_DOCS -> FileKind.DOCUMENT
        in Constants.EXTENSIONS_AUDIO -> FileKind.AUDIO
        else -> FileKind.UNKNOWN
    }

/**
 * Pixel size to decode a thumbnail at when it fills one column of a three-column grid. The
 * confirmation grid is inset by the dialog's padding, so this is an upper bound there.
 */
@Composable
fun gridThumbnailSizePx(): Int {
    val screenWidthPx = with(LocalDensity.current) {
        LocalConfiguration.current.screenWidthDp.dp.roundToPx()
    }
    return screenWidthPx / Constants.DETAILS_GRID_COLUMN_COUNT
}

/**
 * Square preview filling the parent Box: a cropped thumbnail for media, an icon above the file
 * name for everything else. [thumbnailSizePx] caps the decoded bitmap; pass null to let Glide
 * decode at its own size.
 */
@Composable
fun BoxScope.FilePreview(
    listFile: ListFile,
    contentDescription: String,
    thumbnailSizePx: Int? = null
) {
    when (val kind = listFile.kind) {
        FileKind.IMAGE -> Thumbnail(listFile, contentDescription, thumbnailSizePx)

        FileKind.VIDEO -> {
            Thumbnail(listFile, contentDescription, thumbnailSizePx)
            PlayBadge(Modifier.align(Alignment.Center))
        }

        else -> NamedFileIcon(kind, listFile.name)
    }
}

/**
 * Compact square thumbnail for the details list rows: a cropped bitmap for media, a plain tinted
 * icon for everything else. Must be laid out inside a Box of [size].
 */
@Composable
fun BoxScope.FileThumbnail(listFile: ListFile, size: Dp, thumbnailSizePx: Int) {
    when (val kind = listFile.kind) {
        FileKind.IMAGE -> Thumbnail(
            listFile = listFile,
            contentDescription = "image preview",
            thumbnailSizePx = thumbnailSizePx,
            modifier = Modifier.size(size)
        )

        FileKind.VIDEO -> {
            PlayBadge(Modifier.align(Alignment.Center))
            Thumbnail(listFile, "details list item", thumbnailSizePx)
        }

        else -> Icon(
            modifier = Modifier.size(size),
            painter = painterResource(id = kind.iconRes),
            contentDescription = kind.label,
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun Thumbnail(
    listFile: ListFile,
    contentDescription: String,
    thumbnailSizePx: Int?,
    modifier: Modifier = Modifier
) {
    GlideImage(
        modifier = modifier,
        model = listFile.uri,
        contentScale = ContentScale.Crop,
        loading = placeholder(R.drawable.image),
        failure = placeholder(R.drawable.error),
        contentDescription = contentDescription,
        requestBuilderTransform = {
            if (thumbnailSizePx == null) it else it.asThumbnail(thumbnailSizePx)
        }
    )
}

/** Translucent play icon overlaid on a video thumbnail. */
@Composable
private fun PlayBadge(modifier: Modifier = Modifier) {
    Icon(
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.6f))
            .padding(8.dp)
            .aspectRatio(1f)
            .zIndex(2f),
        painter = painterResource(id = R.drawable.video),
        contentDescription = FileKind.VIDEO.label,
    )
}

/** Fallback for files that have no bitmap preview: a large kind icon above the file name. */
@Composable
private fun NamedFileIcon(kind: FileKind, name: String) {
    Column {
        Icon(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.6f))
                .padding(8.dp),
            painter = painterResource(id = kind.iconRes),
            contentDescription = kind.label,
        )

        Text(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(8.dp),
            text = name,
            textAlign = TextAlign.Center,
            maxLines = 2,
            minLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun RequestBuilder<Drawable>.asThumbnail(sizePx: Int): RequestBuilder<Drawable> = override(sizePx)
    .downsample(DownsampleStrategy.CENTER_OUTSIDE)
    .format(DecodeFormat.PREFER_RGB_565)
