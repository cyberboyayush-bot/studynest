package me.aysh.studynest.ui.assignments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.aysh.studynest.data.model.Assignment
import me.aysh.studynest.data.model.AssignmentStatus
import me.aysh.studynest.data.model.Subject
import me.aysh.studynest.data.remote.AppException
import me.aysh.studynest.data.repository.StudyRepository
import me.aysh.studynest.ui.Loadable
import me.aysh.studynest.util.Due
import me.aysh.studynest.util.DueBucket
import java.time.Clock
import java.time.LocalDate

data class AssignmentFilters(
    val subjectId: String? = null,
    val query: String = "",
    val showCompleted: Boolean = false,
) {
    val isFiltering: Boolean get() = subjectId != null || query.isNotBlank()
}

data class AssignmentGroup(val bucket: DueBucket, val items: List<Assignment>)

data class AssignmentsUiState(
    val filters: AssignmentFilters = AssignmentFilters(),
    val subjects: List<Subject> = emptyList(),
    val groups: Loadable<List<AssignmentGroup>> = Loadable.Loading,
    val today: LocalDate = LocalDate.now(),
) {
    val isEmpty: Boolean get() = groups.valueOrNull?.isEmpty() == true
}

/** A row was swiped; the snackbar offers to undo it. [previousCompletedOn] is what Undo restores. */
data class CompletionChange(
    val assignmentId: String,
    val title: String,
    val done: Boolean,
    val previousCompletedOn: LocalDate?,
)

sealed interface AssignmentsEvent {
    data class Changed(val change: CompletionChange) : AssignmentsEvent
    data class Message(val text: String) : AssignmentsEvent
}

class AssignmentsViewModel(
    private val repository: StudyRepository,
    private val clock: Clock,
) : ViewModel() {

    private val filters = MutableStateFlow(AssignmentFilters())
    private val reloads = MutableStateFlow(0)
    /** Rows hidden right after a swipe, before the server round-trip finishes. */
    private val pendingIds = MutableStateFlow<Set<String>>(emptySet())
    private val loaded = MutableStateFlow<Loadable<List<Assignment>>>(Loadable.Loading)

    private val _state = MutableStateFlow(AssignmentsUiState(today = LocalDate.now(clock)))
    val state: StateFlow<AssignmentsUiState> = _state.asStateFlow()

    private val _events = Channel<AssignmentsEvent>(Channel.BUFFERED)
    val events: Flow<AssignmentsEvent> = _events.receiveAsFlow()

    init {
        loadSubjects()
        loadAssignments()
        viewModelScope.launch {
            combine(filters, loaded, pendingIds) { f, list, hidden -> Triple(f, list, hidden) }.collect { (f, list, hidden) ->
                val today = LocalDate.now(clock)
                val groups = when (list) {
                    is Loadable.Ready -> Loadable.Ready(
                        Due.group(list.value.filterNot { it.id in hidden }, today).map { (bucket, items) ->
                            AssignmentGroup(bucket, items)
                        },
                    )
                    is Loadable.Failed -> list
                    Loadable.Loading -> Loadable.Loading
                }
                _state.update { it.copy(filters = f, groups = groups, today = today) }
            }
        }
    }

    @OptIn(FlowPreview::class)
    private fun loadAssignments() {
        // Typing is debounced; chip and toggle changes apply immediately because their query is unchanged.
        val query = filters.map { it.query.trim() }.distinctUntilChanged().debounce(SEARCH_DEBOUNCE_MS)
        val other = filters.map { it.subjectId to it.showCompleted }.distinctUntilChanged()
        viewModelScope.launch {
            combine(query, other, repository.revision, reloads) { q, (subjectId, showCompleted), _, _ ->
                Triple(q, subjectId, showCompleted)
            }.collectLatest { (q, subjectId, showCompleted) ->
                try {
                    val list = repository.assignments(
                        subjectId = subjectId,
                        status = if (showCompleted) AssignmentStatus.All else AssignmentStatus.Open,
                        query = q,
                    )
                    // Stop hiding swiped rows once the server no longer lists them as open.
                    pendingIds.update { ids -> ids.filterTo(mutableSetOf()) { id -> list.any { it.id == id && !it.isDone } } }
                    loaded.value = Loadable.Ready(list)
                } catch (e: AppException) {
                    loaded.value = Loadable.Failed(e.message)
                }
            }
        }
    }

    private fun loadSubjects() {
        viewModelScope.launch {
            combine(repository.revision, reloads) { a, b -> a to b }.collectLatest {
                runCatching { repository.subjects() }.onSuccess { subjects ->
                    _state.update { it.copy(subjects = subjects) }
                    // A deleted subject can't stay selected.
                    filters.update { f -> if (f.subjectId != null && subjects.none { s -> s.id == f.subjectId }) f.copy(subjectId = null) else f }
                }
            }
        }
    }

    fun onQueryChange(value: String) = filters.update { it.copy(query = value.take(60)) }

    fun onSubjectFilter(subjectId: String?) = filters.update { it.copy(subjectId = subjectId) }

    fun onShowCompletedChange(show: Boolean) = filters.update { it.copy(showCompleted = show) }

    fun clearFilters() = filters.update { it.copy(subjectId = null, query = "") }

    fun retry() {
        loaded.value = Loadable.Loading
        reloads.update { it + 1 }
    }

    /** Swipe action: marks an open assignment done, or reopens a completed one. */
    fun toggleDone(assignment: Assignment) {
        val done = !assignment.isDone
        // When completed items are hidden, the row disappears straight away.
        val hide = done && !filters.value.showCompleted
        if (hide) pendingIds.update { it + assignment.id }
        viewModelScope.launch {
            try {
                repository.setDone(assignment.id, done)
                _events.send(
                    AssignmentsEvent.Changed(CompletionChange(assignment.id, assignment.title, done, assignment.completedOn)),
                )
            } catch (e: AppException) {
                if (hide) pendingIds.update { it - assignment.id }
                _events.send(AssignmentsEvent.Message(e.message))
            }
        }
    }

    fun undo(change: CompletionChange) {
        viewModelScope.launch {
            try {
                repository.setCompletedOn(change.assignmentId, change.previousCompletedOn)
                pendingIds.update { it - change.assignmentId }
            } catch (e: AppException) {
                _events.send(AssignmentsEvent.Message(e.message))
            }
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 300L
    }
}
