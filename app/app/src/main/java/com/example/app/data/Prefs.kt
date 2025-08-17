package com.example.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "stay_home_prefs")

object PrefsKeys {
    val USER_ID = intPreferencesKey("user_id")
}

suspend fun saveUserId(context: Context, userId: Int) {
    context.dataStore.edit { prefs ->
        prefs[PrefsKeys.USER_ID] = userId
    }
}

fun userIdFlow(context: Context): Flow<Int?> =
    context.dataStore.data.map { prefs -> prefs[PrefsKeys.USER_ID] }

suspend fun clearUserId(context: Context) {
    context.dataStore.edit { prefs ->
        prefs.remove(PrefsKeys.USER_ID)
    }
}
