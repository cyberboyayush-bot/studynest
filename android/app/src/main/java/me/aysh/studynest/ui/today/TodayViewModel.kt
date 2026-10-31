package me.aysh.studynest.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.aysh.studynest.data.focus.FocusTimer
import me.aysh.studynest.data.focus.FocusTimerState
import me.aysh.studynest.data.model.Assignment
import me.aysh.studynest.data.model.AssignmentStatus
import me.aysh.studynest.data.remote.AppException
import me.aysh.studynest.data.repository.StudyRepository
import me.aysh.studynest.ui.Loadable
import me.aysh.studynest.util.Weeks
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime

data class TodayData(
    val firstName: String,
    val today: LocalDate,
    val now: LocalTime,
    /** Open assignments that are overdue or due within the next week, soonest first. */
    val dueSoon: List<Assignment>,
    val overdueCount: Int,
    val todaySeconds: Long,
    val goalMinutes: Int,
    val currentStreak: Int,
    val subjectCount: Int,
    val openAssignmentCount: Int,
) {
    /** A brand-new student with nothing set up yet. */
    val isNewUser: Boolean get() = subjectCount == 0 && openAssignmentCount == 0 && todaySeconds == 0L
}

data class TodayUiState(
    val content: Loadable<TodayData> = Loadable.Loading,
    val isRefreshing: Boolean = false,
)

class TodayViewModel(
    private val repository: StudyRepository,
    private val clock: Clock,
    focusTimer: FocusTimer,
) : ViewModel() {

    private val _state = MutableStateFlow(TodayUiState())
    val state: StateFlow<TodayUiState> = _state.asStateFlow()

    /** Lets Today show a "session in progress" pill. */
    val timer: StateFlow<FocusTimerState> = focusTimer.state
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), focusTimer.state.value)

    private val reloads = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            combine(repository.revision, reloads) { revision, reload -> revision to reload }
                .collectLatest { load() }
        }
    }

    fun refresh() {
        _state.update { it.copy(isRefreshing = true) }
        reloads.update { it + 1 }
    }

    private suspend fun load() {
        try {
            val data = coroutineScope {
                val today = LocalDate.now(clock)
                val profile = async { repository.profile() }
                val subjects = async { repository.subjects() }
                val open = async { repository.assignments(status = AssignmentStatus.Open) }
                val insights = async { repository.insights(Weeks.startOf(today)) }

                val openList = open.await()
                val week = insights.await()
                TodayData(
                    firstName = profile.await().firstName,
                    today = today,
                    now = LocalTime.now(clock),
                    dueSoon = openList.filter { it.dueOn <= today.plusDays(DUE_SOON_DAYS) }.take(MAX_DUE_SOON),
                    overdueCount = openList.count { it.dueOn < today },
                    todaySeconds = week.days.firstOrNull { it.date == today }?.seconds ?: 0L,
                    goalMinutes = week.dailyGoalMinutes,
                    currentStreak = week.currentStreak,
                    subjectCount = subjects.await().size,
                    openAssignmentCount = openList.size,
                )
            }
            _state.value = TodayUiState(Loadable.Ready(data))
        } catch (e: AppException) {
            _state.value = TodayUiState(Loadable.Failed(e.message))
        }
    }

    private companion object {
        const val DUE_SOON_DAYS = 7L
        const val MAX_DUE_SOON = 5
    }
}
