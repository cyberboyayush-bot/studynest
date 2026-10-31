package me.aysh.studynest.ui.profile

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.aysh.studynest.BuildConfig
import me.aysh.studynest.R
import me.aysh.studynest.data.model.Profile
import me.aysh.studynest.ui.Loadable
import me.aysh.studynest.ui.appViewModel
import me.aysh.studynest.ui.components.BannerTone
import me.aysh.studynest.ui.components.BrandMark
import me.aysh.studynest.ui.components.ErrorState
import me.aysh.studynest.ui.components.NestAlertDialog
import me.aysh.studynest.ui.components.NestBanner
import me.aysh.studynest.ui.components.NestButton
import me.aysh.studynest.ui.components.NestButtonStyle
import me.aysh.studynest.ui.components.NestChoiceChip
import me.aysh.studynest.ui.components.NestTextField
import me.aysh.studynest.ui.components.PrimaryButton
import me.aysh.studynest.ui.components.Reveal
import me.aysh.studynest.ui.components.SectionHeader
import me.aysh.studynest.ui.components.SkeletonBlock
import me.aysh.studynest.ui.components.SoftBlob
import me.aysh.studynest.ui.components.Sparkle
import me.aysh.studynest.ui.components.SquishySurface
import me.aysh.studynest.ui.components.StatefulNestSheet
import me.aysh.studynest.ui.components.drawBlob
import me.aysh.studynest.ui.main.LocalBottomBarPadding
import me.aysh.studynest.ui.navigation.LocalAppMessenger
import me.aysh.studynest.ui.theme.Fredoka
import me.aysh.studynest.ui.theme.NestShapes
import me.aysh.studynest.ui.theme.Pastel
import me.aysh.studynest.ui.theme.StudyNestTheme
import me.aysh.studynest.util.Durations
import me.aysh.studynest.util.Validation
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.PI

@Composable
fun ProfileScreen(onOpenSubjects: () -> Unit) {
    val vm = appViewModel { c -> ProfileViewModel(c.studyRepository, c.authRepository) }
    val state by vm.state.collectAsStateWithLifecycle()
    val messenger = LocalAppMessenger.current
    LaunchedEffect(vm) { vm.messages.collect { messenger.show(it) } }

    ProfileContent(
        state = state,
        onRetry = vm::retry,
        onOpenGoal = vm::openGoalSheet,
        onOpenSubjects = onOpenSubjects,
        onRequestSignOut = vm::requestSignOut,
    )

    StatefulNestSheet(
        value = state.goalSheet,
        onDismissRequest = vm::dismissGoalSheet,
        modifier = Modifier.testTag("goal_sheet"),
        title = stringResource(R.string.profile_goal_sheet_title),
        lockOpen = state.goalSheet?.isSaving == true,
    ) { sheet ->
        GoalSheetContent(sheet, onInput = vm::onGoalInput, onSave = vm::saveGoal)
    }

    if (state.confirmSignOut) {
        NestAlertDialog(
            title = stringResource(R.string.profile_sign_out_title),
            message = stringResource(R.string.profile_sign_out_message),
            confirmLabel = stringResource(R.string.profile_sign_out),
            onConfirm = vm::signOut,
            onDismiss = vm::cancelSignOut,
            confirmTag = "confirm_sign_out",
            destructive = true,
            loading = state.isSigningOut,
            icon = Icons.AutoMirrored.Rounded.Logout,
        )
    }
}

@Composable
private fun ProfileContent(
    state: ProfileUiState,
    onRetry: () -> Unit,
    onOpenGoal: () -> Unit,
    onOpenSubjects: () -> Unit,
    onRequestSignOut: () -> Unit,
) {
    val c = StudyNestTheme.colors
    val bar = LocalBottomBarPadding.current
    val profile = state.profile.valueOrNull
    Box(
        Modifier
            .fillMaxSize()
            .background(c.canvas)
            .testTag("profile_screen"),
    ) {
        SoftBlob(c.wash(c.peach.fill, 0.6f), Modifier.size(320.dp).offset(x = 200.dp, y = (-130).dp), seed = 5)
        SoftBlob(c.wash(c.mint.fill, 0.45f), Modifier.size(200.dp).offset(x = (-110).dp, y = 230.dp), seed = 1)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(bar)
                .padding(horizontal = 20.dp),
        ) {
            Text(
                stringResource(R.string.profile_title),
                style = MaterialTheme.typography.headlineMedium,
                color = c.ink,
                modifier = Modifier
                    .padding(start = 4.dp, top = 16.dp)
                    .semantics { heading() },
            )
            Spacer(Modifier.height(8.dp))
            AnimatedContent(
                targetState = state.profile,
                contentKey = { it::class },
                transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(150)) },
                label = "profileHero",
            ) { loadable ->
                when (loadable) {
                    Loadable.Loading -> HeroSkeleton()
                    is Loadable.Failed -> ErrorState(loadable.message, onRetry = onRetry)
                    is Loadable.Ready -> Hero(loadable.value)
                }
            }
            Spacer(Modifier.height(24.dp))
            Reveal(1) {
                Column {
                    SectionHeader(stringResource(R.string.profile_section_study), accent = c.sky.lip)
                    Spacer(Modifier.height(12.dp))
                    Group {
                        SettingsRow(
                            icon = Icons.Rounded.Timer,
                            pastel = c.lilac,
                            title = stringResource(R.string.profile_goal),
                            subtitle = stringResource(R.string.profile_goal_hint),
                            value = profile?.let { Durations.minutes(it.dailyGoalMinutes) },
                            onClick = onOpenGoal,
                            enabled = profile != null,
                            modifier = Modifier.testTag("profile_goal"),
                        )
                        HorizontalDivider(Modifier.padding(start = 76.dp, end = 16.dp), thickness = 1.dp, color = c.hairline)
                        SettingsRow(
                            icon = Icons.Rounded.Category,
                            pastel = c.mint,
                            title = stringResource(R.string.profile_subjects),
                            subtitle = stringResource(R.string.profile_subjects_hint),
                            value = null,
                            onClick = onOpenSubjects,
                            modifier = Modifier.testTag("profile_subjects"),
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            Reveal(2) {
                Column {
                    SectionHeader(stringResource(R.string.profile_section_about), accent = c.butter.lip)
                    Spacer(Modifier.height(12.dp))
                    AboutCard()
                }
            }
            Spacer(Modifier.height(24.dp))
            Reveal(3) {
                NestButton(
                    text = stringResource(R.string.profile_sign_out),
                    onClick = onRequestSignOut,
                    style = NestButtonStyle.Danger,
                    leadingIcon = Icons.AutoMirrored.Rounded.Logout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_sign_out"),
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ---- Hero ---------------------------------------------------------------------------------------

@Composable
private fun Hero(profile: Profile) {
    val c = StudyNestTheme.colors
    val since = remember(profile.memberSince) {
        DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()).format(profile.memberSince.atZone(ZoneId.systemDefault()))
    }
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Reveal(0) { Avatar(profile) }
        Spacer(Modifier.height(14.dp))
        Text(
            profile.name,
            style = MaterialTheme.typography.headlineMedium,
            color = c.ink,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            profile.email,
            style = MaterialTheme.typography.bodyLarge,
            color = c.inkMuted,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.profile_member_since, since),
            style = MaterialTheme.typography.labelMedium,
            color = c.onPastel,
            modifier = Modifier
                .clip(NestShapes.Pill)
                .background(c.butter.fill)
                .padding(horizontal = 14.dp, vertical = 6.dp),
        )
    }
}

/** Initials on a slowly breathing lilac blob with a sticker lip and a few sparkles around it. */
@Composable
private fun Avatar(profile: Profile) {
    val c = StudyNestTheme.colors
    val description = stringResource(R.string.profile_avatar_description, profile.name)
    val initials = remember(profile.name, profile.email) { initialsOf(profile.name, profile.email) }
    val t = rememberInfiniteTransition(label = "avatar")
    val phase = t.animateFloat(0f, (2 * PI).toFloat(), infiniteRepeatable(tween(8000, easing = LinearEasing)), label = "avatarPhase")
    Box(
        Modifier
            .size(150.dp)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f * 0.78f
            val lip = 6.dp.toPx()
            drawBlob(Offset(center.x, center.y + lip), r, c.lilac.lip, phase.value, seed = 2)
            drawBlob(center, r, c.lilac.fill, phase.value, seed = 2)
        }
        Text(
            initials,
            style = MaterialTheme.typography.displayMedium.copy(fontFamily = Fredoka, fontWeight = FontWeight(600), fontSize = 46.sp),
            color = c.lilac.ink,
            modifier = Modifier.clearAndSetSemantics { },
        )
        Sparkle(c.butter.lip, Modifier.size(24.dp).align(Alignment.TopEnd).offset(x = (-4).dp, y = 10.dp))
        Sparkle(c.peach.lip, Modifier.size(14.dp).align(Alignment.BottomStart).offset(x = 8.dp, y = (-18).dp), delayMillis = 700)
        Sparkle(c.mint.lip, Modifier.size(10.dp).align(Alignment.TopStart).offset(x = 14.dp, y = 30.dp), delayMillis = 300)
    }
}

private fun initialsOf(name: String, email: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    val letters = when {
        parts.size >= 2 -> "${parts.first().first()}${parts.last().first()}"
        parts.size == 1 -> parts.first().take(1)
        else -> email.take(1)
    }
    return letters.uppercase(Locale.getDefault())
}

@Composable
private fun HeroSkeleton() {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(10.dp))
        SkeletonBlock(Modifier.size(124.dp), NestShapes.Pill)
        Spacer(Modifier.height(20.dp))
        SkeletonBlock(Modifier.size(width = 180.dp, height = 24.dp), NestShapes.Pill)
        Spacer(Modifier.height(10.dp))
        SkeletonBlock(Modifier.size(width = 220.dp, height = 16.dp), NestShapes.Pill)
        Spacer(Modifier.height(14.dp))
        SkeletonBlock(Modifier.size(width = 150.dp, height = 28.dp), NestShapes.Pill)
    }
}

// ---- Settings rows ------------------------------------------------------------------------------

@Composable
private fun Group(content: @Composable () -> Unit) {
    val c = StudyNestTheme.colors
    SquishySurface(
        onClick = null,
        modifier = Modifier.fillMaxWidth(),
        fill = c.surface,
        lip = c.surfaceLip,
        border = BorderStroke(1.5.dp, c.hairline),
    ) {
        Column(Modifier.padding(vertical = 6.dp)) { content() }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    pastel: Pastel,
    title: String,
    subtitle: String,
    value: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val c = StudyNestTheme.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val tint by animateFloatAsState(if (pressed) 0.06f else 0f, label = "rowTint")
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .padding(horizontal = 6.dp)
            .clip(NestShapes.Block)
            .background(c.ink.copy(alpha = tint))
            .clickable(source, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(NestShapes.Stamp)
                .background(pastel.fill),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = pastel.ink, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = c.ink)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.inkMuted)
        }
        if (value != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                value,
                style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                color = c.primaryText,
                maxLines = 1,
            )
        }
        Icon(
            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = c.inkMuted,
            modifier = Modifier
                .padding(start = 4.dp)
                .size(22.dp),
        )
    }
}

@Composable
private fun AboutCard() {
    val c = StudyNestTheme.colors
    SquishySurface(
        onClick = null,
        modifier = Modifier.fillMaxWidth(),
        fill = c.surface,
        lip = c.surfaceLip,
        border = BorderStroke(1.5.dp, c.hairline),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(NestShapes.Stamp)
                        .background(c.surfaceSunken),
                    contentAlignment = Alignment.Center,
                ) {
                    BrandMark(Modifier.size(36.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge, color = c.ink)
                    Text(stringResource(R.string.profile_tagline), style = MaterialTheme.typography.bodySmall, color = c.inkMuted)
                }
            }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(thickness = 1.dp, color = c.hairline)
            Spacer(Modifier.height(14.dp))
            Row(Modifier.semantics(mergeDescendants = true) { }, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.profile_version), style = MaterialTheme.typography.bodyMedium, color = c.inkMuted, modifier = Modifier.weight(1f))
                Text(
                    BuildConfig.VERSION_NAME,
                    style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                    color = c.ink,
                    modifier = Modifier
                        .clip(NestShapes.Pill)
                        .background(c.surfaceSunken)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.profile_fonts), style = MaterialTheme.typography.bodySmall, color = c.inkMuted)
        }
    }
}

// ---- Goal sheet ---------------------------------------------------------------------------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GoalSheetContent(sheet: GoalSheetState, onInput: (String) -> Unit, onSave: () -> Unit) {
    val c = StudyNestTheme.colors
    val parsed = Validation.minutes(sheet.input, Validation.MIN_GOAL_MINUTES, Validation.MAX_GOAL_MINUTES)
    Text(stringResource(R.string.profile_goal_sheet_body), style = MaterialTheme.typography.bodyLarge, color = c.inkMuted)
    Spacer(Modifier.height(20.dp))
    Text(
        stringResource(R.string.profile_quick_picks),
        style = MaterialTheme.typography.labelMedium,
        color = c.ink,
        modifier = Modifier.padding(start = 6.dp),
    )
    Spacer(Modifier.height(4.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        GOAL_PRESETS.forEach { minutes ->
            NestChoiceChip(
                selected = parsed == minutes,
                onClick = { onInput(minutes.toString()) },
                label = Durations.minutes(minutes),
                showCheck = true,
                modifier = Modifier.testTag("goal_preset_$minutes"),
            )
        }
    }
    Spacer(Modifier.height(16.dp))
    NestTextField(
        value = sheet.input,
        onValueChange = onInput,
        label = stringResource(R.string.profile_goal_field),
        placeholder = stringResource(R.string.profile_goal_placeholder),
        error = sheet.inputError,
        supportingText = parsed?.let { stringResource(R.string.profile_goal_preview, Durations.minutes(it)) },
        enabled = !sheet.isSaving,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onSave() }),
        trailing = {
            Text(
                stringResource(R.string.focus_minutes_unit),
                style = MaterialTheme.typography.labelLarge,
                color = c.inkMuted,
                modifier = Modifier.padding(end = 14.dp),
            )
        },
        modifier = Modifier.testTag("goal_field"),
    )
    AnimatedVisibility(
        visible = sheet.error != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        NestBanner(
            message = sheet.error.orEmpty(),
            tone = BannerTone.Error,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
    Spacer(Modifier.height(24.dp))
    PrimaryButton(
        text = stringResource(R.string.profile_goal_save),
        onClick = onSave,
        loading = sheet.isSaving,
        modifier = Modifier
            .widthIn(max = 560.dp)
            .fillMaxWidth()
            .testTag("goal_save"),
    )
}
