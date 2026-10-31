package me.aysh.studynest.data.remote

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/** A failure with a message that is safe and friendly to show to the user. */
class AppException(
    override val message: String,
    val kind: Kind,
    val status: Int? = null,
    cause: Throwable? = null,
) : Exception(message, cause) {

    enum class Kind { Network, Unauthorized, InvalidCredentials, NotFound, Conflict, RateLimited, Validation, Server, Unexpected }
}

internal object ApiErrors {

    fun fromStatus(status: Int, rawBody: String?, json: Json): AppException {
        val body = rawBody?.let { runCatching { json.decodeFromString<ErrorBody>(it) }.getOrNull() }
        val code = body?.error?.code ?: body?.code
        val serverMessage = (body?.error?.message ?: body?.message)?.let(::humanize)

        fun error(kind: AppException.Kind, message: String) = AppException(message, kind, status)

        return when {
            status == 401 && code == "INVALID_EMAIL_OR_PASSWORD" ->
                error(AppException.Kind.InvalidCredentials, "Incorrect email or password")
            status == 401 -> error(AppException.Kind.Unauthorized, "Your session has expired. Please sign in again.")
            status == 429 -> error(AppException.Kind.RateLimited, "Too many attempts. Please wait a minute and try again.")
            code?.contains("ALREADY_EXISTS") == true ->
                error(AppException.Kind.Conflict, "An account with this email already exists")
            code == "INVALID_EMAIL" -> error(AppException.Kind.Validation, "Please enter a valid email address")
            code == "PASSWORD_TOO_SHORT" -> error(AppException.Kind.Validation, "Password must be at least 8 characters")
            code == "PASSWORD_TOO_LONG" -> error(AppException.Kind.Validation, "Password is too long")
            status == 404 -> error(AppException.Kind.NotFound, "That item no longer exists")
            status == 409 -> error(AppException.Kind.Conflict, serverMessage ?: "That already exists")
            status == 413 -> error(AppException.Kind.Validation, "That's too much text to save")
            status >= 500 -> error(AppException.Kind.Server, "The server ran into a problem. Please try again.")
            status in 400..499 -> error(AppException.Kind.Validation, serverMessage ?: "Please check what you entered")
            else -> error(AppException.Kind.Unexpected, "Something went wrong (error $status)")
        }
    }

    fun from(throwable: Throwable, json: Json): AppException = when (throwable) {
        is AppException -> throwable
        is HttpException -> fromStatus(throwable.code(), throwable.response()?.errorBody()?.string(), json)
        is IOException -> AppException(
            "Can't reach StudyNest. Check your connection and try again.",
            AppException.Kind.Network,
            cause = throwable,
        )
        is SerializationException -> AppException(
            "Received an unexpected response from the server.",
            AppException.Kind.Unexpected,
            cause = throwable,
        )
        else -> AppException("Something went wrong. Please try again.", AppException.Kind.Unexpected, cause = throwable)
    }

    /** Server validation messages look like "title: Too long"; drop the field prefix for display. */
    private fun humanize(message: String): String =
        message.substringAfter(": ", message).replaceFirstChar { it.uppercase() }
}

/** Runs a network call and converts every failure into an [AppException]. */
internal suspend fun <T> apiCall(json: Json, block: suspend () -> T): T =
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        throw ApiErrors.from(e, json)
    }

/** Unwraps a raw [Response], throwing an [AppException] for non-2xx responses. */
internal fun <T> Response<T>.requireSuccess(json: Json): Response<T> {
    if (isSuccessful) return this
    throw ApiErrors.fromStatus(code(), errorBody()?.string(), json)
}
