package me.aysh.studynest.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface StudyNestApi {

    // Auth: the body is ignored (only the set-auth-token header matters), so these return
    // Response<Unit>. A DTO that is never read would be removed by R8 and break the converter.

    @POST("api/auth/sign-up/email")
    suspend fun signUp(@Body body: SignUpRequest): Response<Unit>

    @POST("api/auth/sign-in/email")
    suspend fun signIn(@Body body: SignInRequest): Response<Unit>

    @POST("api/auth/sign-out")
    suspend fun signOut(@Body body: EmptyBody = EmptyBody()): Response<Unit>

    // Profile

    @GET("api/me")
    suspend fun profile(): ProfileResponse

    @PATCH("api/me")
    suspend fun updateProfile(@Body body: UpdateProfileRequest): ProfileResponse

    // Subjects

    @GET("api/subjects")
    suspend fun subjects(): SubjectsResponse

    @POST("api/subjects")
    suspend fun createSubject(@Body body: SubjectRequest): SubjectResponse

    @PATCH("api/subjects/{id}")
    suspend fun updateSubject(@Path("id") id: String, @Body body: SubjectRequest): SubjectResponse

    @DELETE("api/subjects/{id}")
    suspend fun deleteSubject(@Path("id") id: String): Response<Unit>

    // Assignments

    @GET("api/assignments")
    suspend fun assignments(
        @Query("subjectId") subjectId: String? = null,
        @Query("status") status: String? = null,
        @Query("q") query: String? = null,
    ): AssignmentsResponse

    @GET("api/assignments/{id}")
    suspend fun assignment(@Path("id") id: String): AssignmentResponse

    @POST("api/assignments")
    suspend fun createAssignment(@Body body: AssignmentRequest): AssignmentResponse

    @PATCH("api/assignments/{id}")
    suspend fun updateAssignment(@Path("id") id: String, @Body body: AssignmentRequest): AssignmentResponse

    @PATCH("api/assignments/{id}")
    suspend fun setCompletion(@Path("id") id: String, @Body body: CompletionRequest): AssignmentResponse

    @DELETE("api/assignments/{id}")
    suspend fun deleteAssignment(@Path("id") id: String): Response<Unit>

    // Focus

    @GET("api/focus-sessions")
    suspend fun focusSessions(@Query("week") weekStart: String): FocusSessionsResponse

    @POST("api/focus-sessions")
    suspend fun createFocusSession(@Body body: FocusSessionRequest): FocusSessionResponse

    @GET("api/insights")
    suspend fun insights(@Query("week") weekStart: String, @Query("today") today: String): InsightsDto
}
