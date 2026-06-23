package com.nxvpn.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nxvpn.app.data.model.ServerProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "nxvpn_profiles")

/** Persists the user's imported [ServerProfile]s as a JSON blob in DataStore. */
class ProfileRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val key = stringPreferencesKey("profiles_json")

    val profiles: Flow<List<ServerProfile>> = context.dataStore.data.map { prefs ->
        prefs[key]?.let { runCatching { json.decodeFromString<List<ServerProfile>>(it) }.getOrNull() }
            ?: emptyList()
    }

    suspend fun upsert(profile: ServerProfile) = mutate { current ->
        val without = current.filterNot { it.id == profile.id }
        without + profile
    }

    suspend fun delete(id: String) = mutate { current -> current.filterNot { it.id == id } }

    private suspend fun mutate(transform: (List<ServerProfile>) -> List<ServerProfile>) {
        context.dataStore.edit { prefs ->
            val current = prefs[key]?.let {
                runCatching { json.decodeFromString<List<ServerProfile>>(it) }.getOrNull()
            } ?: emptyList()
            prefs[key] = json.encodeToString(transform(current))
        }
    }
}
