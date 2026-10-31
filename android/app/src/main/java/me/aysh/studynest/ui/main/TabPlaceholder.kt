package me.aysh.studynest.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import me.aysh.studynest.R
import me.aysh.studynest.ui.components.NestIllustration
import me.aysh.studynest.ui.theme.StudyNestTheme

/**
 * TEMPORARY centred placeholder used by the tab stubs until their real screens land.
 * Delete this file once every tab is implemented.
 */
@Composable
internal fun TabPlaceholder(title: String, tag: String) {
    val c = StudyNestTheme.colors
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(LocalBottomBarPadding.current)
            .padding(horizontal = 32.dp)
            .testTag(tag),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        NestIllustration(Modifier.size(width = 200.dp, height = 170.dp), books = 1, showPencil = false)
        Text(title, style = MaterialTheme.typography.headlineMedium, color = c.ink)
        Text(
            stringResource(R.string.placeholder_coming_soon),
            style = MaterialTheme.typography.bodyLarge,
            color = c.inkMuted,
            textAlign = TextAlign.Center,
        )
    }
}
