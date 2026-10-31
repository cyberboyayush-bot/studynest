package me.aysh.studynest.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import me.aysh.studynest.R
import me.aysh.studynest.ui.components.BrandMark
import me.aysh.studynest.ui.components.NestIllustration
import me.aysh.studynest.ui.components.PrimaryButton
import me.aysh.studynest.ui.components.Reveal
import me.aysh.studynest.ui.components.SecondaryButton
import me.aysh.studynest.ui.components.SoftBlob
import me.aysh.studynest.ui.components.Sparkle
import me.aysh.studynest.ui.theme.NestShapes
import me.aysh.studynest.ui.theme.StudyNestTheme

@Composable
fun WelcomeScreen(onGetStarted: () -> Unit, onSignIn: () -> Unit) {
    val c = StudyNestTheme.colors
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(c.canvas)
            .testTag("welcome_screen"),
    ) {
        val wide = maxWidth > 640.dp && maxHeight < 560.dp
        // Atmosphere: soft pastel blobs drifting behind everything.
        SoftBlob(c.wash(c.lilac.fill), Modifier.size(420.dp).offset(x = maxWidth - 250.dp, y = (-120).dp), seed = 1)
        SoftBlob(c.wash(c.peach.fill, 0.7f), Modifier.size(260.dp).offset(x = (-130).dp, y = maxHeight * 0.42f), seed = 3)
        SoftBlob(c.wash(c.mint.fill, 0.7f), Modifier.size(200.dp).offset(x = maxWidth - 90.dp, y = maxHeight - 260.dp), seed = 5)

        if (wide) {
            Row(
                Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(horizontal = 32.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Hero(Modifier.weight(1f).fillMaxHeight())
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 24.dp),
                ) {
                    Wordmark()
                    Spacer(Modifier.height(24.dp))
                    Copy()
                    Spacer(Modifier.height(28.dp))
                    Actions(onGetStarted, onSignIn)
                }
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(horizontal = 24.dp)
                    .widthIn(max = 520.dp)
                    .align(Alignment.TopCenter),
            ) {
                Reveal(0) { Wordmark(Modifier.padding(top = 16.dp)) }
                Hero(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )
                Copy()
                Spacer(Modifier.height(32.dp))
                Actions(onGetStarted, onSignIn)
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun Wordmark(modifier: Modifier = Modifier) {
    val c = StudyNestTheme.colors
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(44.dp)
                .clip(NestShapes.Stamp)
                .background(c.surfaceRaised),
            contentAlignment = Alignment.Center,
        ) {
            BrandMark(Modifier.size(34.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(
            stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            color = c.ink,
        )
    }
}

@Composable
private fun Hero(modifier: Modifier) {
    val c = StudyNestTheme.colors
    val description = stringResource(R.string.welcome_illustration)
    Box(modifier, contentAlignment = Alignment.Center) {
        Sparkle(c.butter.lip, Modifier.size(22.dp).align(Alignment.TopStart).offset(x = 12.dp, y = 28.dp), delayMillis = 200)
        Sparkle(c.rose.lip, Modifier.size(14.dp).align(Alignment.BottomEnd).offset(x = (-24).dp, y = (-36).dp), delayMillis = 700)
        NestIllustration(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(vertical = 8.dp)
                .semantics { contentDescription = description },
        )
    }
}

@Composable
private fun Copy() {
    val c = StudyNestTheme.colors
    Reveal(1) {
        Text(
            stringResource(R.string.welcome_kicker),
            style = MaterialTheme.typography.labelMedium,
            color = c.onPastel,
            modifier = Modifier
                .clip(NestShapes.Pill)
                .background(c.butter.fill)
                .padding(horizontal = 14.dp, vertical = 7.dp),
        )
    }
    Spacer(Modifier.height(16.dp))
    Reveal(2) {
        val line1 = stringResource(R.string.welcome_headline_1)
        val line2 = stringResource(R.string.welcome_headline_2)
        Text(
            buildAnnotatedString {
                append(line1)
                append("\n")
                withStyle(SpanStyle(color = c.primaryText)) { append(line2) }
            },
            style = MaterialTheme.typography.displaySmall,
            color = c.ink,
            modifier = Modifier.semantics { heading() },
        )
    }
    Spacer(Modifier.height(12.dp))
    Reveal(3) {
        Text(
            stringResource(R.string.welcome_body),
            style = MaterialTheme.typography.bodyLarge,
            color = c.inkMuted,
        )
    }
}

@Composable
private fun Actions(onGetStarted: () -> Unit, onSignIn: () -> Unit) {
    Reveal(4) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PrimaryButton(
                text = stringResource(R.string.welcome_get_started),
                onClick = onGetStarted,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("welcome_get_started"),
            )
            SecondaryButton(
                text = stringResource(R.string.welcome_sign_in),
                onClick = onSignIn,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("welcome_sign_in"),
            )
        }
    }
}
