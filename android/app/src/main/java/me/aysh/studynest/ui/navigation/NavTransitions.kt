package me.aysh.studynest.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.Scene

typealias SceneTransition = AnimatedContentTransitionScope<Scene<NavKey>>.() -> ContentTransform

object NavTransitions {

    private const val DURATION = 320

    val push: SceneTransition = {
        (slideInHorizontally(tween(DURATION)) { it / 4 } + fadeIn(tween(DURATION))) togetherWith
            (slideOutHorizontally(tween(DURATION)) { -it / 10 } + fadeOut(tween(DURATION / 2)))
    }

    val pop: SceneTransition = {
        (slideInHorizontally(tween(DURATION)) { -it / 10 } + fadeIn(tween(DURATION))) togetherWith
            (slideOutHorizontally(tween(DURATION)) { it / 4 } + fadeOut(tween(DURATION / 2)))
    }

    val predictivePop: AnimatedContentTransitionScope<Scene<NavKey>>.(Int) -> ContentTransform = { pop() }
}
