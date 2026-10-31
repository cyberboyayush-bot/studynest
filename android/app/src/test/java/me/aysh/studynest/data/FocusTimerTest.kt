package me.aysh.studynest.data

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import me.aysh.studynest.data.focus.FocusRun
import me.aysh.studynest.data.focus.FocusRunStore
import me.aysh.studynest.data.focus.FocusTimer
import me.aysh.studynest.data.focus.FocusTimerState
import me.aysh.studynest.data.focus.PendingFocusSave
import me.aysh.studynest.data.model.SubjectColor
import me.aysh.studynest.data.model.SubjectIcon
import me.aysh.studynest.data.model.SubjectRef
import me.aysh.studynest.data.remote.AppException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class FocusTimerTest {

    private val subject = SubjectRef("s1", "Maths", SubjectColor.Sky, SubjectIcon.Calculator)

    private class FakeStore(var run: FocusRun? = null, var pending: PendingFocusSave? = null) : FocusRunStore {
        override suspend fun load() = run
        override suspend fun save(run: FocusRun?) { this.run = run }
        override suspend fun loadPending() = pending
        override suspend fun savePending(pending: PendingFocusSave?) { this.pending = pending }
    }

    private data class Logged(val id: String, val subjectId: String?, val startedAt: Instant, val seconds: Int)

    private class Harness(scope: TestScope, store: FocusRunStore = FakeStore(), var failWith: AppException? = null) {
        /** Monotonic clock the test moves by hand, independent of coroutine virtual time. */
        var elapsed = 1_000_000L
        /** Wall clock, also moved by hand. */
        var wall = Instant.parse("2026-10-14T09:00:00Z")
        val logged = mutableListOf<Logged>()
        /** When set, uploads hang until the test completes this. */
        var gate: CompletableDeferred<Unit>? = null
        private var ids = 0
        val clock: Clock = object : Clock() {
            override fun getZone() = ZoneOffset.UTC
            override fun withZone(zone: ZoneId?) = this
            override fun instant() = wall
        }
        val timer = FocusTimer(
            scope = scope.backgroundScope,
            store = store,
            logger = { id, subjectId, at, seconds ->
                gate?.await()
                failWith?.let { throw it }
                logged += Logged(id, subjectId, at, seconds)
            },
            elapsedRealtime = { elapsed },
            clock = clock,
            newId = { "id-${++ids}" },
        )
    }

    private fun TestScope.settle() {
        advanceTimeBy(500)
        runCurrent()
    }

    @Test
    fun `time comes from timestamps, not from ticks`() = runTest {
        val h = Harness(this)
        h.timer.start(subject, 25)
        // Simulate the app sleeping in the background: lots of real time, a single tick.
        h.elapsed += 10 * 60_000L
        settle()
        val active = h.timer.state.value as FocusTimerState.Active
        assertEquals(15 * 60_000L, active.remainingMillis)
        assertEquals(0.4f, active.progress, 0.001f)
    }

    @Test
    fun `pausing freezes the clock until resumed`() = runTest {
        val h = Harness(this)
        h.timer.start(subject, 25)
        h.elapsed += 60_000L
        h.timer.pause()
        h.elapsed += 30 * 60_000L
        settle()
        val paused = h.timer.state.value as FocusTimerState.Active
        assertTrue(paused.isPaused)
        assertEquals(60_000L, paused.elapsedMillis)

        h.timer.resume()
        h.elapsed += 60_000L
        settle()
        assertEquals(120_000L, (h.timer.state.value as FocusTimerState.Active).elapsedMillis)
    }

    @Test
    fun `finishing saves the full planned duration`() = runTest {
        val h = Harness(this)
        h.timer.start(subject, 1)
        h.elapsed += 61_000L
        settle()
        val finished = h.timer.state.value as FocusTimerState.Finished
        assertTrue(finished.completed)
        assertTrue(finished.saved)
        assertEquals(listOf(Logged("id-1", "s1", Instant.parse("2026-10-14T09:00:00Z"), 60)), h.logged)
    }

    @Test
    fun `stopping early saves what was studied, but not under a minute`() = runTest {
        val h = Harness(this)
        h.timer.start(null, 25)
        h.elapsed += 30_000L
        h.timer.stop()
        settle()
        val short = h.timer.state.value as FocusTimerState.Finished
        assertTrue(short.tooShort)
        assertTrue(h.logged.isEmpty())

        h.timer.dismiss()
        h.timer.start(null, 25)
        h.elapsed += 10 * 60_000L + 900L
        h.timer.stop()
        settle()
        assertEquals(600, h.logged.single().seconds)
        assertNull(h.logged.single().subjectId)
    }

    @Test
    fun `a failed save can be retried`() = runTest {
        val h = Harness(this, failWith = AppException("offline", AppException.Kind.Network))
        h.timer.start(subject, 1)
        h.elapsed += 60_000L
        settle()
        assertEquals("offline", (h.timer.state.value as FocusTimerState.Finished).error)

        h.failWith = null
        h.timer.retrySave()
        settle()
        assertTrue((h.timer.state.value as FocusTimerState.Finished).saved)
        assertEquals(1, h.logged.size)
        assertEquals("the retry reuses the same id", "id-1", h.logged.single().id)
    }

    @Test
    fun `an unsent result survives process death and is retried with the same id`() = runTest {
        val store = FakeStore()
        val first = Harness(this, store, failWith = AppException("offline", AppException.Kind.Network))
        first.timer.start(subject, 1)
        first.elapsed += 60_000L
        settle()
        assertEquals("id-1", store.pending?.id)
        assertNull("the finished run itself is no longer persisted", store.run)

        val second = Harness(this, store)
        second.timer.restore()
        settle()
        assertTrue((second.timer.state.value as FocusTimerState.Finished).saved)
        assertEquals(listOf(Logged("id-1", "s1", Instant.parse("2026-10-14T09:00:00Z"), 60)), second.logged)
        assertNull("cleared once the server has it", store.pending)
    }

    @Test
    fun `reset wins over an upload that is still in flight`() = runTest {
        val store = FakeStore()
        val h = Harness(this, store)
        h.gate = CompletableDeferred()
        h.timer.start(subject, 1)
        h.elapsed += 60_000L
        settle()
        assertTrue(h.timer.state.value is FocusTimerState.Saving)

        h.timer.reset()
        h.gate!!.complete(Unit)
        settle()
        assertEquals(FocusTimerState.Idle, h.timer.state.value)
        assertNull(store.pending)
    }

    @Test
    fun `after a reboot the wall clock is trusted`() {
        val run = FocusRun(
            subject = null,
            plannedMillis = 25 * 60_000L,
            startedAt = Instant.EPOCH,
            accumulatedMillis = 0L,
            resumedAtElapsed = 500_000L,
            resumedAtWallMillis = 1_000_000L,
        )
        // Uptime restarted from zero and has since passed the stored value: only 1 s by uptime, 10 min by wall.
        assertEquals(10 * 60_000L, run.elapsedMillis(nowElapsed = 501_000L, nowWallMillis = 1_600_000L))
        // Normal case: both clocks agree.
        assertEquals(60_000L, run.elapsedMillis(nowElapsed = 560_000L, nowWallMillis = 1_060_000L))
    }

    @Test
    fun `a run is persisted and restored after process death`() = runTest {
        val store = FakeStore()
        val first = Harness(this, store)
        first.timer.start(subject, 25)
        first.elapsed += 5 * 60_000L
        first.timer.pause()
        settle()
        val saved = store.run!!
        assertEquals(5 * 60_000L, saved.accumulatedMillis)

        val second = Harness(this, store)
        second.timer.restore()
        settle()
        val restored = second.timer.state.value as FocusTimerState.Active
        assertTrue(restored.isPaused)
        assertEquals(20 * 60_000L, restored.remainingMillis)
        assertEquals(subject, restored.run.subject)
    }

    @Test
    fun `reset clears the persisted run`() = runTest {
        val store = FakeStore()
        val h = Harness(this, store)
        h.timer.start(subject, 25)
        settle()
        h.timer.reset()
        settle()
        assertEquals(FocusTimerState.Idle, h.timer.state.value)
        assertNull(store.run)
    }
}
