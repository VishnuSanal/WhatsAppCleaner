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

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.Environment
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vishnu.whatsappcleaner.ui.DetailsScreen
import com.vishnu.whatsappcleaner.ui.HomeScreen
import com.vishnu.whatsappcleaner.ui.PermissionScreen
import com.vishnu.whatsappcleaner.ui.theme.WhatsAppCleanerTheme
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: MainViewModel

    private lateinit var storagePermissionGranted: MutableState<Boolean>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val resultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK && result.data?.data?.path != null) {
                    val relativePath = result.data!!.data!!.path!!.split(":")[1]
                    val absolutePath =
                        Environment.getExternalStorageDirectory().absolutePath + File.separator + relativePath

                    viewModel.listDirectories(absolutePath)

                    lifecycleScope.launch {
                        repeatOnLifecycle(Lifecycle.State.STARTED) {
                            viewModel.directories.collect { dirList ->
                                if (dirList.toString().contains("/Media")) {
                                    contentResolver.takePersistableUriPermission(
                                        result.data!!.data!!,
                                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                    )

                                    viewModel.saveHomeUri(absolutePath)
                                    restartActivity()
                                } else {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Wrong directory selected, please select the right directory...",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    }
                } else {
                    Toast.makeText(this, "Please grant permissions...", Toast.LENGTH_SHORT).show()
                }
            }

        val storagePermissionResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (Build.VERSION.SDK_INT >= VERSION_CODES.R && !Environment.isExternalStorageManager()) {
                    Toast.makeText(
                        this,
                        "Please grant permissions...",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

        viewModel = ViewModelProvider(
            this,
            MainViewModelFactory(application)
        ).get(MainViewModel::class.java)

        setContent {
            WhatsAppCleanerTheme {
                storagePermissionGranted =
                    remember {
                        mutableStateOf(
                            (Build.VERSION.SDK_INT >= VERSION_CODES.R && Environment.isExternalStorageManager()) ||
                                ActivityCompat.checkSelfPermission(
                                    this@MainActivity,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                                ) == PackageManager.PERMISSION_GRANTED
                        )
                    }

                var startDestination =
                    if (Build.VERSION.SDK_INT >= VERSION_CODES.R &&
                        Environment.isExternalStorageManager() &&
                        contentResolver.persistedUriPermissions.isNotEmpty()
                    ) Constants.SCREEN_HOME
                    else if (ActivityCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ) == PackageManager.PERMISSION_GRANTED &&
                        contentResolver.persistedUriPermissions.isNotEmpty()
                    ) Constants.SCREEN_HOME
                    else {
                        Toast.makeText(
                            this,
                            "Please grant all permissions...",
                            Toast.LENGTH_SHORT
                        ).show()
                        Constants.SCREEN_PERMISSION
                    }

                val navController = rememberNavController()

                NavHost(
                    modifier = Modifier.background(MaterialTheme.colorScheme.background),
                    navController = navController,
                    startDestination = startDestination
                ) {
                    composable(route = Constants.SCREEN_PERMISSION) {
                        PermissionScreen(
                            navController = navController,
                            permissionsGranted = Pair(
                                storagePermissionGranted.value,
                                contentResolver.persistedUriPermissions.isNotEmpty()
                            ),
                            requestPermission = {
                                if (Build.VERSION.SDK_INT >= VERSION_CODES.R) {
                                    storagePermissionResultLauncher.launch(
                                        Intent(
                                            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                            Uri.parse("package:" + packageName)
                                        )
                                    )
                                } else {
                                    if (ActivityCompat.shouldShowRequestPermissionRationale(
                                            this@MainActivity,
                                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                                        )
                                    ) {
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Storage permission required for the app to work",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }

                                    requestPermissions(
                                        arrayOf(
                                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                            Manifest.permission.READ_EXTERNAL_STORAGE
                                        ),
                                        Constants.REQUEST_PERMISSIONS_CODE_WRITE_STORAGE
                                    )
                                }
                            },
                            chooseDirectory = {
                                resultLauncher.launch(
                                    Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                                        if (Build.VERSION.SDK_INT >= VERSION_CODES.O) putExtra(
                                            DocumentsContract.EXTRA_INITIAL_URI,
                                            Uri.parse(Constants.WHATSAPP_HOME_URI)
                                        )
                                    }
                                )
                            },
                        )
                    }

                    composable(
                        route = Constants.SCREEN_HOME,
                        exitTransition = {
                            slideOutOfContainer(
                                AnimatedContentTransitionScope.SlideDirection.Start,
                                animationSpec = tween(durationMillis = 700)
                            ) + fadeOut(animationSpec = tween(durationMillis = 700))
                        },
                        popEnterTransition = {
                            slideIntoContainer(
                                AnimatedContentTransitionScope.SlideDirection.End,
                                animationSpec = tween(durationMillis = 700)
                            ) + fadeIn(animationSpec = tween(durationMillis = 700))
                        }
                    ) {
                        HomeScreen(navController, viewModel)
                    }

                    composable(
                        route = Constants.SCREEN_DETAILS,
                        enterTransition = {
                            slideIntoContainer(
                                AnimatedContentTransitionScope.SlideDirection.Start,
                                animationSpec = tween(durationMillis = 700)
                            ) + fadeIn(animationSpec = tween(durationMillis = 700))
                        },
                        popExitTransition = {
                            slideOutOfContainer(
                                AnimatedContentTransitionScope.SlideDirection.End,
                                animationSpec = tween(durationMillis = 700)
                            ) + fadeOut(animationSpec = tween(durationMillis = 700))
                        }
                    ) {
                        DetailsScreen(navController, viewModel)
                    }
                }
            }
        }
    }

    @Deprecated("Deprecated callback")
    public override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == Constants.REQUEST_PERMISSIONS_CODE_WRITE_STORAGE) {
            for (i in permissions.indices) {
                val permission = permissions[i]
                val grantResult = grantResults[i]

                if (permission == Manifest.permission.WRITE_EXTERNAL_STORAGE) {
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        storagePermissionGranted.value = true
                    } else {
                        requestPermissions(
                            arrayOf<String>(
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.READ_EXTERNAL_STORAGE
                            ),
                            Constants.REQUEST_PERMISSIONS_CODE_WRITE_STORAGE
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= VERSION_CODES.R && ::storagePermissionGranted.isInitialized)
            storagePermissionGranted.value = Environment.isExternalStorageManager()
    }

    private fun restartActivity() {
        // terrible hack!
        val intent = intent
        finish()
        startActivity(intent)
    }
}
