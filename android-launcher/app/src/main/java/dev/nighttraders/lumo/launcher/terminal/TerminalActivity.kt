package dev.nighttraders.lumo.launcher.terminal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import dev.nighttraders.lumo.launcher.ui.theme.LumoLauncherTheme
import kotlinx.coroutines.launch

class TerminalActivity : ComponentActivity() {
    private val session by lazy { TerminalSession(lifecycleScope, applicationContext) }
    private val buffer = TerminalBuffer(cols = 80, rows = 40)
    private var bufferVersion by mutableLongStateOf(0L)
    private var title by mutableStateOf("Terminal")
    private var isRunning by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureSystemBars()

        // Collect output and feed to buffer
        lifecycleScope.launch {
            session.output.collect { text ->
                buffer.process(text)
                bufferVersion = buffer.version
            }
        }

        // Track session state
        lifecycleScope.launch {
            session.isRunning.collect { running ->
                isRunning = running
            }
        }

        lifecycleScope.launch {
            session.title.collect { t ->
                title = t
            }
        }

        // Start the shell
        session.start(rows = buffer.rows, cols = buffer.cols)

        setContent {
            LumoLauncherTheme {
                TerminalScreen(
                    buffer = buffer,
                    bufferVersion = bufferVersion,
                    title = title,
                    isRunning = isRunning,
                    onInput = { text ->
                        session.write(text)
                    },
                    onSpecialKey = { key ->
                        session.sendSpecialKey(key)
                    },
                    onClose = { finish() },
                )
            }
        }
    }

    override fun onDestroy() {
        session.destroy()
        super.onDestroy()
    }

    @Deprecated("Use OnBackPressedDispatcher")
    override fun onBackPressed() {
        // Send Ctrl+C instead of closing
        session.sendSpecialKey(TerminalSession.SpecialKey.CTRL_C)
    }

    private fun configureSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
