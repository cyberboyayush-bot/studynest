package me.aysh.studynest.data.focus

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import me.aysh.studynest.data.model.SubjectRef
import me.aysh.studynest.data.remote.AppException
import java.time.Clock
import java.time.Instant

/**
 * One focus run. Time is derived from timestamps, never from counting ticks, so the timer stays
 * correct while the app is in the background or the process is restarted.
 */
data class FocusRun(
    val subject: SubjectRef?,
    val plannedMillis: Long,
    /** Wall-clock start, recorded with the session. */
    val startedAt: Instant,
    /** Running time from stretches that have already ended (before a pause). */
    val accumulatedMillis: Long,
    /** Monotonic time ([FocusTimer.elapsedRealtime]) when the current stretch began; null while paused. */
    val resumedAtElapsed: Long?,
    /** Wall-clock twin of [resumedAtElapsed], used only if the monotonic clock reset (device reboot). */
    val resumedAtWallMillis: Long?,
) {
    val isPaused: Boolean get() = resumedAtElapsed == null

    fun elapsedMillis(nowElapsed: Long, nowWallMillis: Long): Long {
        val stretch = when {
            resumedAtElapsed == null -> 0L
            nowElapsed >= resumedAtElapsed -> nowElapsed - resumedAtElapsed
            else -> (nowWallMillis - (resumedAtWallMillis ?: nowWallMillis)).coerceAtLeast(0L)
        }
        return (accumulatedMillis + stretch).coerceAtMost(plannedMillis)
    }
}

/** Where an in-progress run is kept so it survives process death. */
interface FocusRunStore {
    suspend fun load(): FocusRun?
    suspend fun save(run: FocusRun?)
}

sealed interface FocusTimerState {
    data object Idle : FocusTimerState

    data class Active(val run: FocusRun, val elapsedMillis: Long) : FocusTimerState {
        val remainingMillis: Long get() = (run.plannedMillis - elapsedMillis).coerceAtLeast(0L)
        val progress: Float get() = if (run.plannedMillis == 0L) 0f else elapsedMillis.toFloat() / run.plannedMillis
        val isPaused: Boolean get() = run.isPaused
    }

    data class Saving(val subject: SubjectRef?, val durationSeconds: Int) : FocusTimerState

    /**
     * The run ended. [saved] tells whether it was recorded; [error] is set when saving failed
     * (and [retry][FocusTimer.retrySave] is possible); [tooShort] when it was stopped under a minute.
     */
    data class Finished(
        val subject: SubjectRef?,
        val durationSeconds: Int,
        val completed: Boolean,
        val saved: Boolean,
        val tooShort: Boolean = false,
        val error: String? = null,
    ) : FocusTimerState
}

/** Records a finished session; returns normally on success or throws [AppException]. */
fun interface FocusSessionLogger {
    suspend fun log(subjectId: String?, startedAt: Instant, durationSeconds: Int)
}

/**
 * App-wide focus timer. All methods must be called on [scope]'s (single, main) thread.
 */
class FocusTimer(
    private val scope: CoroutineScope,
    private val store: FocusRunStore,
    private val logger: FocusSessionLogger,
    private val elapsedRealtime: () -> Long,
    private val clock: Clock,
) {

    private val _state = MutableStateFlow<FocusTimerState>(FocusTimerState.Idle)
    val state: StateFlow<FocusTimerState> = _state.asStateFlow()

    private var ticker: Job? = null
    private var pendingSave: PendingSave? = null

    private data class PendingSave(val subject: SubjectRef?, val startedAt: Instant, val seconds: Int, val completed: Boolean)

    private var persistJob: Job? = null

    /** Picks up a run that was in progress when the process last died. Call once signed in. */
    fun restore() {
        if (_state.value !is FocusTimerState.Idle) return
        val previous = persistJob
        scope.launch {
            previous?.join()
            val restored = runCatching { store.load() }.getOrNull() ?: return@launch
            if (_state.value is FocusTimerState.Idle) {
                publish(restored)
                if (!restored.isPaused) startTicker()
            }
        }
    }

    fun start(subject: SubjectRef?, minutes: Int) {
        if (_state.value is FocusTimerState.Active || _state.value is FocusTimerState.Saving) return
        val run = FocusRun(
            subject = subject,
            plannedMillis = minutes.coerceAtLeast(1) * 60_000L,
            startedAt = clock.instant(),
            accumulatedMillis = 0L,
            resumedAtElapsed = elapsedRealtime(),
            resumedAtWallMillis = clock.millis(),
        )
        pendingSave = null
        update(run)
        startTicker()
    }

    fun pause() {
        val active = _state.value as? FocusTimerState.Active ?: return
        if (active.isPaused) return
        ticker?.cancel()
        update(active.run.copy(accumulatedMillis = elapsedNow(active.run), resumedAtElapsed = null, resumedAtWallMillis = null))
    }

    fun resume() {
        val active = _state.value as? FocusTimerState.Active ?: return
        if (!active.isPaused) return
        update(active.run.copy(resumedAtElapsed = elapsedRealtime(), resumedAtWallMillis = clock.millis()))
        startTicker()
    }

    /** Ends early. Runs of at least a minute are still recorded. */
    fun stop() {
        val active = _state.value as? FocusTimerState.Active ?: return
        ticker?.cancel()
        val seconds = (elapsedNow(active.run) / 1000).toInt()
        if (seconds < MIN_SAVED_SECONDS) {
            persist(null)
            _state.value = FocusTimerState.Finished(active.run.subject, seconds, completed = false, saved = false, tooShort = true)
            return
        }
        save(PendingSave(active.run.subject, active.run.startedAt, seconds, completed = false))
    }

    fun retrySave() {
        val pending = pendingSave ?: return
        if ((_state.value as? FocusTimerState.Finished)?.error == null) return
        save(pending)
    }

    /** Clears a finished run so a new one can start. */
    fun dismiss() {
        if (_state.value is FocusTimerState.Finished) {
            pendingSave = null
            _state.value = FocusTimerState.Idle
        }
    }

    /** Forgets everything, e.g. on sign-out. */
    fun reset() {
        ticker?.cancel()
        pendingSave = null
        persist(null)
        _state.value = FocusTimerState.Idle
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = scope.launch {
            while (isActive) {
                val active = _state.value as? FocusTimerState.Active ?: break
                if (active.isPaused) break
                val elapsed = elapsedNow(active.run)
                if (elapsed >= active.run.plannedMillis) {
                    save(PendingSave(active.run.subject, active.run.startedAt, (active.run.plannedMillis / 1000).toInt(), completed = true))
                    break
                }
                _state.value = active.copy(elapsedMillis = elapsed)
                delay(TICK_MILLIS)
            }
        }
    }

    private fun save(pending: PendingSave) {
        pendingSave = pending
        persist(null)
        _state.value = FocusTimerState.Saving(pending.subject, pending.seconds)
        scope.launch {
            _state.value = try {
                logger.log(pending.subject?.id, pending.startedAt, pending.seconds)
                pendingSave = null
                FocusTimerState.Finished(pending.subject, pending.seconds, pending.completed, saved = true)
            } catch (e: AppException) {
                FocusTimerState.Finished(pending.subject, pending.seconds, pending.completed, saved = false, error = e.message)
            }
        }
    }

    private fun update(run: FocusRun) {
        publish(run)
        persist(run)
    }

    private fun publish(run: FocusRun) {
        _state.value = FocusTimerState.Active(run, elapsedNow(run))
    }

    /** Writes are chained so they land in call order; a stale run can never overwrite a newer one. */
    private fun persist(run: FocusRun?) {
        val previous = persistJob
        persistJob = scope.launch {
            previous?.join()
            runCatching { store.save(run) }
        }
    }

    private fun elapsedNow(run: FocusRun) = run.elapsedMillis(elapsedRealtime(), clock.millis())

    companion object {
        const val MIN_SAVED_SECONDS = 60
        private const val TICK_MILLIS = 200L
    }
}
