package me.aysh.studynest.util

object Validation {

    const val MIN_PASSWORD = 8
    const val MAX_PASSWORD = 128
    const val MAX_NAME = 60
    const val MAX_SUBJECT_NAME = 40
    const val MAX_TITLE = 100
    const val MAX_NOTES = 500
    const val MIN_GOAL_MINUTES = 5
    const val MAX_GOAL_MINUTES = 720
    const val MIN_CUSTOM_FOCUS_MINUTES = 1
    const val MAX_CUSTOM_FOCUS_MINUTES = 240

    private val EMAIL = Regex("""^[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}$""")

    fun name(value: String): String? = when {
        value.isBlank() -> "Enter your name"
        value.trim().length > MAX_NAME -> "Name is too long"
        else -> null
    }

    fun email(value: String): String? = when {
        value.isBlank() -> "Enter your email"
        !EMAIL.matches(value.trim()) -> "Enter a valid email address"
        else -> null
    }

    /** Sign-in only checks presence so we don't leak password rules for existing accounts. */
    fun passwordPresent(value: String): String? = if (value.isEmpty()) "Enter your password" else null

    fun newPassword(value: String): String? = when {
        value.isEmpty() -> "Choose a password"
        value.length < MIN_PASSWORD -> "Use at least $MIN_PASSWORD characters"
        value.length > MAX_PASSWORD -> "Password is too long"
        else -> null
    }

    /** 0–4 score used for the password strength meter. */
    fun passwordStrength(value: String): Int {
        if (value.length < MIN_PASSWORD) return if (value.isEmpty()) 0 else 1
        var score = 1
        if (value.length >= 12) score++
        if (value.any(Char::isDigit) && value.any(Char::isLetter)) score++
        if (value.any { !it.isLetterOrDigit() } || (value.any(Char::isUpperCase) && value.any(Char::isLowerCase))) score++
        return score.coerceAtMost(4)
    }

    fun subjectName(value: String): String? = when {
        value.isBlank() -> "Give the subject a name"
        value.trim().length > MAX_SUBJECT_NAME -> "Keep it under $MAX_SUBJECT_NAME characters"
        else -> null
    }

    fun title(value: String): String? = when {
        value.isBlank() -> "Give it a title"
        value.trim().length > MAX_TITLE -> "Keep it under $MAX_TITLE characters"
        else -> null
    }

    fun notes(value: String): String? =
        if (value.trim().length > MAX_NOTES) "Notes can be up to $MAX_NOTES characters" else null

    /** Parses a whole number of minutes within [min]..[max], or null if it isn't one. */
    fun minutes(input: String, min: Int, max: Int): Int? =
        input.trim().takeIf { it.isNotEmpty() && it.all(Char::isDigit) && it.length <= 4 }
            ?.toInt()
            ?.takeIf { it in min..max }

    fun dailyGoal(input: String): String? = when {
        input.isBlank() -> "Enter a number of minutes"
        minutes(input, MIN_GOAL_MINUTES, MAX_GOAL_MINUTES) == null ->
            "Choose between $MIN_GOAL_MINUTES and $MAX_GOAL_MINUTES minutes"
        else -> null
    }

    fun customFocus(input: String): String? = when {
        input.isBlank() -> "Enter a number of minutes"
        minutes(input, MIN_CUSTOM_FOCUS_MINUTES, MAX_CUSTOM_FOCUS_MINUTES) == null ->
            "Choose between $MIN_CUSTOM_FOCUS_MINUTES and $MAX_CUSTOM_FOCUS_MINUTES minutes"
        else -> null
    }
}
