package com.example.turnoshospi.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject // O usar Gson/Kotlinx.serialization si está disponible

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "offline_store")

class DataStoreOfflineStore(private val context: Context) : OfflineStore {

    private val SHIFTS_JSON_KEY = stringPreferencesKey("shifts_map_json")

    override val shiftsMap: Flow<Map<String, String>> = context.dataStore.data
        .map { preferences ->
            val jsonString = preferences[SHIFTS_JSON_KEY] ?: "{}"
            parseJsonToMap(jsonString)
        }

    override suspend fun saveShift(date: String, shiftId: String) {
        context.dataStore.edit { preferences ->
            val currentJson = preferences[SHIFTS_JSON_KEY] ?: "{}"
            val map = parseJsonToMap(currentJson).toMutableMap()
            map[date] = shiftId
            preferences[SHIFTS_JSON_KEY] = JSONObject(map as Map<*, *>).toString()
        }
    }

    override suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }

    private fun parseJsonToMap(jsonString: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val jsonObject = JSONObject(jsonString)
        jsonObject.keys().forEach { key ->
            map[key] = jsonObject.getString(key)
        }
        return map
    }
}
