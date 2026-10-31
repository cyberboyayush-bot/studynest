package me.aysh.studynest.data.remote

import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

val AppJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    coerceInputValues = true
}

/**
 * Attaches the bearer token to app requests and reports a 401 from the app API
 * (not from sign-in) so the session can be cleared.
 */
class AuthInterceptor(
    private val tokenProvider: () -> String?,
    private val onUnauthorized: (token: String) -> Unit,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val token = tokenProvider()
        val authed = if (token != null) {
            request.newBuilder().header("Authorization", "Bearer $token").build()
        } else {
            request
        }
        val response = chain.proceed(authed)
        val isAuthEndpoint = request.url.encodedPath.startsWith("/api/auth/")
        if (response.code == 401 && token != null && !isAuthEndpoint) onUnauthorized(token)
        return response
    }
}

fun createApi(
    baseUrl: String,
    tokenProvider: () -> String?,
    onUnauthorized: (token: String) -> Unit,
    json: Json = AppJson,
): StudyNestApi {
    val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(AuthInterceptor(tokenProvider, onUnauthorized))
        .build()

    return Retrofit.Builder()
        .baseUrl(baseUrl.toHttpUrl())
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(StudyNestApi::class.java)
}
