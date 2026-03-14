package dev.nighttraders.lumo.launcher.terminal

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream
import java.io.OutputStream

/**
 * Manages a terminal session backed by a shell process.
 * Supports connecting to Termux's proot-distro Ubuntu or falling back to a basic shell.
 */
class TerminalSession(
    private val scope: CoroutineScope,
    private val context: Context,
) {
    private var process: Process? = null
    private var outputStream: OutputStream? = null
    private var readJob: Job? = null
    private var localEcho = true

    private val _output = MutableSharedFlow<String>(replay = 64, extraBufferCapacity = 256)
    val output: SharedFlow<String> = _output.asSharedFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _title = MutableStateFlow("Terminal")
    val title: StateFlow<String> = _title.asStateFlow()

    /**
     * Start the terminal session. Tries Termux proot Ubuntu first,
     * then Termux shell, then falls back to /system/bin/sh.
     */
    fun start(rows: Int = 40, cols: Int = 80) {
        if (_isRunning.value) return

        val shellInfo = resolveShell()
        _title.value = shellInfo.title

        scope.launch {
            _output.emit("Lumo Terminal\r\n")
            _output.emit("Shell: ${shellInfo.displayName}\r\n")
            if (shellInfo.notes.isNotEmpty()) {
                _output.emit("${shellInfo.notes}\r\n")
            }
            _output.emit("\r\n")
        }

        try {
            val homeDir = ensureHomeDir(shellInfo.env["HOME"])

            val envVars = mutableMapOf(
                "TERM" to "xterm-256color",
                "COLORTERM" to "truecolor",
                "HOME" to homeDir.absolutePath,
                "LANG" to "en_US.UTF-8",
                "LINES" to rows.toString(),
                "COLUMNS" to cols.toString(),
                "PS1" to "\\$ ",
            )
            envVars.putAll(shellInfo.env)
            envVars["HOME"] = homeDir.absolutePath

            val pb = ProcessBuilder(shellInfo.command)
                .redirectErrorStream(true)

            pb.environment().putAll(envVars)
            pb.directory(homeDir)

            process = pb.start()
            outputStream = process!!.outputStream
            _isRunning.value = true

            readJob = scope.launch(Dispatchers.IO) {
                readOutput(process!!.inputStream)
            }

            // Monitor process exit
            scope.launch(Dispatchers.IO) {
                val exitCode = process?.waitFor() ?: -1
                _isRunning.value = false
                _output.emit("\r\n[Process exited with code $exitCode]\r\n")
            }
        } catch (e: Exception) {
            scope.launch {
                _output.emit("\r\nFailed to start shell: ${e.message}\r\n")
                _output.emit("Command: ${shellInfo.command.joinToString(" ")}\r\n")
                _isRunning.value = false
            }
        }
    }

    fun write(data: String) {
        // Write to the process stdin
        try {
            outputStream?.let { os ->
                os.write(data.toByteArray())
                os.flush()
            }
        } catch (_: Exception) {}

        // Local echo — without a PTY, the shell won't echo input back.
        // This is deliberately outside the try-catch above so echo works
        // even if the process pipe is broken.
        if (localEcho) {
            scope.launch {
                for (c in data) {
                    when (c) {
                        '\u007F', '\b' -> _output.emit("\b \b") // Backspace: erase character
                        '\r', '\n' -> _output.emit("\r\n")
                        else -> if (c.code >= 32) _output.emit(c.toString())
                    }
                }
            }
        }
    }

    fun write(data: ByteArray) {
        try {
            outputStream?.let { os ->
                os.write(data)
                os.flush()
            }
        } catch (_: Exception) {}
    }

    fun sendSpecialKey(key: SpecialKey) {
        write(key.sequence)
    }

    fun resize(rows: Int, cols: Int) {
        // For a basic ProcessBuilder session we can't resize the PTY.
        // The SIGWINCH approach requires native JNI. For now, environment
        // variables are set at start time.
    }

    fun destroy() {
        readJob?.cancel()
        process?.destroyForcibly()
        process = null
        outputStream = null
        _isRunning.value = false
    }

    private suspend fun readOutput(input: InputStream) {
        val buffer = ByteArray(4096)
        try {
            while (true) {
                val bytesRead = input.read(buffer)
                if (bytesRead <= 0) break
                val text = String(buffer, 0, bytesRead)
                _output.emit(text)
            }
        } catch (_: Exception) {}
    }

    private fun ensureHomeDir(preferred: String?): File {
        // Try the preferred home dir first
        if (preferred != null) {
            val dir = File(preferred)
            if (dir.isDirectory && dir.canWrite()) return dir
        }
        // Use the app's files directory — always writable
        val appHome = File(context.filesDir, "terminal_home")
        if (!appHome.exists()) appHome.mkdirs()
        return appHome
    }

    data class ShellInfo(
        val command: List<String>,
        val env: Map<String, String>,
        val title: String,
        val displayName: String,
        val notes: String = "",
    )

    /**
     * Resolve the best available shell, trying in order:
     * 1. Termux proot-distro login ubuntu (via Termux's shared user path)
     * 2. Termux bash
     * 3. /system/bin/sh (interactive)
     */
    private fun resolveShell(): ShellInfo {
        val termuxFiles = "/data/data/com.termux/files"
        val termuxUsr = "$termuxFiles/usr"
        val termuxBin = "$termuxUsr/bin"
        val termuxHome = "$termuxFiles/home"

        val termuxEnv = mapOf(
            "HOME" to termuxHome,
            "PREFIX" to termuxUsr,
            "PATH" to "$termuxBin:/usr/local/bin:/usr/bin:/bin:/system/bin",
            "LD_LIBRARY_PATH" to "$termuxUsr/lib",
            "TMPDIR" to "$termuxUsr/tmp",
            "ANDROID_DATA" to "/data",
            "ANDROID_ROOT" to "/system",
        )

        // 1. Try proot-distro Ubuntu
        val prootDistro = File("$termuxBin/proot-distro")
        if (prootDistro.exists() && prootDistro.canExecute()) {
            val ubuntuDir = File("$termuxUsr/var/lib/proot-distro/installed-rootfs/ubuntu")
            if (ubuntuDir.isDirectory) {
                return ShellInfo(
                    command = listOf("$termuxBin/proot-distro", "login", "ubuntu"),
                    env = termuxEnv,
                    title = "Ubuntu (proot)",
                    displayName = "Termux proot-distro Ubuntu",
                )
            }
        }

        // 2. Try Termux bash
        val termuxBash = File("$termuxBin/bash")
        if (termuxBash.exists() && termuxBash.canExecute()) {
            return ShellInfo(
                command = listOf("$termuxBin/bash", "-li"),
                env = termuxEnv,
                title = "Termux",
                displayName = "Termux bash",
            )
        }

        // 3. Try Termux via run-as (if debuggable/same signature)
        // This rarely works but is worth trying
        val runAsBash = tryRunAs()
        if (runAsBash != null) return runAsBash

        // 4. Fallback to system shell — force interactive mode
        val fallbackEnv = mapOf(
            "PATH" to "/system/bin:/system/xbin:/vendor/bin:/sbin",
        )
        return ShellInfo(
            command = listOf("/system/bin/sh", "-i"),
            env = fallbackEnv,
            title = "Terminal",
            displayName = "/system/bin/sh (interactive)",
            notes = "Termux not accessible (Android sandboxing). Using system shell.\nTo access Termux Ubuntu, open Termux first.",
        )
    }

    private fun tryRunAs(): ShellInfo? {
        return try {
            val test = ProcessBuilder("run-as", "com.termux", "ls", "/data/data/com.termux/files/usr/bin/bash")
                .redirectErrorStream(true)
                .start()
            val output = test.inputStream.bufferedReader().readText()
            val exitCode = test.waitFor()
            if (exitCode == 0 && output.contains("bash")) {
                ShellInfo(
                    command = listOf("run-as", "com.termux", "/data/data/com.termux/files/usr/bin/bash", "-li"),
                    env = mapOf(
                        "HOME" to "/data/data/com.termux/files/home",
                        "PREFIX" to "/data/data/com.termux/files/usr",
                        "PATH" to "/data/data/com.termux/files/usr/bin:/system/bin",
                        "LD_LIBRARY_PATH" to "/data/data/com.termux/files/usr/lib",
                    ),
                    title = "Termux",
                    displayName = "Termux bash (via run-as)",
                )
            } else null
        } catch (_: Exception) {
            null
        }
    }

    enum class SpecialKey(val sequence: String) {
        TAB("\t"),
        ESCAPE("\u001B"),
        UP("\u001B[A"),
        DOWN("\u001B[B"),
        RIGHT("\u001B[C"),
        LEFT("\u001B[D"),
        HOME("\u001B[H"),
        END("\u001B[F"),
        DELETE("\u001B[3~"),
        PAGE_UP("\u001B[5~"),
        PAGE_DOWN("\u001B[6~"),
        CTRL_C("\u0003"),
        CTRL_D("\u0004"),
        CTRL_Z("\u001A"),
        CTRL_L("\u000C"),
        CTRL_A("\u0001"),
        CTRL_E("\u0005"),
        CTRL_K("\u000B"),
        CTRL_U("\u0015"),
        CTRL_W("\u0017"),
        CTRL_R("\u0012"),
    }
}
