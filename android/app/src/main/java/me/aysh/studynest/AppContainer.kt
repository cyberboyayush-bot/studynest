package me.aysh.studynest

import android.content.Context
import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.aysh.studynest.data.focus.DataStoreFocusRunStore
import me.aysh.studynest.data.focus.FocusTimer
import me.aysh.studynest.data.remote.AppJson
import me.aysh.studynest.data.remote.createApi
import me.aysh.studynest.data.repository.AuthRepository
import me.aysh.studynest.data.repository.StudyRepository
import me.aysh.studynest.data.repository.TokenStore
import me.aysh.studynest.data.session.SessionState
import me.aysh.studynest.data.session.SessionStore
import me.aysh.studynest.data.session.TokenCipher
import java.time.Clock

/** Manual dependency container: one instance of each shared object for the whole app. */
class AppContainer(context: Context) {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val clock: Clock = Clock.systemDefaultZone()

    val sessionStore = SessionStore(context, TokenCipher(), appScope)

    private val api = createApi(
        baseUrl = BuildConfig.API_BASE_URL,
        tokenProvider = { sessionStore.currentToken },
        onUnauthorized = { token -> appScope.launch { sessionStore.clearIfCurrent(token) } },
    )

    val authRepository = AuthRepository(
        api = api,
        tokens = object : TokenStore {
            override suspend fun save(token: String) = sessionStore.save(token)
            override suspend fun clear() = sessionStore.clear()
        },
        json = AppJson,
    )

    val studyRepository = StudyRepository(api, AppJson, clock)

    val focusTimer = FocusTimer(
        scope = mainScope,
        store = DataStoreFocusRunStore(context, AppJson),
        logger = { subjectId, startedAt, seconds -> studyRepository.logFocusSession(subjectId, startedAt, seconds) },
        elapsedRealtime = SystemClock::elapsedRealtime,
        clock = clock,
    )

    init {
        // A timer belongs to the signed-in student: resume it after sign-in, drop it on sign-out.
        mainScope.launch {
            sessionStore.state.collect { state ->
                when (state) {
                    is SessionState.SignedIn -> focusTimer.restore()
                    SessionState.SignedOut -> focusTimer.reset()
                    SessionState.Loading -> Unit
                }
            }
        }
    }
}
