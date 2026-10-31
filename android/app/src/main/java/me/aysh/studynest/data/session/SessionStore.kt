package me.aysh.studynest.data.session

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

private val Context.sessionDataStore by preferencesDataStore(
    name = "session",
    // A corrupt file can't hold a usable session: start over signed out instead of crashing.
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

sealed interface SessionState {
    data object Loading : SessionState
    data object SignedOut : SessionState
    data class SignedIn(val token: String) : SessionState
}

/** Persists the bearer token, encrypted with [TokenCipher], and exposes it as observable state. */
class SessionStore(context: Context, private val cipher: TokenCipher, scope: CoroutineScope) {

    private val dataStore = context.applicationContext.sessionDataStore

    val state: StateFlow<SessionState> = dataStore.data
        // An unreadable file means there is no usable session: report signed out (so the splash never hangs),
        // then keep listening so a later successful sign-in is still picked up.
        .retryWhen { cause, attempt ->
            if (cause !is IOException) return@retryWhen false
            emit(emptyPreferences())
            delay(minOf(1_000L * (attempt + 1), 10_000L))
            true
        }
        .map { prefs ->
            prefs[TOKEN_KEY]?.let(cipher::decrypt)?.let { SessionState.SignedIn(it) } ?: SessionState.SignedOut
        }
        .stateIn(scope, SharingStarted.Eagerly, SessionState.Loading)

    val currentToken: String? get() = (state.value as? SessionState.SignedIn)?.token

    suspend fun save(token: String) {
        val encrypted = withContext(Dispatchers.IO) { cipher.encrypt(token) }
        dataStore.edit { it[TOKEN_KEY] = encrypted }
    }

    suspend fun clear() {
        dataStore.edit { it.remove(TOKEN_KEY) }
    }

    /** Clears the session only if it still holds [token], so a late 401 can't end a newer session. */
    suspend fun clearIfCurrent(token: String) {
        dataStore.edit { prefs ->
            if (prefs[TOKEN_KEY]?.let(cipher::decrypt) == token) prefs.remove(TOKEN_KEY)
        }
    }

    private companion object {
        val TOKEN_KEY = stringPreferencesKey("token")
    }
}
