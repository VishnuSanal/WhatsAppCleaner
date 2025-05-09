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

package com.vishnu.whatsappcleaner.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class StoreData(val context: Context) {

    companion object {
        private val Context.dataStore by preferencesDataStore(name = "store_data")
        val IS_GRID_VIEW_KEY = booleanPreferencesKey("is_grid_view")
    }

    suspend fun set(key: String, value: String) {
        context.dataStore.edit { preferences ->
            preferences[stringPreferencesKey(key)] = value
        }
    }

    suspend fun get(key: String): String? = context.dataStore.data.first().get(
        stringPreferencesKey(key)
    )

    suspend fun setGridViewPreference(isGridView: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_GRID_VIEW_KEY] = isGridView
        }
    }

    val isGridViewFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[IS_GRID_VIEW_KEY] ?: false
        }
}
