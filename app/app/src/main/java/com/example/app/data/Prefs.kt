package com.example.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "stay_home_prefs")

object PrefsKeys {
    val USER_ID = intPreferencesKey("user_id")
    val HOME_SSID = stringPreferencesKey("home_ssid")
    val HOME_BSSID = stringPreferencesKey("home_bssid")
    val CURRENT_LOG_ID = intPreferencesKey("current_log_id")
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

suspend fun saveHomeWifi(context: Context, ssid: String, bssid: String) {
    context.dataStore.edit { prefs ->
        prefs[PrefsKeys.HOME_SSID] = ssid
        prefs[PrefsKeys.HOME_BSSID] = bssid
    }
}

fun homeWifiFlow(context: Context): Flow<Pair<String?, String?>> =
    context.dataStore.data.map { prefs ->
        Pair(prefs[PrefsKeys.HOME_SSID], prefs[PrefsKeys.HOME_BSSID])
    }

suspend fun saveCurrentLogId(context: Context, logId: Int) {
    context.dataStore.edit { prefs ->
        prefs[PrefsKeys.CURRENT_LOG_ID] = logId
    }
}

suspend fun clearCurrentLogId(context: Context) {
    context.dataStore.edit { prefs ->
        prefs.remove(PrefsKeys.CURRENT_LOG_ID)
    }
}

fun currentLogIdFlow(context: Context): Flow<Int?> =
    context.dataStore.data.map { prefs -> prefs[PrefsKeys.CURRENT_LOG_ID] }
