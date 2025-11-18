package com.example.aplikasidavin.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore("user_prefs")


class UserPreferences(private val context: Context) {
    companion object {
        val USER_ID = intPreferencesKey("user_id")
    }

    suspend fun saveUserId(id: Int) {
        context.dataStore.edit { it[USER_ID] = id }
    }

    fun getUserId(): Flow<Int?> {
        return context.dataStore.data.map { it[USER_ID] }
    }

    suspend fun getUserIdOnce(): Int? {
        return context.dataStore.data.first()[USER_ID]
    }

    suspend fun clearUser() {
        context.dataStore.edit { it.clear() }
    }
}

