package dev.nighttraders.lumo.launcher.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.nighttraders.lumo.launcher.data.LumoDebugLog

@Composable
fun LumoDebugScreen(
    onBack: () -> Unit,
) {
    val entries by LumoDebugLog.entries.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    // rememberSaveable survives rotation
    var fontScale by rememberSaveable { mutableFloatStateOf(1f) }
    val baseFontSize = 11f
    val effectiveFontSize = (baseFontSize * fontScale).coerceIn(7f, 24f)

    // Auto-scroll to bottom when new entries arrive
    LaunchedEffect(entries.size) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        // Terminal title bar
        Surface(
            color = Color(0xFF1E1E1E),
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
            modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Traffic light dots
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFED3146))
                        .clickable(onClick = onBack),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFD866)),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF77DD77)),
                )

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = "lumo@debug:~$ (${entries.size} entries)",
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF999999),
                    modifier = Modifier.weight(1f),
                )

                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "Clear log",
                    tint = Color(0xFF777777),
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { LumoDebugLog.clear() },
                )

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF777777),
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(onClick = onBack),
                )
            }
        }

        // Terminal body — selectable text, pinch-to-zoom
        Surface(
            color = Color(0xFF0D0D0D),
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoom, _ ->
                            fontScale = (fontScale * zoom).coerceIn(0.6f, 2.2f)
                        }
                    },
            ) {
                if (entries.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No log entries yet.",
                            fontFamily = FontFamily.Monospace,
                            fontSize = effectiveFontSize.sp,
                            color = Color(0xFF555555),
                        )
                    }
                } else {
                    // Build all log text as a single selectable annotated string
                    val annotated = buildAnnotatedLogText(entries)

                    SelectionContainer(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = annotated,
                            fontFamily = FontFamily.Monospace,
                            fontSize = effectiveFontSize.sp,
                            lineHeight = (effectiveFontSize * 1.4f).sp,
                        )
                    }
                }
            }
        }
    }
}

private fun buildAnnotatedLogText(entries: List<LumoDebugLog.Entry>): AnnotatedString =
    buildAnnotatedString {
        entries.forEachIndexed { index, entry ->
            val color = Color(entry.level.color)
            withStyle(SpanStyle(color = color)) {
                append(entry.formatted())
            }
            if (index < entries.size - 1) {
                append("\n")
            }
        }
    }
