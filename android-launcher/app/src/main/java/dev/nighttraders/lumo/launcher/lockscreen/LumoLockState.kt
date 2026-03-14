package dev.nighttraders.lumo.launcher.lockscreen

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton that tracks whether the launcher dash is locked.
 * Starts locked on every process start. Resets to locked on screen off.
 * Only unlocked after successful PIN/password verification.
 */
object LumoLockState {
    private val _isLocked = MutableStateFlow(true)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    fun unlock() {
        _isLocked.value = false
    }

    fun lock() {
        _isLocked.value = true
    }
}
