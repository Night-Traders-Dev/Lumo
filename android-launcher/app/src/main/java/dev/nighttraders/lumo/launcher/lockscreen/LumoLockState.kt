package dev.nighttraders.lumo.launcher.lockscreen

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Singleton that tracks whether the launcher dash is locked.
 * Starts locked on every process start. Resets to locked on screen off
 * after a configurable delay (default 3 seconds).
 * Only unlocked after successful PIN/password verification.
 */
object LumoLockState {
    private val _isLocked = MutableStateFlow(true)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()
    private val scope = CoroutineScope(Dispatchers.Main)
    private var lockJob: Job? = null
    private const val LOCK_DELAY_MS = 3000L

    fun unlock() {
        lockJob?.cancel()
        lockJob = null
        _isLocked.value = false
    }

    /**
     * Schedules the lock to engage after [LOCK_DELAY_MS].
     * If the screen turns back on within that window (and [unlock] is called),
     * the lock is cancelled — the user stays unlocked.
     */
    fun lock() {
        lockJob?.cancel()
        lockJob = scope.launch {
            delay(LOCK_DELAY_MS)
            _isLocked.value = true
        }
    }

    /** Lock immediately without delay (used on process start). */
    fun lockNow() {
        lockJob?.cancel()
        lockJob = null
        _isLocked.value = true
    }
}
