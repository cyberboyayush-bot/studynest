package me.aysh.studynest.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

/**
 * A [NestBottomSheet] driven by nullable ViewModel state: it opens when [value] becomes non-null and, when the
 * ViewModel clears it (e.g. after a successful save), slides closed with the normal hide animation instead of
 * vanishing. The last non-null value keeps rendering while it animates out.
 *
 * [lockOpen] (e.g. while saving) stops swipe/scrim/back from hiding the sheet. [modifier] lands on the sheet's
 * content column (put the sheet's test tag there).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T : Any> StatefulNestSheet(
    value: T?,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    lockOpen: Boolean = false,
    content: @Composable ColumnScope.(T) -> Unit,
) {
    var retained by remember { mutableStateOf(value) }
    SideEffect { if (value != null) retained = value }
    val current = value ?: retained ?: return
    val locked by rememberUpdatedState(lockOpen)
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden || !locked },
    )
    LaunchedEffect(value == null) {
        if (value == null) {
            try {
                sheetState.hide()
            } finally {
                retained = null
            }
        }
    }
    NestBottomSheet(
        onDismissRequest = { if (!locked) onDismissRequest() },
        modifier = modifier,
        sheetState = sheetState,
        title = title,
    ) {
        content(current)
    }
}
