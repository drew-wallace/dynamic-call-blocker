package com.example.dynamiccallblocker

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

private val Context.blockingRulesDataStore by preferencesDataStore(name = "blocking_rules.preferences_pb")

object AppContainer {
    @Volatile
    private var repository: BlockRepository? = null

    fun repository(context: Context): BlockRepository {
        return repository ?: synchronized(this) {
            repository ?: BlockRepository(context.applicationContext.blockingRulesDataStore)
                .also { repository = it }
        }
    }
}
