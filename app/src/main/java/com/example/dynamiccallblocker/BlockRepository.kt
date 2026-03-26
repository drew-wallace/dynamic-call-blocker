package com.example.dynamiccallblocker

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

class BlockRepository(context: Context) {
    private val dataStore = PreferenceDataStoreFactory.create {
        context.preferencesDataStoreFile("blocking_rules.preferences_pb")
    }

    private val blockExactKey = stringSetPreferencesKey("block_exact")
    private val blockPrefixKey = stringSetPreferencesKey("block_prefix")
    private val allowExactKey = stringSetPreferencesKey("allow_exact")
    private val allowPrefixKey = stringSetPreferencesKey("allow_prefix")
    private val allowContactsKey = booleanPreferencesKey("allow_contacts")

    val rulesFlow: Flow<BlockRules> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            BlockRules(
                blockExact = prefs[blockExactKey].orEmpty(),
                blockPrefix = prefs[blockPrefixKey].orEmpty(),
                allowExact = prefs[allowExactKey].orEmpty(),
                allowPrefix = prefs[allowPrefixKey].orEmpty(),
                allowContacts = prefs[allowContactsKey] ?: true
            )
        }

    suspend fun addRule(listType: ListType, mode: MatchMode, rawNumber: String) {
        val number = normalizeForRule(rawNumber)
        if (number.isEmpty()) return

        dataStore.edit { prefs ->
            val key = keyFor(listType, mode)
            prefs[key] = prefs[key].orEmpty().toMutableSet().apply { add(number) }
        }
    }

    suspend fun removeRule(listType: ListType, mode: MatchMode, number: String) {
        dataStore.edit { prefs ->
            val key = keyFor(listType, mode)
            prefs[key] = prefs[key].orEmpty().toMutableSet().apply { remove(number) }
        }
    }

    suspend fun setAllowContacts(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[allowContactsKey] = enabled
        }
    }

    private fun keyFor(listType: ListType, mode: MatchMode) = when (listType) {
        ListType.BLOCK -> if (mode == MatchMode.EXACT) blockExactKey else blockPrefixKey
        ListType.ALLOW -> if (mode == MatchMode.EXACT) allowExactKey else allowPrefixKey
    }

    companion object {
        fun normalizeForRule(number: String): String {
            return number.filter { it.isDigit() || it == '+' }
        }
    }
}

data class BlockRules(
    val blockExact: Set<String> = emptySet(),
    val blockPrefix: Set<String> = emptySet(),
    val allowExact: Set<String> = emptySet(),
    val allowPrefix: Set<String> = emptySet(),
    val allowContacts: Boolean = true
)

enum class ListType { BLOCK, ALLOW }
enum class MatchMode { EXACT, PREFIX }
