package me.aysh.studynest.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.aysh.studynest.R
import me.aysh.studynest.ui.appViewModel
import me.aysh.studynest.ui.components.BrandMark
import me.aysh.studynest.ui.components.NestBanner
import me.aysh.studynest.ui.components.NestIconButton
import me.aysh.studynest.ui.components.NestTextButton
import me.aysh.studynest.ui.components.NestTextField
import me.aysh.studynest.ui.components.PrimaryButton
import me.aysh.studynest.ui.components.Reveal
import me.aysh.studynest.ui.components.SoftBlob
import me.aysh.studynest.ui.components.Sparkle
import me.aysh.studynest.ui.theme.NestShapes
import me.aysh.studynest.ui.theme.StudyNestTheme
import me.aysh.studynest.util.Validation

@Composable
fun AuthScreen(mode: AuthMode, onBack: () -> Unit, onSwitchMode: () -> Unit) {
    val vm = appViewModel(key = "auth-${mode.name}") { c -> AuthViewModel(mode, c.authRepository) }
    val state by vm.state.collectAsStateWithLifecycle()
    AuthContent(
        state = state,
        onBack = onBack,
        onSwitchMode = onSwitchMode,
        onNameChange = vm::onNameChange,
        onEmailChange = vm::onEmailChange,
        onPasswordChange = vm::onPasswordChange,
        onTogglePassword = vm::togglePasswordVisibility,
        onSubmit = vm::submit,
    )
}

@Composable
private fun AuthContent(
    state: AuthUiState,
    onBack: () -> Unit,
    onSwitchMode: () -> Unit,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePassword: () -> Unit,
    onSubmit: () -> Unit,
) {
    val c = StudyNestTheme.colors
    val signUp = state.mode == AuthMode.SignUp
    val focusManager = LocalFocusManager.current
    val accent = if (signUp) c.lilac else c.sky
    val busy = state.isSubmitting
    // Keep the strength meter visible above the keyboard once it appears.
    val scroll = rememberScrollState()
    val passwordBlock = remember { BringIntoViewRequester() }
    val showMeter = signUp && state.password.isNotEmpty()
    LaunchedEffect(showMeter) {
        if (showMeter) {
            delay(220)
            passwordBlock.bringIntoView()
        }
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(c.canvas)
            .testTag("auth_screen"),
    ) {
        SoftBlob(c.wash(accent.fill), Modifier.size(340.dp).offset(x = maxWidth - 210.dp, y = (-110).dp), seed = if (signUp) 2 else 4)
        SoftBlob(
            c.wash((if (signUp) c.peach else c.mint).fill, 0.6f),
            Modifier.size(220.dp).offset(x = (-120).dp, y = maxHeight - 170.dp),
            seed = 6,
        )

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
                .verticalScroll(scroll)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .widthIn(max = 480.dp)
                .align(Alignment.TopCenter),
        ) {
            Spacer(Modifier.height(8.dp))
            NestIconButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.action_back),
                onClick = onBack,
                modifier = Modifier
                    .offset(x = (-4).dp)
                    .testTag("auth_back"),
            )
            Spacer(Modifier.height(20.dp))

            Reveal(0) { HeaderSticker(accentFill = accent.fill, accentLip = accent.lip, tilt = if (signUp) -6f else 5f) }
            Spacer(Modifier.height(20.dp))
            Reveal(1) {
                Column {
                    Text(
                        stringResource(if (signUp) R.string.auth_sign_up_title else R.string.auth_sign_in_title),
                        style = MaterialTheme.typography.headlineLarge,
                        color = c.ink,
                        modifier = Modifier.semantics { heading() },
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(if (signUp) R.string.auth_sign_up_subtitle else R.string.auth_sign_in_subtitle),
                        style = MaterialTheme.typography.bodyLarge,
                        color = c.inkMuted,
                    )
                }
            }
            Spacer(Modifier.height(28.dp))

            AnimatedVisibility(
                visible = state.formError != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                NestBanner(
                    message = state.formError.orEmpty(),
                    modifier = Modifier
                        .padding(bottom = 20.dp)
                        .testTag("auth_form_error"),
                )
            }

            Reveal(2) {
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    if (signUp) {
                        NestTextField(
                            value = state.name,
                            onValueChange = onNameChange,
                            label = stringResource(R.string.auth_name),
                            placeholder = stringResource(R.string.auth_name_placeholder),
                            leadingIcon = Icons.Rounded.Face,
                            error = state.nameError,
                            enabled = !busy,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next,
                            ),
                            modifier = Modifier
                                .testTag("auth_name")
                                .semantics { contentType = ContentType.PersonFullName },
                        )
                    }
                    NestTextField(
                        value = state.email,
                        onValueChange = onEmailChange,
                        label = stringResource(R.string.auth_email),
                        placeholder = stringResource(R.string.auth_email_placeholder),
                        leadingIcon = Icons.Rounded.AlternateEmail,
                        error = state.emailError,
                        enabled = !busy,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            autoCorrectEnabled = false,
                            imeAction = ImeAction.Next,
                        ),
                        modifier = Modifier
                            .testTag("auth_email")
                            .semantics {
                                contentType = if (signUp) ContentType.EmailAddress + ContentType.NewUsername
                                else ContentType.EmailAddress + ContentType.Username
                            },
                    )
                    Column(Modifier.bringIntoViewRequester(passwordBlock)) {
                        NestTextField(
                            value = state.password,
                            onValueChange = onPasswordChange,
                            label = stringResource(R.string.auth_password),
                            placeholder = stringResource(
                                if (signUp) R.string.auth_password_placeholder_new else R.string.auth_password_placeholder,
                            ),
                            leadingIcon = Icons.Rounded.Lock,
                            error = state.passwordError,
                            enabled = !busy,
                            visualTransformation = if (state.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                autoCorrectEnabled = false,
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(onDone = {
                                if (state.isValid) focusManager.clearFocus()
                                onSubmit()
                            }),
                            trailing = {
                                NestIconButton(
                                    icon = if (state.passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = stringResource(
                                        if (state.passwordVisible) R.string.auth_hide_password else R.string.auth_show_password,
                                    ),
                                    onClick = onTogglePassword,
                                    filled = false,
                                    tint = c.inkMuted,
                                    modifier = Modifier.testTag("auth_password_toggle"),
                                )
                            },
                            modifier = Modifier
                                .testTag("auth_password")
                                .semantics { contentType = if (signUp) ContentType.NewPassword else ContentType.Password },
                        )
                        if (signUp) {
                            AnimatedVisibility(
                                visible = state.password.isNotEmpty(),
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically(),
                            ) {
                                StrengthMeter(
                                    password = state.password,
                                    score = state.passwordStrength,
                                    modifier = Modifier.padding(top = 14.dp, start = 4.dp, end = 4.dp),
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
            Reveal(3) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PrimaryButton(
                        text = stringResource(if (signUp) R.string.auth_sign_up_submit else R.string.auth_sign_in_submit),
                        onClick = {
                            if (state.isValid) focusManager.clearFocus()
                            onSubmit()
                        },
                        loading = busy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_submit"),
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(if (signUp) R.string.auth_have_account else R.string.auth_no_account),
                            style = MaterialTheme.typography.bodyMedium,
                            color = c.inkMuted,
                        )
                        NestTextButton(
                            text = stringResource(if (signUp) R.string.auth_switch_to_sign_in else R.string.auth_switch_to_sign_up),
                            onClick = onSwitchMode,
                            enabled = !busy,
                            contentPadding = PaddingValues(horizontal = 10.dp),
                            modifier = Modifier.testTag("auth_switch_mode"),
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
        // Soft canvas scrim under the status bar so scrolled content doesn't collide with system icons.
        Box(
            Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars.add(WindowInsets(top = 24.dp)))
                .graphicsLayer { alpha = (scroll.value / 48f).coerceIn(0f, 1f) }
                .background(Brush.verticalGradient(listOf(c.canvas, c.canvas.copy(alpha = 0.9f), c.canvas.copy(alpha = 0f)))),
        )
    }
}

/** The tilted "sticker" with the StudyNest mark that crowns the auth header. */
@Composable
private fun HeaderSticker(accentFill: Color, accentLip: Color, tilt: Float) {
    val c = StudyNestTheme.colors
    Box(Modifier.size(width = 92.dp, height = 84.dp).clearAndSetSemantics { }) {
        Box(Modifier.rotate(tilt).size(76.dp).offset(y = 5.dp).clip(NestShapes.Stamp).background(accentLip))
        Box(
            Modifier
                .rotate(tilt)
                .size(76.dp)
                .clip(NestShapes.Stamp)
                .background(accentFill),
            contentAlignment = Alignment.Center,
        ) {
            BrandMark(Modifier.size(58.dp))
        }
        Sparkle(c.butter.lip, Modifier.size(20.dp).align(Alignment.TopEnd), delayMillis = 300)
    }
}

/** Four animated segments + label. Score from [Validation.passwordStrength]; under 8 chars reads "Too short". */
@Composable
private fun StrengthMeter(password: String, score: Int, modifier: Modifier = Modifier) {
    val c = StudyNestTheme.colors
    val tooShort = password.length < Validation.MIN_PASSWORD
    val label = stringResource(
        when {
            tooShort -> R.string.auth_strength_too_short
            score <= 1 -> R.string.auth_strength_weak
            score == 2 -> R.string.auth_strength_fair
            score == 3 -> R.string.auth_strength_good
            else -> R.string.auth_strength_strong
        },
    )
    val filled = if (tooShort) 1 else score.coerceIn(1, 4)
    val tone = when {
        tooShort -> c.overdueAccent
        filled == 1 -> c.peach.ink
        filled == 2 -> if (c.isDark) c.butter.fill else c.butter.lip
        filled == 3 -> c.mint.lip
        else -> if (c.isDark) c.mint.fill else c.success
    }
    val labelColor = when {
        tooShort -> c.overdue
        c.isDark -> c.ink
        filled == 1 -> c.peach.ink
        filled == 2 -> c.butter.ink
        else -> c.success
    }
    val description = stringResource(R.string.auth_strength_description, label)
    Column(modifier.clearAndSetSemantics { contentDescription = description }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(4) { i ->
                    val on = i < filled
                    val color by animateColorAsState(if (on) tone else c.hairline, label = "seg$i")
                    val grow by animateFloatAsState(
                        if (on) 1f else 0.85f,
                        spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMediumLow),
                        label = "segGrow$i",
                    )
                    Box(
                        Modifier
                            .weight(1f)
                            .height(8.dp)
                            .graphicsLayer { scaleY = grow }
                            .clip(NestShapes.Pill)
                            .background(color),
                    )
                }
            }
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = labelColor,
                modifier = Modifier.padding(start = 12.dp).widthIn(min = 64.dp),
                textAlign = TextAlign.End,
            )
        }
        if (filled < 3) {
            Text(
                stringResource(R.string.auth_strength_hint),
                style = MaterialTheme.typography.bodySmall,
                color = c.inkMuted,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
