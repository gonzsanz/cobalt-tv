package io.github.gonzsanz.cobalttv.data.favorites

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Persists the IDs of favourite channels.
 *
 * Channel IDs come from the importers and are stable for the same name + URL,
 * so a favourite survives refreshing the playlist. IDs of channels that are no
 * longer part of the active source are kept: if the user imports the same list
 * again, their favourites come back instead of being silently lost.
 *
 * A single playlist is active at a time, so this is intentionally a small
 * SharedPreferences-backed set. Room/DataStore can replace it later without
 * changing the screens, which only observe [favorites].
 */
class FavoritesRepository(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _favorites = MutableStateFlow(
        prefs.getStringSet(KEY_CHANNEL_IDS, null)?.toSet().orEmpty(),
    )
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    /** Adds or removes [channelId]. Returns true when it is now a favourite. */
    fun toggle(channelId: String): Boolean {
        val current = _favorites.value
        val next = if (channelId in current) current - channelId else current + channelId
        _favorites.value = next
        prefs.edit().putStringSet(KEY_CHANNEL_IDS, next).apply()
        return channelId in next
    }

    private companion object {
        const val PREFS_NAME = "cobalt_favorites"
        const val KEY_CHANNEL_IDS = "channel_ids"
    }
}
