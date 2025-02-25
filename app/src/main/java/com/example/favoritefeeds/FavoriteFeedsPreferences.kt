package com.example.favoritefeeds

import android.content.Context
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class FavoriteFeedsPreferences(private val context: Context) {

    companion object { //to make sure only one instance of the object is created
        private val Context.datastore: DataStore<Preferences>
            by preferencesDataStore(name = "favoriteFeeds")
    }//companion object

    //return a Flow with the list of favorite feeds - Flows are suspend by default
    fun getPreferences(): Flow<Preferences> {
        return context.datastore.data
    }

    //map the Flow<Preferences> inro a Flow<String>
    fun getFeed(key: String): Flow<String> {
        return context.datastore.data.map { preferences ->
            preferences[stringPreferencesKey(key)] ?: ""
        }
    }

    suspend fun setFeed(feed: FeedData) {
        context.datastore.edit { preferences ->
            preferences[stringPreferencesKey(feed.tag)] = feed.path
        }
    }

    suspend fun removeFeed(feed: FeedData) {
        context.datastore.edit { preferences ->
            preferences.remove(stringPreferencesKey(feed.tag))
        }
    }

    suspend fun getValueByKey(key: Preferences.Key<*>): FeedData? {
        val value = context.datastore.data.map {
            it[key]
        }
        val actualValue = value.firstOrNull() ?: return null
        return FeedData(path = actualValue.toString(), tag = key.toString())
    }

    suspend fun readAllFeeds(): SnapshotStateList<FeedData> {
        val keys = context.datastore.data.map {
            it.asMap().keys
        }
        val actualKeys = keys.firstOrNull() ?: return SnapshotStateList()

        val listItems = SnapshotStateList<FeedData>()
        actualKeys.forEach {
            val path = context.datastore.data.map { preferences ->
                preferences[it].toString()
            }
            listItems.add(FeedData(path = path.firstOrNull() ?: "", tag = it.toString()))
        }
        return listItems
    }

    suspend fun removeAllFeeds() {
        context.datastore.edit { preferences ->
            preferences.clear()
        }
    }
}//FavoriteFeedsPreferences