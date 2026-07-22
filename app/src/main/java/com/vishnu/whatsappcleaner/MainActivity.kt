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

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vishnu.whatsappcleaner.data.Storage
import com.vishnu.whatsappcleaner.ui.AboutScreen
import com.vishnu.whatsappcleaner.ui.DetailsScreen
import com.vishnu.whatsappcleaner.ui.HomeScreen
import com.vishnu.whatsappcleaner.ui.PermissionScreen
import com.vishnu.whatsappcleaner.ui.theme.WhatsAppCleanerTheme

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        window.setBackgroundDrawableResource(android.R.color.transparent)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val resultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                val treeUri = result.data?.data
                if (result.resultCode == RESULT_OK && treeUri != null) {
                    if (hasMediaFolder(treeUri)) {
                        contentResolver.takePersistableUriPermission(
                            treeUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )

                        viewModel.saveHomeUri(treeUri.toString())
                        restartActivity()
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "Wrong directory selected, please select the WhatsApp directory...",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(this, "Please grant access...", Toast.LENGTH_SHORT).show()
                }
            }

        // API >= 30, non-Play: All Files Access is granted from a system Settings screen.
        val manageStorageLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (Storage.isReady(this)) {
                    restartActivity()
                } else {
                    Toast.makeText(
                        this,
                        "Please grant All Files Access to continue...",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

        // API <= 29: the direct File API needs read (listing) and write (deletion) storage access.
        val storagePermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
                if (results.values.all { it }) {
                    restartActivity()
                } else {
                    Toast.makeText(this, "Please grant storage access...", Toast.LENGTH_SHORT)
                        .show()
                }
            }

        // Picks the right onboarding flow for the active storage backend (see Storage.mode()).
        val requestAccess = {
            when (Storage.mode()) {
                Storage.Mode.SAF -> resultLauncher.launch(chooseDirectoryIntent())
                Storage.Mode.FILE_MANAGE -> requestAllFilesAccess(manageStorageLauncher)
                Storage.Mode.FILE_LEGACY ->
                    storagePermissionLauncher.launch(
                        arrayOf(
                            android.Manifest.permission.READ_EXTERNAL_STORAGE,
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                        )
                    )
            }
        }

        viewModel = ViewModelProvider(
            this,
            MainViewModelFactory(application)
        ).get(MainViewModel::class.java)

        setContent {
            WhatsAppCleanerTheme {
                val startDestination =
                    if (Storage.isReady(this))
                        Constants.SCREEN_HOME
                    else
                        Constants.SCREEN_PERMISSION

                val navController = rememberNavController()

                NavHost(
                    modifier = Modifier.background(MaterialTheme.colorScheme.background),
                    navController = navController,
                    startDestination = startDestination
                ) {
                    composable(
                        route = Constants.SCREEN_PERMISSION,
                        enterTransition = {
                            slideIntoContainer(
                                AnimatedContentTransitionScope.SlideDirection.Right,
                                animationSpec = tween(durationMillis = 700)
                            ) + fadeIn(animationSpec = tween(durationMillis = 700))
                        },
                        exitTransition = {
                            slideOutOfContainer(
                                AnimatedContentTransitionScope.SlideDirection.Left,
                                animationSpec = tween(durationMillis = 700)
                            ) + fadeOut(animationSpec = tween(durationMillis = 700))
                        },
                        popEnterTransition = {
                            slideIntoContainer(
                                AnimatedContentTransitionScope.SlideDirection.Left,
                                animationSpec = tween(durationMillis = 700)
                            ) + fadeIn(animationSpec = tween(durationMillis = 700))
                        },
                        popExitTransition = {
                            slideOutOfContainer(
                                AnimatedContentTransitionScope.SlideDirection.Right,
                                animationSpec = tween(durationMillis = 700)
                            ) + fadeOut(animationSpec = tween(durationMillis = 700))
                        }
                    ) {
                        PermissionScreen(
                            isAccessGranted = Storage.isReady(this@MainActivity),
                            showDirectoryHint = Storage.mode() == Storage.Mode.SAF,
                            requestAccess = requestAccess,
                        )
                    }

                    composable(
                        route = Constants.SCREEN_HOME,
                        enterTransition = {
                            slideIntoContainer(
                                AnimatedContentTransitionScope.SlideDirection.Left,
                                animationSpec = tween(durationMillis = 700)
                            ) + fadeIn(animationSpec = tween(durationMillis = 700))
                        },
                        exitTransition = {
                            slideOutOfContainer(
                                AnimatedContentTransitionScope.SlideDirection.Left,
                                animationSpec = tween(durationMillis = 700)
                            ) + fadeOut(animationSpec = tween(durationMillis = 700))
                        },
                        popEnterTransition = {
                            slideIntoContainer(
                                AnimatedContentTransitionScope.SlideDirection.Right,
                                animationSpec = tween(durationMillis = 700)
                            ) + fadeIn(animationSpec = tween(durationMillis = 700))
                        },
                        popExitTransition = {
                            slideOutOfContainer(
                                AnimatedContentTransitionScope.SlideDirection.Right,
                                animationSpec = tween(durationMillis = 700)
                            ) + fadeOut(animationSpec = tween(durationMillis = 700))
                        }
                    ) {
                        HomeScreen(navController, viewModel)
                    }

                    composable(
                        route = Constants.SCREEN_DETAILS,
                        enterTransition = {
                            slideIntoContainer(
                                AnimatedContentTransitionScope.SlideDirection.Left,
                                animationSpec = tween(durationMillis = 700)
                            ) + fadeIn(animationSpec = tween(durationMillis = 700))
                        },
                        exitTransition = {
                            slideOutOfContainer(
                                AnimatedContentTransitionScope.SlideDirection.Left,
                                animationSpec = tween(durationMillis = 700)
                            ) + fadeOut(animationSpec = tween(durationMillis = 700))
                        },
                        popEnterTransition = {
                            slideIntoContainer(
                                AnimatedContentTransitionScope.SlideDirection.Right,
                                animationSpec = tween(durationMillis = 700)
                            ) + fadeIn(animationSpec = tween(durationMillis = 700))
                        },
                        popExitTransition = {
                            slideOutOfContainer(
                                AnimatedContentTransitionScope.SlideDirection.Right,
                                animationSpec = tween(durationMillis = 700)
                            ) + fadeOut(animationSpec = tween(durationMillis = 700))
                        }
                    ) {
                        DetailsScreen(navController, viewModel)
                    }

                    composable(
                        route = Constants.SCREEN_ABOUT,
                        enterTransition = {
                            slideIntoContainer(
                                AnimatedContentTransitionScope.SlideDirection.Left,
                                animationSpec = tween(durationMillis = 700)
                            ) + fadeIn(animationSpec = tween(durationMillis = 700))
                        },
                        exitTransition = {
                            slideOutOfContainer(
                                AnimatedContentTransitionScope.SlideDirection.Left,
                                animationSpec = tween(durationMillis = 700)
                            ) + fadeOut(animationSpec = tween(durationMillis = 700))
                        },
                        popEnterTransition = {
                            slideIntoContainer(
                                AnimatedContentTransitionScope.SlideDirection.Right,
                                animationSpec = tween(durationMillis = 700)
                            ) + fadeIn(animationSpec = tween(durationMillis = 700))
                        },
                        popExitTransition = {
                            slideOutOfContainer(
                                AnimatedContentTransitionScope.SlideDirection.Right,
                                animationSpec = tween(durationMillis = 700)
                            ) + fadeOut(animationSpec = tween(durationMillis = 700))
                        }
                    ) {
                        AboutScreen(navController)
                    }
                }
            }
        }
    }

    /**
     * Verifies the user picked an actual WhatsApp directory by checking for a "Media" child.
     * Works for both the legacy "/WhatsApp" tree and the scoped
     * "/Android/media/com.whatsapp/WhatsApp" tree, since neither requires raw file access.
     */
    private fun hasMediaFolder(treeUri: Uri): Boolean = try {
        val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
        val mediaUri =
            DocumentsContract.buildDocumentUriUsingTree(treeUri, "$rootDocId/Media")
        contentResolver.query(
            mediaUri,
            arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID),
            null,
            null,
            null
        )?.use { it.moveToFirst() } ?: false
    } catch (e: Exception) {
        false
    }

    /** SAF tree picker intent, pre-pointed at the scoped WhatsApp directory. */
    private fun chooseDirectoryIntent(): Intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
        addFlags(
            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        )
        if (Build.VERSION.SDK_INT >= VERSION_CODES.O)
            putExtra(
                DocumentsContract.EXTRA_INITIAL_URI,
                DocumentsContract.buildDocumentUri(
                    EXTERNAL_STORAGE_AUTHORITY,
                    "primary:Android/media/com.whatsapp/WhatsApp"
                )
            )
    }

    /**
     * Opens the All Files Access settings screen for this app, falling back to the global list if
     * the per-app screen is unavailable on the device.
     */
    private fun requestAllFilesAccess(
        launcher: androidx.activity.result.ActivityResultLauncher<Intent>
    ) {
        try {
            launcher.launch(
                Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    "package:$packageName".toUri()
                )
            )
        } catch (e: Exception) {
            launcher.launch(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
        }
    }

    private fun restartActivity() {
        // terrible hack!
        val intent = intent
        finish()
        startActivity(intent)
    }

    companion object {
        private const val EXTERNAL_STORAGE_AUTHORITY =
            "com.android.externalstorage.documents"
    }
}
