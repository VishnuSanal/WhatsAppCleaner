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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.vishnu.whatsappcleaner.R

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun PermissionScreen(
    navController: NavHostController,
    permissionsGranted: Pair<Boolean, Boolean>,
    requestPermission: () -> Unit,
    chooseDirectory: () -> Unit
) {
    Scaffold(
        Modifier.background(MaterialTheme.colorScheme.background)
    ) { contentPadding ->
        Column(Modifier.padding(contentPadding)) {
            Text(
                modifier = Modifier.padding(16.dp, 32.dp, 16.dp, 0.dp),
                text = "Welcome!",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Start,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Text(
                modifier = Modifier.padding(16.dp),
                text = "Thanks for installing WhatsAppCleaner. Please follow this quick setup to get started.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Start,
                color = MaterialTheme.colorScheme.onBackground,
            )

            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                text = "1. Please grant the required permissions to WhatsAppCleaner",
                textAlign = TextAlign.Justify,
                style = MaterialTheme.typography.bodyMedium,
            )

            OutlinedButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                enabled = !permissionsGranted.first,
                content = {
                    Text(
                        if (!permissionsGranted.first)
                            "Grant storage permissions"
                        else
                            "Storage permission granted"
                    )
                },
                onClick = {
                    requestPermission()
                }
            )

            if (!permissionsGranted.first)
                Spacer(Modifier.weight(1f))

            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                text = "2. Please grant access to the WhatsApp directory as shown in the picture below",
                textAlign = TextAlign.Justify,
                style = MaterialTheme.typography.bodyMedium,
            )

            OutlinedButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                enabled = permissionsGranted.first && !permissionsGranted.second,
                content = {
                    Text(
                        if (!permissionsGranted.first)
                            "Grant storage permissions"
                        else
                            "Choose WhatsApp directory"
                    )
                },
                onClick = {
                    chooseDirectory()
                },
            )

            if (permissionsGranted.first && !permissionsGranted.second)
                GlideImage(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.CenterHorizontally)
                        .fillMaxWidth()
                        .fillMaxHeight(),
                    model = R.drawable.permission_hint,
                    contentScale = ContentScale.Inside,
                    loading = placeholder(R.drawable.image),
                    failure = placeholder(R.drawable.error),
                    contentDescription = "permission hint"
                )
        }
    }
}
