package me.aysh.studynest.data

import kotlinx.coroutines.test.runTest
import me.aysh.studynest.data.model.AssignmentDraft
import me.aysh.studynest.data.model.AssignmentStatus
import me.aysh.studynest.data.model.Priority
import me.aysh.studynest.data.model.SubjectColor
import me.aysh.studynest.data.model.SubjectDraft
import me.aysh.studynest.data.model.SubjectIcon
import me.aysh.studynest.data.remote.AppException
import me.aysh.studynest.data.remote.AppJson
import me.aysh.studynest.data.remote.createApi
import me.aysh.studynest.data.repository.AuthRepository
import me.aysh.studynest.data.repository.StudyRepository
import me.aysh.studynest.data.repository.TokenStore
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.SocketEffect
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class RepositoryTest {

    private lateinit var server: MockWebServer
    private var token: String? = null
    private val unauthorizedTokens = mutableListOf<String>()

    private val tokenStore = object : TokenStore {
        override suspend fun save(token: String) { this@RepositoryTest.token = token }
        override suspend fun clear() { token = null }
    }

    /** 2026-10-14 is a Wednesday; 20:30 in Kolkata is 15:00 UTC. */
    private val clock = Clock.fixed(Instant.parse("2026-10-14T15:00:00Z"), ZoneId.of("Asia/Kolkata"))

    private lateinit var auth: AuthRepository
    private lateinit var repo: StudyRepository

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val api = createApi(
            baseUrl = server.url("/").toString(),
            tokenProvider = { token },
            onUnauthorized = { unauthorizedTokens += it },
        )
        auth = AuthRepository(api, tokenStore, AppJson)
        repo = StudyRepository(api, AppJson, clock)
    }

    @After
    fun tearDown() = server.close()

    private fun json(code: Int, body: String, headers: Map<String, String> = emptyMap()) {
        val builder = MockResponse.Builder().code(code).body(body).addHeader("Content-Type", "application/json")
        headers.forEach { (k, v) -> builder.addHeader(k, v) }
        server.enqueue(builder.build())
    }

    private suspend fun expectFailure(block: suspend () -> Unit): AppException {
        try {
            block()
        } catch (e: AppException) {
            return e
        }
        fail("Expected AppException")
        error("unreachable")
    }

    private val subjectJson = """{"id":"s1","name":"Maths","color":"sky","icon":"calculator"}"""

    private fun assignmentJson(completedOn: String? = null) =
        """{"id":"a1","title":"Set 1","notes":"","dueOn":"2026-10-20","priority":"high",""" +
            """"completedOn":${completedOn?.let { "\"$it\"" } ?: "null"},"subject":$subjectJson}"""

    // Auth

    @Test
    fun `sign in stores the token from the response header and normalises the email`() = runTest {
        json(200, """{"user":{"id":"1"},"token":"x"}""", mapOf("set-auth-token" to "abc.sig"))
        auth.signIn("  A@B.co ", "password1")
        assertEquals("abc.sig", token)
        val request = server.takeRequest()
        assertEquals("/api/auth/sign-in/email", request.url.encodedPath)
        assertTrue(request.body!!.utf8().contains("\"email\":\"a@b.co\""))
    }

    @Test
    fun `sign up without a token header fails and stores nothing`() = runTest {
        json(200, """{"user":{"id":"1"}}""")
        val error = expectFailure { auth.signUp("Ayush", "a@b.co", "password1") }
        assertEquals(AppException.Kind.Unexpected, error.kind)
        assertNull(token)
    }

    @Test
    fun `wrong password maps to a friendly message without clearing anything`() = runTest {
        token = "old.sig"
        json(401, """{"code":"INVALID_EMAIL_OR_PASSWORD","message":"Invalid email or password"}""")
        val error = expectFailure { auth.signIn("a@b.co", "nope-nope") }
        assertEquals(AppException.Kind.InvalidCredentials, error.kind)
        assertEquals("Incorrect email or password", error.message)
        assertTrue("auth endpoints never trigger a session reset", unauthorizedTokens.isEmpty())
    }

    @Test
    fun `duplicate email maps to a conflict`() = runTest {
        json(422, """{"code":"USER_ALREADY_EXISTS_USE_ANOTHER_EMAIL","message":"User already exists"}""")
        val error = expectFailure { auth.signUp("A", "a@b.co", "password1") }
        assertEquals(AppException.Kind.Conflict, error.kind)
        assertEquals("An account with this email already exists", error.message)
    }

    @Test
    fun `sign out posts an empty body with the token, then clears it`() = runTest {
        token = "t.sig"
        json(200, """{"success":true}""")
        auth.signOut()
        assertNull(token)
        val request = server.takeRequest()
        assertEquals("/api/auth/sign-out", request.url.encodedPath)
        assertEquals("Bearer t.sig", request.headers["Authorization"])
        assertEquals("{}", request.body!!.utf8())
    }

    @Test
    fun `sign out clears the token even when offline`() = runTest {
        token = "t.sig"
        server.enqueue(MockResponse.Builder().onRequestStart(SocketEffect.CloseSocket()).build())
        auth.signOut()
        assertNull(token)
    }

    // Session handling

    @Test
    fun `app requests carry the bearer token`() = runTest {
        token = "abc.sig"
        json(200, """{"subjects":[]}""")
        repo.subjects()
        assertEquals("Bearer abc.sig", server.takeRequest().headers["Authorization"])
    }

    @Test
    fun `a 401 from the app api reports the token that was sent`() = runTest {
        token = "sent.sig"
        json(401, """{"error":{"code":"unauthorized","message":"Please sign in"}}""")
        val error = expectFailure { repo.subjects() }
        assertEquals(AppException.Kind.Unauthorized, error.kind)
        assertEquals(listOf("sent.sig"), unauthorizedTokens)
    }

    @Test
    fun `a 401 without a token does not report anything`() = runTest {
        json(401, """{"error":{"code":"unauthorized","message":"Please sign in"}}""")
        expectFailure { repo.profile() }
        assertTrue(unauthorizedTokens.isEmpty())
    }

    // Error mapping

    @Test
    fun `errors map to friendly kinds`() = runTest {
        val cases = listOf(
            Triple(404, """{"error":{"code":"not_found","message":"Assignment not found"}}""", AppException.Kind.NotFound),
            Triple(409, """{"error":{"code":"conflict","message":"You already have a subject with this name"}}""", AppException.Kind.Conflict),
            Triple(429, """{"message":"Too many requests"}""", AppException.Kind.RateLimited),
            Triple(400, """{"error":{"code":"bad_request","message":"title: Too big"}}""", AppException.Kind.Validation),
            Triple(500, """{"error":{"code":"internal_error","message":"boom"}}""", AppException.Kind.Server),
        )
        for ((status, body, kind) in cases) {
            json(status, body)
            assertEquals("HTTP $status", kind, expectFailure { repo.subjects() }.kind)
        }
    }

    @Test
    fun `server messages are shown without the field prefix`() = runTest {
        json(409, """{"error":{"code":"conflict","message":"You already have a subject with this name"}}""")
        assertEquals("You already have a subject with this name", expectFailure { repo.subjects() }.message)
        json(400, """{"error":{"code":"bad_request","message":"subjectId: Subject not found"}}""")
        assertEquals("Subject not found", expectFailure { repo.subjects() }.message)
    }

    @Test
    fun `network failures map to a network error`() = runTest {
        server.enqueue(MockResponse.Builder().onRequestStart(SocketEffect.CloseSocket()).build())
        val error = expectFailure { repo.subjects() }
        assertEquals(AppException.Kind.Network, error.kind)
    }

    @Test
    fun `malformed bodies map to an unexpected error`() = runTest {
        json(200, """{"subjects":"nope"}""")
        assertEquals(AppException.Kind.Unexpected, expectFailure { repo.subjects() }.kind)
    }

    // Requests and parsing

    @Test
    fun `assignment filters are sent as query params and blank search is omitted`() = runTest {
        json(200, """{"assignments":[${assignmentJson()}]}""")
        val list = repo.assignments(subjectId = "s1", status = AssignmentStatus.Open, query = "  essay ")
        assertEquals(1, list.size)
        assertEquals(Priority.High, list.single().priority)
        assertEquals(SubjectColor.Sky, list.single().subject.color)
        val url = server.takeRequest().url
        assertEquals("s1", url.queryParameter("subjectId"))
        assertEquals("open", url.queryParameter("status"))
        assertEquals("essay", url.queryParameter("q"))

        json(200, """{"assignments":[]}""")
        repo.assignments(query = "   ")
        val second = server.takeRequest().url
        assertNull(second.queryParameter("q"))
        assertNull(second.queryParameter("subjectId"))
        assertEquals("all", second.queryParameter("status"))
    }

    @Test
    fun `marking done sends the local date and reopening sends an explicit null`() = runTest {
        json(200, """{"assignment":${assignmentJson("2026-10-14")}}""")
        val done = repo.setDone("a1", done = true)
        assertTrue(done.isDone)
        assertEquals("""{"completedOn":"2026-10-14"}""", server.takeRequest().body!!.utf8())

        json(200, """{"assignment":${assignmentJson()}}""")
        repo.setDone("a1", done = false)
        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals("/api/assignments/a1", request.url.encodedPath)
        assertEquals("""{"completedOn":null}""", request.body!!.utf8())

        json(200, """{"assignment":${assignmentJson("2026-10-02")}}""")
        repo.setCompletedOn("a1", LocalDate.of(2026, 10, 2))
        assertEquals("""{"completedOn":"2026-10-02"}""", server.takeRequest().body!!.utf8())
    }

    @Test
    fun `focus sessions record the local day of the start time`() = runTest {
        json(201, """{"session":{"id":"f1","subject":null,"startedAt":"2026-10-13T19:00:00Z","durationSeconds":1500,"day":"2026-10-14"}}""")
        // 19:00 UTC on the 13th is 00:30 on the 14th in Kolkata.
        repo.logFocusSession("f1", null, Instant.parse("2026-10-13T19:00:00Z"), 1500)
        val body = server.takeRequest().body!!.utf8()
        assertTrue(body, body.contains("\"id\":\"f1\""))
        assertTrue(body, body.contains("\"day\":\"2026-10-14\""))
        assertTrue(body, body.contains("\"durationSeconds\":1500"))
    }

    @Test
    fun `insights are requested for the week with the local today`() = runTest {
        json(
            200,
            """{"weekStart":"2026-10-12","weekEnd":"2026-10-18","totalSeconds":5400,"sessionCount":3,
                "days":[{"date":"2026-10-12","seconds":5400}],
                "bySubject":[{"subject":$subjectJson,"seconds":3600},{"subject":null,"seconds":1800}],
                "assignmentsCompleted":2,"streak":{"current":3,"longest":5},"dailyGoalMinutes":90}""",
        )
        val insights = repo.insights(LocalDate.of(2026, 10, 12))
        assertEquals(5400L, insights.totalSeconds)
        assertEquals(3, insights.currentStreak)
        assertNull(insights.bySubject[1].subject)
        val url = server.takeRequest().url
        assertEquals("2026-10-12", url.queryParameter("week"))
        assertEquals("2026-10-14", url.queryParameter("today"))
    }

    @Test
    fun `subject requests are trimmed`() = runTest {
        json(201, """{"subject":$subjectJson}""")
        repo.addSubject(SubjectDraft("  Maths ", SubjectColor.Sky, SubjectIcon.Calculator))
        assertEquals("""{"name":"Maths","color":"sky","icon":"calculator"}""", server.takeRequest().body!!.utf8())
    }

    @Test
    fun `profile update only sends provided fields`() = runTest {
        json(200, """{"profile":{"id":"u","name":"Ayush","email":"a@b.co","dailyFocusGoalMinutes":45,"createdAt":"2026-10-01T00:00:00Z"}}""")
        val profile = repo.updateProfile(dailyGoalMinutes = 45)
        assertEquals(45, profile.dailyGoalMinutes)
        assertEquals("""{"dailyFocusGoalMinutes":45}""", server.takeRequest().body!!.utf8())
    }

    // Revision

    @Test
    fun `writes bump the revision but reads and failures do not`() = runTest {
        val start = repo.revision.value
        json(200, """{"subjects":[]}""")
        repo.subjects()
        assertEquals(start, repo.revision.value)

        json(201, """{"assignment":${assignmentJson()}}""")
        repo.addAssignment(AssignmentDraft("Set 1", "s1", LocalDate.of(2026, 10, 20), Priority.High, ""))
        assertEquals(start + 1, repo.revision.value)

        json(400, """{"error":{"code":"bad_request","message":"title: Too big"}}""")
        expectFailure { repo.updateAssignment("a1", AssignmentDraft("x", "s1", LocalDate.of(2026, 10, 20), Priority.Low, "")) }
        assertEquals(start + 1, repo.revision.value)

        server.enqueue(MockResponse.Builder().code(204).build())
        repo.deleteAssignment("a1")
        assertEquals(start + 2, repo.revision.value)

        json(404, """{"error":{"code":"not_found","message":"Subject not found"}}""")
        expectFailure { repo.deleteSubject("s1") }
        assertEquals(start + 2, repo.revision.value)
    }
}
