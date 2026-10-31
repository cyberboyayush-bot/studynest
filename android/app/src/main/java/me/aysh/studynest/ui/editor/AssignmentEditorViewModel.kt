package me.aysh.studynest.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.aysh.studynest.data.model.AssignmentDraft
import me.aysh.studynest.data.model.Priority
import me.aysh.studynest.data.model.Subject
import me.aysh.studynest.data.remote.AppException
import me.aysh.studynest.data.repository.StudyRepository
import me.aysh.studynest.util.Validation
import java.time.Clock
import java.time.LocalDate

data class AssignmentEditorUiState(
    val isNew: Boolean,
    /** True until the assignment (when editing) and the subject list have loaded. */
    val isLoading: Boolean = true,
    /** Set when loading failed; saving is disabled so defaults can never overwrite the real item. */
    val loadError: String? = null,
    val subjects: List<Subject> = emptyList(),
    val title: String = "",
    val subjectId: String? = null,
    val dueOn: LocalDate,
    val priority: Priority = Priority.Medium,
    val notes: String = "",
    val isDone: Boolean = false,
    val showErrors: Boolean = false,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val confirmDelete: Boolean = false,
    val formError: String? = null,
) {
    val titleError: String? get() = if (showErrors) Validation.title(title) else null
    val subjectError: String? get() = if (showErrors && subjectId == null) "Pick a subject" else null
    val notesError: String? get() = Validation.notes(notes)
    val canEdit: Boolean get() = !isLoading && loadError == null && !isSaving && !isDeleting
    val needsSubject: Boolean get() = !isLoading && loadError == null && subjects.isEmpty()
}

sealed interface EditorEvent {
    data class Closed(val message: String) : EditorEvent
}

class AssignmentEditorViewModel(
    private val assignmentId: String?,
    presetSubjectId: String?,
    private val repository: StudyRepository,
    clock: Clock,
) : ViewModel() {

    private val _state = MutableStateFlow(
        AssignmentEditorUiState(
            isNew = assignmentId == null,
            subjectId = presetSubjectId,
            dueOn = LocalDate.now(clock).plusDays(1),
        ),
    )
    val state: StateFlow<AssignmentEditorUiState> = _state.asStateFlow()

    private val _events = Channel<EditorEvent>(Channel.BUFFERED)
    val events: Flow<EditorEvent> = _events.receiveAsFlow()

    init {
        load()
        // New subjects (e.g. created from the empty state) show up without touching the form.
        viewModelScope.launch {
            repository.revision.drop(1).collect {
                runCatching { repository.subjects() }.onSuccess { subjects -> _state.update { it.copy(subjects = subjects) } }
            }
        }
    }

    fun retry() {
        _state.update { it.copy(isLoading = true, loadError = null) }
        load()
    }

    private fun load() {
        viewModelScope.launch {
            try {
                coroutineScope {
                    val subjects = async { repository.subjects() }
                    val existing = assignmentId?.let { id -> async { repository.assignment(id) } }
                    val item = existing?.await()
                    val list = subjects.await()
                    _state.update { state ->
                        val base = state.copy(isLoading = false, loadError = null, subjects = list)
                        if (item == null) {
                            // Default to the only subject, or keep a preset one if it still exists.
                            val subjectId = state.subjectId?.takeIf { id -> list.any { it.id == id } }
                                ?: list.singleOrNull()?.id
                            base.copy(subjectId = subjectId)
                        } else {
                            base.copy(
                                title = item.title,
                                subjectId = item.subject.id,
                                dueOn = item.dueOn,
                                priority = item.priority,
                                notes = item.notes,
                                isDone = item.isDone,
                            )
                        }
                    }
                }
            } catch (e: AppException) {
                _state.update { it.copy(isLoading = false, loadError = e.message) }
            }
        }
    }

    fun onTitleChange(value: String) = edit { it.copy(title = value.take(Validation.MAX_TITLE), formError = null) }
    fun onSubjectChange(id: String) = edit { it.copy(subjectId = id, formError = null) }
    fun onDueDateChange(date: LocalDate) = edit { it.copy(dueOn = date, formError = null) }
    fun onPriorityChange(priority: Priority) = edit { it.copy(priority = priority, formError = null) }
    fun onNotesChange(value: String) = edit { it.copy(notes = value.take(Validation.MAX_NOTES), formError = null) }

    fun save() {
        val current = _state.value
        if (!current.canEdit) return
        val subjectId = current.subjectId
        if (Validation.title(current.title) != null || subjectId == null || current.notesError != null) {
            _state.update { it.copy(showErrors = true) }
            return
        }
        _state.update { it.copy(isSaving = true, showErrors = true, formError = null) }
        viewModelScope.launch {
            val draft = AssignmentDraft(current.title, subjectId, current.dueOn, current.priority, current.notes)
            try {
                if (assignmentId == null) repository.addAssignment(draft) else repository.updateAssignment(assignmentId, draft)
                _events.send(EditorEvent.Closed(if (assignmentId == null) "Assignment added" else "Changes saved"))
            } catch (e: AppException) {
                _state.update { it.copy(isSaving = false, formError = e.message) }
            }
        }
    }

    fun requestDelete() = _state.update { if (assignmentId != null && it.canEdit) it.copy(confirmDelete = true) else it }

    fun cancelDelete() = _state.update { it.copy(confirmDelete = false) }

    fun confirmDelete() {
        val id = assignmentId ?: return
        if (_state.value.isDeleting) return
        _state.update { it.copy(isDeleting = true, confirmDelete = false, formError = null) }
        viewModelScope.launch {
            try {
                repository.deleteAssignment(id)
                _events.send(EditorEvent.Closed("Assignment deleted"))
            } catch (e: AppException) {
                _state.update { it.copy(isDeleting = false, formError = e.message) }
            }
        }
    }

    private fun edit(transform: (AssignmentEditorUiState) -> AssignmentEditorUiState) =
        _state.update { if (it.canEdit) transform(it) else it }
}
