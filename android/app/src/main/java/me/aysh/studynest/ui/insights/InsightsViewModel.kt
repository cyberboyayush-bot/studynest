package me.aysh.studynest.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.aysh.studynest.data.model.WeekInsights
import me.aysh.studynest.data.remote.AppException
import me.aysh.studynest.data.repository.StudyRepository
import me.aysh.studynest.ui.Loadable
import me.aysh.studynest.util.Weeks
import java.time.Clock
import java.time.LocalDate

data class InsightsUiState(
    val weekStart: LocalDate,
    val currentWeekStart: LocalDate,
    val today: LocalDate,
    val content: Loadable<WeekInsights> = Loadable.Loading,
) {
    val isCurrentWeek: Boolean get() = weekStart == currentWeekStart
    val canGoNext: Boolean get() = weekStart < currentWeekStart
    val rangeLabel: String get() = Weeks.rangeLabel(weekStart)
}

class InsightsViewModel(
    private val repository: StudyRepository,
    private val clock: Clock,
) : ViewModel() {

    private val week = MutableStateFlow(Weeks.startOf(LocalDate.now(clock)))
    private val reloads = MutableStateFlow(0)

    private val _state = MutableStateFlow(
        InsightsUiState(weekStart = week.value, currentWeekStart = week.value, today = LocalDate.now(clock)),
    )
    val state: StateFlow<InsightsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(week, repository.revision, reloads) { w, _, _ -> w }.collectLatest { weekStart ->
                val today = LocalDate.now(clock)
                _state.update {
                    // Switching weeks must never show the previous week's numbers under the new label.
                    val content = if (it.weekStart == weekStart) it.content else Loadable.Loading
                    it.copy(weekStart = weekStart, currentWeekStart = Weeks.startOf(today), today = today, content = content)
                }
                val result = try {
                    Loadable.Ready(repository.insights(weekStart))
                } catch (e: AppException) {
                    Loadable.Failed(e.message)
                }
                _state.update { if (it.weekStart == weekStart) it.copy(content = result) else it }
            }
        }
    }

    fun previousWeek() = week.update { it.minusWeeks(1) }

    fun nextWeek() = week.update { current ->
        val next = current.plusWeeks(1)
        if (next <= Weeks.startOf(LocalDate.now(clock))) next else current
    }

    fun retry() {
        _state.update { it.copy(content = Loadable.Loading) }
        reloads.update { it + 1 }
    }
}
