package me.aysh.studynest.data.repository

import kotlinx.serialization.json.Json
import me.aysh.studynest.data.remote.AppException
import me.aysh.studynest.data.remote.StudyNestApi
import me.aysh.studynest.data.remote.SignInRequest
import me.aysh.studynest.data.remote.SignUpRequest
import me.aysh.studynest.data.remote.apiCall
import me.aysh.studynest.data.remote.requireSuccess
import retrofit2.Response

/** Abstraction over where the session token is kept, so the repository is testable on the JVM. */
interface TokenStore {
    suspend fun save(token: String)
    suspend fun clear()
}

class AuthRepository(
    private val api: StudyNestApi,
    private val tokens: TokenStore,
    private val json: Json,
) {

    suspend fun signUp(name: String, email: String, password: String) {
        val token = apiCall(json) {
            api.signUp(SignUpRequest(name.trim(), email.trim().lowercase(), password)).requireToken()
        }
        tokens.save(token)
    }

    suspend fun signIn(email: String, password: String) {
        val token = apiCall(json) {
            api.signIn(SignInRequest(email.trim().lowercase(), password)).requireToken()
        }
        tokens.save(token)
    }

    /** Revokes the session on the server when possible, and always forgets it locally. */
    suspend fun signOut() {
        try {
            apiCall(json) { api.signOut() }
        } catch (_: AppException) {
            // Offline or already expired: clearing locally is still the right outcome.
        } finally {
            tokens.clear()
        }
    }

    /** Only the session token header matters; the response body is ignored. */
    private fun Response<Unit>.requireToken(): String {
        requireSuccess(json)
        return headers()[AUTH_TOKEN_HEADER]?.takeIf { it.isNotBlank() }
            ?: throw AppException("The server didn't return a session. Please try again.", AppException.Kind.Unexpected)
    }

    private companion object {
        const val AUTH_TOKEN_HEADER = "set-auth-token"
    }
}
