package dev.nighttraders.lumo.launcher.terminal

import android.view.KeyEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

// Ubuntu Touch terminal palette
private val TermBg = Color(0xFF300A24) // Ubuntu aubergine terminal bg
private val TermHeaderBg = Color(0xFF2C001E)
private val ToolbarBg = Color(0xFF1A0816)
private val ToolbarKeyBg = Color(0xFF3C2847)
private val ToolbarKeyFg = Color(0xFFEEEEEE)
private val UbuntuOrange = Color(0xFFE95420)
private val TextLight = Color(0xFFEEEEEE)
private val TextMuted = Color(0xFF888888)
private val CursorColor = Color(0xFFE95420)

@Composable
fun TerminalScreen(
    buffer: TerminalBuffer,
    bufferVersion: Long,
    title: String,
    isRunning: Boolean,
    onInput: (String) -> Unit,
    onSpecialKey: (TerminalSession.SpecialKey) -> Unit,
    onClose: () -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    var hiddenInput by remember { mutableStateOf(TextFieldValue("")) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TermBg)
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        // Header bar
        Surface(color = TermHeaderBg) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Close button
                Surface(
                    modifier = Modifier
                        .clickable(onClick = onClose),
                    color = Color(0x33FFFFFF),
                    shape = RoundedCornerShape(4.dp),
                ) {
                    Text(
                        text = " \u2715 ",
                        fontSize = 14.sp,
                        color = TextLight,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextLight,
                    modifier = Modifier.weight(1f),
                )

                // Status indicator
                Box(
                    modifier = Modifier
                        .width(8.dp)
                        .height(8.dp)
                        .background(
                            color = if (isRunning) Color(0xFF4E9A06) else Color(0xFFCC0000),
                            shape = RoundedCornerShape(4.dp),
                        ),
                )
            }
        }

        // Terminal content area — canvas-based rendering
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures {
                        focusRequester.requestFocus()
                        keyboardController?.show()
                    }
                },
        ) {
            // Hidden text field to capture keyboard input
            BasicTextField(
                value = hiddenInput,
                onValueChange = { newValue ->
                    val oldLen = hiddenInput.text.length
                    val newLen = newValue.text.length
                    if (newLen > oldLen) {
                        val added = newValue.text.substring(oldLen)
                        // Check for newlines (Enter key)
                        for (c in added) {
                            if (c == '\n') {
                                onInput("\n")
                            } else {
                                onInput(c.toString())
                            }
                        }
                    } else if (newLen < oldLen) {
                        onInput("\u007F")
                    }
                    hiddenInput = newValue
                    // Periodically reset to prevent unbounded growth
                    if (hiddenInput.text.length > 200) {
                        hiddenInput = TextFieldValue("")
                    }
                },
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .width(1.dp)
                    .height(1.dp),
                textStyle = TextStyle(fontSize = 1.sp, color = Color.Transparent),
                cursorBrush = SolidColor(Color.Transparent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.None),
                singleLine = false,
            )

            TerminalCanvasView(
                buffer = buffer,
                version = bufferVersion,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Special keys toolbar (Ubuntu Touch style)
        TerminalToolbar(
            onSpecialKey = onSpecialKey,
            onInput = onInput,
        )

        // Orange accent bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(UbuntuOrange),
        )
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }
}

@Composable
private fun TerminalCanvasView(
    buffer: TerminalBuffer,
    version: Long,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val charWidthSp = 8.sp
    val charHeightSp = 16.sp

    val charWidthPx = with(density) { charWidthSp.toPx() }
    val charHeightPx = with(density) { charHeightSp.toPx() }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Auto-scroll to bottom
    LaunchedEffect(version) {
        val totalItems = buffer.scrollbackSize + buffer.rows
        if (totalItems > 0) {
            listState.scrollToItem(maxOf(0, totalItems - buffer.rows))
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.padding(horizontal = 4.dp),
    ) {
        // Scrollback lines
        items(buffer.scrollbackSize) { index ->
            val line = buffer.getScrollbackLine(index) ?: return@items
            TerminalLineCanvas(
                cells = line,
                charWidth = charWidthPx,
                charHeight = charHeightPx,
                cursorCol = -1,
            )
        }
        // Screen lines
        items(buffer.rows) { row ->
            val line = buffer.getLine(row)
            TerminalLineCanvas(
                cells = line,
                charWidth = charWidthPx,
                charHeight = charHeightPx,
                cursorCol = if (row == buffer.cursorRow) buffer.cursorCol else -1,
            )
        }
    }
}

@Composable
private fun TerminalLineCanvas(
    cells: Array<TermCell>,
    charWidth: Float,
    charHeight: Float,
    cursorCol: Int,
) {
    val lineHeightDp = with(LocalDensity.current) { charHeight.toDp() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(lineHeightDp),
    ) {
        // Layer 1: backgrounds and cursor
        Canvas(modifier = Modifier.matchParentSize()) {
            cells.forEachIndexed { col, cell ->
                if (cell.bg != Color.Transparent) {
                    drawRect(
                        color = cell.bg,
                        topLeft = Offset(col * charWidth, 0f),
                        size = Size(charWidth, charHeight),
                    )
                }
            }

            if (cursorCol in cells.indices) {
                drawRect(
                    color = CursorColor,
                    topLeft = Offset(cursorCol * charWidth, 0f),
                    size = Size(charWidth, charHeight),
                    alpha = 0.7f,
                )
            }
        }

        // Layer 2: text overlay
        Row(
            modifier = Modifier
                .matchParentSize()
                .horizontalScroll(rememberScrollState()),
        ) {
            var i = 0
            while (i < cells.size) {
                val startIdx = i
                val fg = cells[i].fg
                val bold = cells[i].bold
                while (i < cells.size && cells[i].fg == fg && cells[i].bold == bold) {
                    i++
                }
                val text = buildString {
                    for (j in startIdx until i) {
                        append(cells[j].char)
                    }
                }
                val displayText = if (i == cells.size) text.trimEnd() else text
                if (displayText.isNotEmpty()) {
                    Text(
                        text = displayText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = with(LocalDensity.current) { charHeight.toSp() },
                        color = fg,
                        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
                        letterSpacing = 0.sp,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun TerminalToolbar(
    onSpecialKey: (TerminalSession.SpecialKey) -> Unit,
    onInput: (String) -> Unit,
) {
    val scrollState = rememberScrollState()

    Surface(color = ToolbarBg) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ToolbarKey("Esc") { onSpecialKey(TerminalSession.SpecialKey.ESCAPE) }
            ToolbarKey("Tab") { onSpecialKey(TerminalSession.SpecialKey.TAB) }
            ToolbarKey("Ctrl") { } // Modifier — handled below
            ToolbarKey("\u2191") { onSpecialKey(TerminalSession.SpecialKey.UP) }
            ToolbarKey("\u2193") { onSpecialKey(TerminalSession.SpecialKey.DOWN) }
            ToolbarKey("\u2190") { onSpecialKey(TerminalSession.SpecialKey.LEFT) }
            ToolbarKey("\u2192") { onSpecialKey(TerminalSession.SpecialKey.RIGHT) }
            ToolbarKey("Home") { onSpecialKey(TerminalSession.SpecialKey.HOME) }
            ToolbarKey("End") { onSpecialKey(TerminalSession.SpecialKey.END) }
            ToolbarKey("^C") { onSpecialKey(TerminalSession.SpecialKey.CTRL_C) }
            ToolbarKey("^D") { onSpecialKey(TerminalSession.SpecialKey.CTRL_D) }
            ToolbarKey("^Z") { onSpecialKey(TerminalSession.SpecialKey.CTRL_Z) }
            ToolbarKey("^L") { onSpecialKey(TerminalSession.SpecialKey.CTRL_L) }
            ToolbarKey("^A") { onSpecialKey(TerminalSession.SpecialKey.CTRL_A) }
            ToolbarKey("^E") { onSpecialKey(TerminalSession.SpecialKey.CTRL_E) }
            ToolbarKey("^R") { onSpecialKey(TerminalSession.SpecialKey.CTRL_R) }
            ToolbarKey("^W") { onSpecialKey(TerminalSession.SpecialKey.CTRL_W) }
            ToolbarKey("|") { onInput("|") }
            ToolbarKey("/") { onInput("/") }
            ToolbarKey("-") { onInput("-") }
            ToolbarKey("~") { onInput("~") }
        }
    }
}

@Composable
private fun ToolbarKey(
    label: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .clickable(onClick = onClick),
        color = ToolbarKeyBg,
        shape = RoundedCornerShape(4.dp),
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = ToolbarKeyFg,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            fontFamily = FontFamily.Monospace,
        )
    }
}
