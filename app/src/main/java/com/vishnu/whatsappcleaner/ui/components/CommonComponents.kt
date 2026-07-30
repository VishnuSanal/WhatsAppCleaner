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

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

/** Circular headline showing a single piece of text — used for the total size on every screen. */
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

/**
 * Formats a size string such as "1.2 GB" for a [Banner], rendering the unit smaller than the value.
 * Sizes without a unit (or in any other shape) are rendered whole at [valueFontSize].
 */
fun sizeBannerText(
    size: String,
    valueFontSize: TextUnit = 24.sp,
    unitFontSize: TextUnit = 18.sp
): AnnotatedString = buildAnnotatedString {
    val parts = size.split(" ")
    if (parts.size == 2) {
        withStyle(SpanStyle(fontSize = valueFontSize)) { append(parts[0]) }
        withStyle(SpanStyle(fontSize = unitFontSize)) { append(" ${parts[1]}") }
    } else {
        withStyle(SpanStyle(fontSize = valueFontSize)) { append(size) }
    }
}

/** Single-line message shown in a [Banner] while sizes are still being computed, or on failure. */
fun bannerMessage(message: String): AnnotatedString = buildAnnotatedString {
    withStyle(SpanStyle(fontSize = 18.sp)) { append(message) }
}

@Composable
fun CleanUpButton(
    modifier: Modifier = Modifier,
    selectedItems: List<ListFile>,
    onShowDialog: () -> Unit
) {
    val isEnabled = selectedItems.isNotEmpty()

    val containerColor by animateColorAsState(
        targetValue = if (isEnabled)
            MaterialTheme.colorScheme.primary
        else
            MaterialTheme.colorScheme.surfaceVariant,
        label = "ContainerColorAnimation"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isEnabled)
            MaterialTheme.colorScheme.onPrimary
        else
            MaterialTheme.colorScheme.onSurfaceVariant,
        label = "ContentColorAnimation"
    )

    TextButton(
        modifier = modifier.padding(2.dp),
        colors = ButtonDefaults.textButtonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor,
            disabledContentColor = contentColor
        ),
        shape = RoundedCornerShape(64.dp),
        contentPadding = PaddingValues(8.dp),
        enabled = isEnabled,
        onClick = {
            if (selectedItems.isNotEmpty()) onShowDialog()
        }
    ) {
        Text(
            text = "Cleanup",
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.titleLarge,
            fontSize = 18.sp,
            letterSpacing = 1.sp
        )
    }
}
