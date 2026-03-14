package dev.nighttraders.lumo.launcher.data

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Singleton debug logger that stores entries in-memory for display in the
 * terminal-style debug console inside Lumo Settings.
 */
object LumoDebugLog {
    private const val MAX_ENTRIES = 500
    private const val TAG = "LumoDebug"

    data class Entry(
        val timestamp: Long,
        val level: Level,
        val tag: String,
        val message: String,
    ) {
        enum class Level(val label: String, val color: Long) {
            INFO("INF", 0xFF77DD77),
            WARN("WRN", 0xFFFFD866),
            ERROR("ERR", 0xFFED3146),
            DEBUG("DBG", 0xFF88AAFF),
            FIX("FIX", 0xFF77DD77),
        }

        fun formatted(): String {
            val ts = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(timestamp))
            return "[$ts] [${level.label}] $tag: $message"
        }
    }

    private val _entries = MutableStateFlow<List<Entry>>(emptyList())
    val entries: StateFlow<List<Entry>> = _entries.asStateFlow()

    private fun append(level: Entry.Level, tag: String, message: String) {
        val entry = Entry(
            timestamp = System.currentTimeMillis(),
            level = level,
            tag = tag,
            message = message,
        )
        synchronized(this) {
            val current = _entries.value.toMutableList()
            current.add(entry)
            if (current.size > MAX_ENTRIES) {
                current.removeAt(0)
            }
            _entries.value = current
        }
        Log.d(TAG, entry.formatted())
    }

    fun i(tag: String, message: String) = append(Entry.Level.INFO, tag, message)
    fun w(tag: String, message: String) = append(Entry.Level.WARN, tag, message)
    fun e(tag: String, message: String) = append(Entry.Level.ERROR, tag, message)
    fun d(tag: String, message: String) = append(Entry.Level.DEBUG, tag, message)
    fun fix(tag: String, message: String) = append(Entry.Level.FIX, tag, message)

    fun clear() {
        synchronized(this) {
            _entries.value = emptyList()
        }
    }

    private var knownIssuesLogged = false

    /** Log known bugs and their fixes for developer reference. */
    fun logKnownIssues() {
        if (knownIssuesLogged) return
        knownIssuesLogged = true
        fix("IconRender", "Ubuntu symbol used evenOdd fill — stroke+transparent broke Compose tint")
        fix("StateTrack", "Replaced scattered booleans with LauncherScreen enum for single source of truth")
        fix("NavIntent", "Added navigationRequestId counter — sidebar/home intents now always update currentScreen")
        fix("UsageStats", "Replaced invalid android:requiredFeature with tools:ignore on PACKAGE_USAGE_STATS")
        fix("BFBGhost", "Removed old ic_ubuntu_bfb.xml and all references — eliminated random gray dot icon")
        fix("SidebarIcons", "Rewrote overlay sidebar buildRailView() — removed broken dot grid and old BFB button")
        fix("AppDrawerBtn", "Ungated left edge gesture — rail now reveals from any screen, not just home")
        fix("BackGesture", "Removed LumoBackGestureService entirely — was unused accessibility service")
        fix("MultitaskCards", "Reduced card height to 320dp, added accent-tinted backgrounds")
        i("Startup", "LumoDebugLog initialized — tracking ${_entries.value.size} known fixes")
    }
}
