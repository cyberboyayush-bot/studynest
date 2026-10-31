package me.aysh.studynest.ui.subjects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.aysh.studynest.data.model.Subject
import me.aysh.studynest.data.model.SubjectColor
import me.aysh.studynest.data.model.SubjectDraft
import me.aysh.studynest.data.model.SubjectIcon
import me.aysh.studynest.data.remote.AppException
import me.aysh.studynest.data.repository.StudyRepository
import me.aysh.studynest.ui.Loadable
import me.aysh.studynest.util.Validation

/** State of the add/edit bottom sheet. [id] is null when adding. */
data class SubjectEditorState(
    val id: String? = null,
    val name: String = "",
    val color: SubjectColor = SubjectColor.Lilac,
    val icon: SubjectIcon = SubjectIcon.Book,
    val showErrors: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    /** Open assignments that would be deleted with the subject (edit mode only). */
    val assignmentCount: Int = 0,
) {
    val isNew: Boolean get() = id == null
    val nameError: String? get() = if (showErrors) Validation.subjectName(name) else null
}

data class SubjectsUiState(
    val subjects: Loadable<List<Subject>> = Loadable.Loading,
    val editor: SubjectEditorState? = null,
    /** Subject awaiting delete confirmation. */
    val confirmDelete: SubjectEditorState? = null,
    val isDeleting: Boolean = false,
)

class SubjectsViewModel(private val repository: StudyRepository) : ViewModel() {

    private val _state = MutableStateFlow(SubjectsUiState())
    val state: StateFlow<SubjectsUiState> = _state.asStateFlow()

    private val _messages = Channel<String>(Channel.BUFFERED)
    /** One-off snackbar messages. */
    val messages: Flow<String> = _messages.receiveAsFlow()

    private val reloads = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            combine(repository.revision, reloads) { a, b -> a to b }.collectLatest {
                val result = try {
                    Loadable.Ready(repository.subjects())
                } catch (e: AppException) {
                    Loadable.Failed(e.message)
                }
                _state.update { it.copy(subjects = result) }
            }
        }
    }

    fun retry() {
        _state.update { it.copy(subjects = Loadable.Loading) }
        reloads.update { it + 1 }
    }

    fun openNew() {
        val used = state.value.subjects.valueOrNull.orEmpty().map { it.color }.toSet()
        val color = SubjectColor.entries.firstOrNull { it !in used } ?: SubjectColor.Lilac
        _state.update { it.copy(editor = SubjectEditorState(color = color)) }
    }

    fun openEdit(subject: Subject) = _state.update {
        it.copy(
            editor = SubjectEditorState(
                id = subject.id,
                name = subject.name,
                color = subject.color,
                icon = subject.icon,
                assignmentCount = subject.totalCount,
            ),
        )
    }

    fun dismissEditor() = _state.update { if (it.editor?.isSaving == true) it else it.copy(editor = null) }

    fun onNameChange(value: String) = updateEditor { it.copy(name = value.take(Validation.MAX_SUBJECT_NAME), error = null) }
    fun onColorChange(value: SubjectColor) = updateEditor { it.copy(color = value) }
    fun onIconChange(value: SubjectIcon) = updateEditor { it.copy(icon = value) }

    fun save() {
        val editor = _state.value.editor ?: return
        if (editor.isSaving) return
        if (Validation.subjectName(editor.name) != null) {
            updateEditor { it.copy(showErrors = true) }
            return
        }
        updateEditor { it.copy(isSaving = true, showErrors = true, error = null) }
        viewModelScope.launch {
            val draft = SubjectDraft(editor.name, editor.color, editor.icon)
            try {
                if (editor.id == null) repository.addSubject(draft) else repository.updateSubject(editor.id, draft)
                _state.update { it.copy(editor = null) }
                _messages.send(if (editor.isNew) "Subject added" else "Subject updated")
            } catch (e: AppException) {
                updateEditor { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    fun requestDelete() = _state.update { state ->
        val editor = state.editor?.takeIf { !it.isNew && !it.isSaving } ?: return@update state
        state.copy(confirmDelete = editor)
    }

    fun cancelDelete() = _state.update { if (it.isDeleting) it else it.copy(confirmDelete = null) }

    fun confirmDelete() {
        val target = _state.value.confirmDelete ?: return
        val id = target.id ?: return
        if (_state.value.isDeleting) return
        _state.update { it.copy(isDeleting = true) }
        viewModelScope.launch {
            try {
                repository.deleteSubject(id)
                _state.update { it.copy(confirmDelete = null, editor = null, isDeleting = false) }
                _messages.send("Deleted ${target.name.trim()}")
            } catch (e: AppException) {
                _state.update { it.copy(confirmDelete = null, isDeleting = false) }
                _messages.send(e.message)
            }
        }
    }

    private fun updateEditor(transform: (SubjectEditorState) -> SubjectEditorState) =
        _state.update { state -> state.editor?.let { state.copy(editor = transform(it)) } ?: state }
}
