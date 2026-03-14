package dev.nighttraders.lumo.launcher.terminal

import androidx.compose.ui.graphics.Color

/**
 * Terminal character with optional ANSI color attributes.
 */
data class TermCell(
    val char: Char = ' ',
    val fg: Color = Color(0xFFEEEEEE),
    val bg: Color = Color.Transparent,
    val bold: Boolean = false,
    val underline: Boolean = false,
)

/**
 * VT100/ANSI terminal buffer that processes escape sequences and maintains
 * a grid of characters with color attributes.
 */
class TerminalBuffer(
    var cols: Int = 80,
    var rows: Int = 40,
) {
    // Screen buffer: rows x cols grid
    private var screen: Array<Array<TermCell>> = createScreen()
    // Scrollback buffer for history
    private val scrollback = mutableListOf<Array<TermCell>>()
    private val maxScrollback = 2000

    var cursorRow = 0
        private set
    var cursorCol = 0
        private set

    // Current text attributes
    private var currentFg = defaultFg
    private var currentBg = Color.Transparent
    private var currentBold = false
    private var currentUnderline = false

    // ANSI parser state
    private var escapeState = EscapeState.NORMAL
    private val escapeParams = StringBuilder()

    // Version counter for triggering recomposition
    var version = 0L
        private set

    fun getLine(row: Int): Array<TermCell> {
        return if (row in screen.indices) screen[row] else Array(cols) { TermCell() }
    }

    fun getScrollbackLine(index: Int): Array<TermCell>? {
        return scrollback.getOrNull(index)
    }

    val scrollbackSize: Int get() = scrollback.size

    val totalLines: Int get() = scrollback.size + rows

    /**
     * Process a chunk of output from the terminal process.
     */
    fun process(text: String) {
        for (char in text) {
            processChar(char)
        }
        version++
    }

    fun resize(newCols: Int, newRows: Int) {
        if (newCols == cols && newRows == rows) return
        val oldScreen = screen
        cols = newCols
        rows = newRows
        screen = createScreen()
        // Copy what fits
        for (r in 0 until minOf(rows, oldScreen.size)) {
            for (c in 0 until minOf(cols, oldScreen[r].size)) {
                screen[r][c] = oldScreen[r][c]
            }
        }
        cursorRow = cursorRow.coerceIn(0, rows - 1)
        cursorCol = cursorCol.coerceIn(0, cols - 1)
        version++
    }

    private fun processChar(c: Char) {
        when (escapeState) {
            EscapeState.NORMAL -> processNormal(c)
            EscapeState.ESCAPE -> processEscape(c)
            EscapeState.CSI -> processCsi(c)
            EscapeState.OSC -> processOsc(c)
        }
    }

    private fun processNormal(c: Char) {
        when (c) {
            '\u001B' -> {
                escapeState = EscapeState.ESCAPE
                escapeParams.clear()
            }
            '\r' -> cursorCol = 0
            '\n' -> newLine()
            '\b' -> if (cursorCol > 0) cursorCol--
            '\t' -> {
                val nextTab = ((cursorCol / 8) + 1) * 8
                cursorCol = minOf(nextTab, cols - 1)
            }
            '\u0007' -> {} // Bell — ignore
            else -> {
                if (c.code >= 32) {
                    putChar(c)
                }
            }
        }
    }

    private fun processEscape(c: Char) {
        when (c) {
            '[' -> {
                escapeState = EscapeState.CSI
                escapeParams.clear()
            }
            ']' -> {
                escapeState = EscapeState.OSC
                escapeParams.clear()
            }
            '(' -> escapeState = EscapeState.NORMAL // Charset designation — ignore
            ')' -> escapeState = EscapeState.NORMAL
            'c' -> {
                // Reset
                reset()
                escapeState = EscapeState.NORMAL
            }
            'M' -> {
                // Reverse index (scroll down)
                if (cursorRow > 0) cursorRow-- else scrollDown()
                escapeState = EscapeState.NORMAL
            }
            else -> escapeState = EscapeState.NORMAL
        }
    }

    private fun processCsi(c: Char) {
        when {
            c in '0'..'9' || c == ';' || c == '?' -> escapeParams.append(c)
            else -> {
                executeCsi(c)
                escapeState = EscapeState.NORMAL
            }
        }
    }

    private fun processOsc(c: Char) {
        // OSC sequences end with BEL or ST
        if (c == '\u0007' || (escapeParams.endsWith('\u001B') && c == '\\')) {
            escapeState = EscapeState.NORMAL
        } else {
            escapeParams.append(c)
        }
    }

    private fun executeCsi(finalChar: Char) {
        val params = escapeParams.toString()
            .removePrefix("?")
            .split(';')
            .map { it.toIntOrNull() ?: 0 }

        fun param(index: Int, default: Int = 0): Int =
            params.getOrElse(index) { default }.let { if (it == 0) default else it }

        when (finalChar) {
            'A' -> cursorRow = maxOf(0, cursorRow - param(0, 1))
            'B' -> cursorRow = minOf(rows - 1, cursorRow + param(0, 1))
            'C' -> cursorCol = minOf(cols - 1, cursorCol + param(0, 1))
            'D' -> cursorCol = maxOf(0, cursorCol - param(0, 1))
            'E' -> { cursorRow = minOf(rows - 1, cursorRow + param(0, 1)); cursorCol = 0 }
            'F' -> { cursorRow = maxOf(0, cursorRow - param(0, 1)); cursorCol = 0 }
            'G' -> cursorCol = (param(0, 1) - 1).coerceIn(0, cols - 1)
            'H', 'f' -> {
                cursorRow = (param(0, 1) - 1).coerceIn(0, rows - 1)
                cursorCol = (param(1, 1) - 1).coerceIn(0, cols - 1)
            }
            'J' -> {
                when (param(0)) {
                    0 -> clearFromCursor()
                    1 -> clearToCursor()
                    2, 3 -> clearScreen()
                }
            }
            'K' -> {
                when (param(0)) {
                    0 -> clearLineFromCursor()
                    1 -> clearLineToCursor()
                    2 -> clearLine()
                }
            }
            'L' -> insertLines(param(0, 1))
            'M' -> deleteLines(param(0, 1))
            'P' -> deleteChars(param(0, 1))
            'S' -> repeat(param(0, 1)) { scrollUp() }
            'T' -> repeat(param(0, 1)) { scrollDown() }
            'd' -> cursorRow = (param(0, 1) - 1).coerceIn(0, rows - 1)
            'm' -> processSgr(params)
            'r' -> {} // Set scrolling region — simplified, ignore
            'h', 'l' -> {} // Set/reset mode — ignore most
            'n' -> {
                // Device status report
                if (param(0) == 6) {
                    // Cursor position report
                    val response = "\u001B[${cursorRow + 1};${cursorCol + 1}R"
                    // Would need to write back to process — skip for now
                }
            }
            '@' -> insertChars(param(0, 1))
            'X' -> eraseChars(param(0, 1))
        }
    }

    private fun processSgr(params: List<Int>) {
        var i = 0
        val p = params.ifEmpty { listOf(0) }
        while (i < p.size) {
            when (p[i]) {
                0 -> {
                    currentFg = defaultFg
                    currentBg = Color.Transparent
                    currentBold = false
                    currentUnderline = false
                }
                1 -> currentBold = true
                4 -> currentUnderline = true
                22 -> currentBold = false
                24 -> currentUnderline = false
                in 30..37 -> currentFg = ansiColor(p[i] - 30, currentBold)
                38 -> {
                    if (i + 1 < p.size && p[i + 1] == 5 && i + 2 < p.size) {
                        currentFg = xterm256Color(p[i + 2])
                        i += 2
                    } else if (i + 1 < p.size && p[i + 1] == 2 && i + 4 < p.size) {
                        currentFg = Color(p[i + 2], p[i + 3], p[i + 4])
                        i += 4
                    }
                }
                39 -> currentFg = defaultFg
                in 40..47 -> currentBg = ansiColor(p[i] - 40, false)
                48 -> {
                    if (i + 1 < p.size && p[i + 1] == 5 && i + 2 < p.size) {
                        currentBg = xterm256Color(p[i + 2])
                        i += 2
                    } else if (i + 1 < p.size && p[i + 1] == 2 && i + 4 < p.size) {
                        currentBg = Color(p[i + 2], p[i + 3], p[i + 4])
                        i += 4
                    }
                }
                49 -> currentBg = Color.Transparent
                in 90..97 -> currentFg = ansiColor(p[i] - 90 + 8, false)
                in 100..107 -> currentBg = ansiColor(p[i] - 100 + 8, false)
            }
            i++
        }
    }

    private fun putChar(c: Char) {
        if (cursorCol >= cols) {
            cursorCol = 0
            newLine()
        }
        screen[cursorRow][cursorCol] = TermCell(
            char = c,
            fg = currentFg,
            bg = currentBg,
            bold = currentBold,
            underline = currentUnderline,
        )
        cursorCol++
    }

    private fun newLine() {
        if (cursorRow < rows - 1) {
            cursorRow++
        } else {
            scrollUp()
        }
    }

    private fun scrollUp() {
        // Move top line to scrollback
        if (scrollback.size >= maxScrollback) scrollback.removeFirst()
        scrollback.add(screen[0].copyOf())
        // Shift all lines up
        for (r in 0 until rows - 1) {
            screen[r] = screen[r + 1]
        }
        screen[rows - 1] = Array(cols) { TermCell() }
    }

    private fun scrollDown() {
        for (r in rows - 1 downTo 1) {
            screen[r] = screen[r - 1]
        }
        screen[0] = Array(cols) { TermCell() }
    }

    private fun clearScreen() {
        screen = createScreen()
        cursorRow = 0
        cursorCol = 0
    }

    private fun clearFromCursor() {
        clearLineFromCursor()
        for (r in cursorRow + 1 until rows) {
            screen[r] = Array(cols) { TermCell() }
        }
    }

    private fun clearToCursor() {
        clearLineToCursor()
        for (r in 0 until cursorRow) {
            screen[r] = Array(cols) { TermCell() }
        }
    }

    private fun clearLine() {
        screen[cursorRow] = Array(cols) { TermCell() }
    }

    private fun clearLineFromCursor() {
        for (c in cursorCol until cols) {
            screen[cursorRow][c] = TermCell()
        }
    }

    private fun clearLineToCursor() {
        for (c in 0..cursorCol.coerceAtMost(cols - 1)) {
            screen[cursorRow][c] = TermCell()
        }
    }

    private fun insertLines(count: Int) {
        val n = count.coerceAtMost(rows - cursorRow)
        for (i in 0 until n) {
            for (r in rows - 1 downTo cursorRow + 1) {
                screen[r] = screen[r - 1]
            }
            screen[cursorRow] = Array(cols) { TermCell() }
        }
    }

    private fun deleteLines(count: Int) {
        val n = count.coerceAtMost(rows - cursorRow)
        for (i in 0 until n) {
            for (r in cursorRow until rows - 1) {
                screen[r] = screen[r + 1]
            }
            screen[rows - 1] = Array(cols) { TermCell() }
        }
    }

    private fun deleteChars(count: Int) {
        val n = count.coerceAtMost(cols - cursorCol)
        val row = screen[cursorRow]
        for (c in cursorCol until cols - n) {
            row[c] = row[c + n]
        }
        for (c in cols - n until cols) {
            row[c] = TermCell()
        }
    }

    private fun insertChars(count: Int) {
        val n = count.coerceAtMost(cols - cursorCol)
        val row = screen[cursorRow]
        for (c in cols - 1 downTo cursorCol + n) {
            row[c] = row[c - n]
        }
        for (c in cursorCol until cursorCol + n) {
            row[c] = TermCell()
        }
    }

    private fun eraseChars(count: Int) {
        val n = count.coerceAtMost(cols - cursorCol)
        for (c in cursorCol until cursorCol + n) {
            screen[cursorRow][c] = TermCell()
        }
    }

    private fun reset() {
        clearScreen()
        currentFg = defaultFg
        currentBg = Color.Transparent
        currentBold = false
        currentUnderline = false
    }

    private fun createScreen(): Array<Array<TermCell>> =
        Array(rows) { Array(cols) { TermCell() } }

    private enum class EscapeState {
        NORMAL, ESCAPE, CSI, OSC,
    }

    companion object {
        val defaultFg = Color(0xFFEEEEEE)

        private val ansiColors = arrayOf(
            Color(0xFF000000), // 0 Black
            Color(0xFFCC0000), // 1 Red
            Color(0xFF4E9A06), // 2 Green
            Color(0xFFC4A000), // 3 Yellow
            Color(0xFF3465A4), // 4 Blue
            Color(0xFF75507B), // 5 Magenta
            Color(0xFF06989A), // 6 Cyan
            Color(0xFFD3D7CF), // 7 White
            Color(0xFF555753), // 8 Bright black
            Color(0xFFEF2929), // 9 Bright red
            Color(0xFF8AE234), // 10 Bright green
            Color(0xFFFCE94F), // 11 Bright yellow
            Color(0xFF729FCF), // 12 Bright blue
            Color(0xFFAD7FA8), // 13 Bright magenta
            Color(0xFF34E2E2), // 14 Bright cyan
            Color(0xFFEEEEEC), // 15 Bright white
        )

        fun ansiColor(index: Int, bold: Boolean): Color {
            val adjusted = if (bold && index < 8) index + 8 else index
            return ansiColors.getOrElse(adjusted) { defaultFg }
        }

        fun xterm256Color(index: Int): Color = when {
            index < 16 -> ansiColors.getOrElse(index) { defaultFg }
            index < 232 -> {
                val i = index - 16
                val r = (i / 36) * 51
                val g = ((i % 36) / 6) * 51
                val b = (i % 6) * 51
                Color(r, g, b)
            }
            else -> {
                val gray = 8 + (index - 232) * 10
                Color(gray, gray, gray)
            }
        }
    }
}
