package me.aysh.studynest.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object Welcome : NavKey

@Serializable
data object SignIn : NavKey

@Serializable
data object SignUp : NavKey

/** The signed-in shell with the bottom navigation bar. */
@Serializable
data object Main : NavKey

@Serializable
data object Subjects : NavKey

/** Adds a new assignment when [assignmentId] is null (optionally for [subjectId]), otherwise edits it. */
@Serializable
data class AssignmentEditor(val assignmentId: String? = null, val subjectId: String? = null) : NavKey

enum class Tab { Today, Assignments, Focus, Insights, Profile }
