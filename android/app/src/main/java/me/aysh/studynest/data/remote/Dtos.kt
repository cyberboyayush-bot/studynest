package me.aysh.studynest.data.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive

@Serializable
data class SignUpRequest(val name: String, val email: String, val password: String)

@Serializable
data class SignInRequest(val email: String, val password: String)

@Serializable
data class SubjectRefDto(val id: String, val name: String, val color: String, val icon: String)

@Serializable
data class SubjectDto(
    val id: String,
    val name: String,
    val color: String,
    val icon: String,
    val openCount: Int = 0,
    val totalCount: Int = 0,
)

@Serializable
data class SubjectRequest(val name: String, val color: String, val icon: String)

@Serializable
data class SubjectResponse(val subject: SubjectDto)

@Serializable
data class SubjectsResponse(val subjects: List<SubjectDto>)

@Serializable
data class AssignmentDto(
    val id: String,
    val title: String,
    val notes: String = "",
    val dueOn: String,
    val priority: String,
    val completedOn: String? = null,
    val subject: SubjectRefDto,
)

@Serializable
data class AssignmentRequest(
    val title: String,
    val subjectId: String,
    val dueOn: String,
    val priority: String,
    val notes: String,
)

/**
 * Marks an assignment done (a date) or open again (JSON null). A [JsonElement] is used because
 * [AppJson] omits null properties, but here null is meaningful and must be sent.
 */
@Serializable
data class CompletionRequest(val completedOn: JsonElement) {
    companion object {
        fun of(date: String?) = CompletionRequest(date?.let(::JsonPrimitive) ?: JsonNull)
    }
}

@Serializable
data class AssignmentResponse(val assignment: AssignmentDto)

@Serializable
data class AssignmentsResponse(val assignments: List<AssignmentDto>)

@Serializable
data class FocusSessionDto(
    val id: String,
    val subject: SubjectRefDto? = null,
    val startedAt: String,
    val durationSeconds: Int,
    val day: String,
)

@Serializable
data class FocusSessionRequest(
    val id: String,
    val subjectId: String?,
    val startedAt: String,
    val durationSeconds: Int,
    val day: String,
)

@Serializable
data class FocusSessionResponse(val session: FocusSessionDto)

@Serializable
data class FocusSessionsResponse(val sessions: List<FocusSessionDto>)

@Serializable
data class DayFocusDto(val date: String, val seconds: Long)

@Serializable
data class SubjectFocusDto(val subject: SubjectRefDto? = null, val seconds: Long)

@Serializable
data class StreakDto(val current: Int, val longest: Int)

@Serializable
data class InsightsDto(
    val weekStart: String,
    val totalSeconds: Long,
    val sessionCount: Int,
    val days: List<DayFocusDto>,
    val bySubject: List<SubjectFocusDto>,
    val assignmentsCompleted: Int,
    val streak: StreakDto,
    val dailyGoalMinutes: Int,
)

@Serializable
data class ProfileDto(
    val id: String,
    val name: String,
    val email: String,
    val dailyFocusGoalMinutes: Int,
    val createdAt: String,
)

@Serializable
data class ProfileResponse(val profile: ProfileDto)

/** Null fields are omitted from the JSON body, so only the provided fields are updated. */
@Serializable
data class UpdateProfileRequest(val name: String? = null, val dailyFocusGoalMinutes: Int? = null)

/** Covers both the app API shape `{ error: { code, message } }` and Better Auth's `{ code, message }`. */
@Serializable
data class ErrorBody(
    val error: ErrorDetail? = null,
    val code: String? = null,
    val message: String? = null,
)

@Serializable
data class ErrorDetail(val code: String? = null, val message: String? = null)

@Serializable
class EmptyBody
