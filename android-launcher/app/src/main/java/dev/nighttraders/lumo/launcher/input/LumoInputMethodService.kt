package dev.nighttraders.lumo.launcher.input

import android.content.res.ColorStateList
import android.graphics.Color
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.text.InputType
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.CompletionInfo
import android.view.inputmethod.EditorInfo
import android.view.textservice.SentenceSuggestionsInfo
import android.view.textservice.SpellCheckerSession
import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextInfo
import android.view.textservice.TextServicesManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import androidx.core.content.getSystemService

class LumoInputMethodService : InputMethodService(), SpellCheckerSession.SpellCheckerSessionListener {
    private var shifted = false
    private var symbolMode = false
    private var alternateSymbolMode = false
    private var spellCheckerSession: SpellCheckerSession? = null
    private lateinit var rootView: LinearLayout
    private lateinit var suggestionStrip: LinearLayout
    private lateinit var keysContainer: LinearLayout
    private var appSuggestions: List<SuggestionCandidate> = emptyList()
    private var spellSuggestions: List<SuggestionCandidate> = emptyList()
    private var currentWord = ""

    override fun onCreate() {
        super.onCreate()
        val textServicesManager = getSystemService(TextServicesManager::class.java)
        spellCheckerSession = textServicesManager?.newSpellCheckerSession(null, null, this, true)
    }

    override fun onDestroy() {
        spellCheckerSession?.close()
        spellCheckerSession = null
        super.onDestroy()
    }

    override fun onCreateInputView(): View {
        suggestionStrip = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        keysContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        rootView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#1B0F16"))
            setPadding(dp(8), dp(10), dp(8), dp(10))

            addView(
                HorizontalScrollView(this@LumoInputMethodService).apply {
                    isHorizontalScrollBarEnabled = false
                    overScrollMode = View.OVER_SCROLL_NEVER
                    addView(
                        suggestionStrip,
                        FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.WRAP_CONTENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT,
                        ),
                    )
                },
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ).apply {
                    bottomMargin = dp(8)
                },
            )

            addView(
                keysContainer,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                ),
            )
        }

        rebuildKeyboardRows()
        refreshSuggestionStrip()
        return rootView
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        shifted = false
        symbolMode = false
        alternateSymbolMode = false
        appSuggestions = emptyList()
        spellSuggestions = emptyList()
        currentWord = ""
        rebuildKeyboardRows()
        refreshSuggestionStrip()
    }

    override fun onDisplayCompletions(completions: Array<CompletionInfo>?) {
        appSuggestions = completions.orEmpty()
            .mapNotNull { completion ->
                completion.text?.toString()
                    ?.takeIf(String::isNotBlank)
                    ?.let { text -> SuggestionCandidate(text = text, completion = completion) }
            }
            .distinctBy { suggestion -> suggestion.text.lowercase() }
            .take(3)
        refreshSuggestionStrip()
    }

    override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        candidatesStart: Int,
        candidatesEnd: Int,
    ) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        updateSuggestions()
    }

    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onGetSuggestions(results: Array<SuggestionsInfo>) {
        val word = currentWordBeforeCursor()
        if (word.isBlank()) {
            spellSuggestions = emptyList()
            refreshSuggestionStrip()
            return
        }

        spellSuggestions = results
            .flatMap { info ->
                (0 until info.suggestionsCount).mapNotNull { index ->
                    info.getSuggestionAt(index)
                        ?.trim()
                        ?.takeIf { suggestion ->
                            suggestion.isNotBlank() && !suggestion.equals(word, ignoreCase = true)
                        }
                }
            }
            .distinctBy { suggestion -> suggestion.lowercase() }
            .take(3)
            .map(::SuggestionCandidate)

        refreshSuggestionStrip()
    }

    override fun onGetSentenceSuggestions(results: Array<SentenceSuggestionsInfo>) {
        val collected = buildList {
            results.forEach { sentence ->
                repeat(sentence.suggestionsCount) { index ->
                    val suggestionsInfo = sentence.getSuggestionsInfoAt(index)
                    repeat(suggestionsInfo.suggestionsCount) { suggestionIndex ->
                        suggestionsInfo.getSuggestionAt(suggestionIndex)
                            ?.trim()
                            ?.takeIf(String::isNotBlank)
                            ?.let(::add)
                    }
                }
            }
        }

        if (collected.isEmpty()) {
            return
        }

        val activeWord = currentWordBeforeCursor()
        spellSuggestions = collected
            .filterNot { suggestion -> suggestion.equals(activeWord, ignoreCase = true) }
            .distinctBy { suggestion -> suggestion.lowercase() }
            .take(3)
            .map(::SuggestionCandidate)
        refreshSuggestionStrip()
    }

    private fun rebuildKeyboardRows() {
        keysContainer.removeAllViews()

        keyboardLayoutForCurrentMode().forEach { row ->
            keysContainer.addView(createMixedRow(row))
        }
    }

    private fun keyboardLayoutForCurrentMode(): List<List<KeySpec>> =
        when {
            !symbolMode -> listOf(
                listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p").map(::letterKey),
                listOf("a", "s", "d", "f", "g", "h", "j", "k", "l").map(::letterKey),
                buildList {
                    add(actionKey(if (shifted) "SHIFT" else "Shift", weight = 1.4f, highlighted = shifted) {
                        toggleShift()
                    })
                    addAll(listOf("z", "x", "c", "v", "b", "n", "m").map(::letterKey))
                    add(actionKey("Bksp", weight = 1.4f) { backspace() })
                },
                listOf(
                    actionKey("?123", weight = 1.3f, highlighted = symbolMode) { openPrimarySymbols() },
                    textKey(","),
                    actionKey("Space", weight = 4.2f) { commitSpace() },
                    textKey("."),
                    actionKey("Enter", weight = 1.6f) { handleEnter() },
                ),
            )

            !alternateSymbolMode -> listOf(
                listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0").map(::textKey),
                listOf("@", "#", "$", "%", "&", "-", "+", "(", ")").map(::textKey),
                buildList {
                    addAll(listOf("=", "/", ";", ":", "\"", "'", "!", "?").map(::textKey))
                    add(actionKey("Bksp", weight = 1.4f) { backspace() })
                },
                listOf(
                    actionKey("ABC", weight = 1.2f) { closeSymbols() },
                    actionKey("#+=", weight = 1.2f, highlighted = alternateSymbolMode) { openAlternateSymbols() },
                    actionKey("Space", weight = 3.8f) { commitSpace() },
                    textKey("."),
                    actionKey("Enter", weight = 1.6f) { handleEnter() },
                ),
            )

            else -> listOf(
                listOf("[", "]", "{", "}", "<", ">", "^", "*", "+", "=").map(::textKey),
                listOf("_", "\\", "|", "~", "`", "€", "£", "¥", "•").map(::textKey),
                buildList {
                    addAll(listOf("!", "?", "\"", "'", ":", ";", "(", ")").map(::textKey))
                    add(actionKey("Bksp", weight = 1.4f) { backspace() })
                },
                listOf(
                    actionKey("ABC", weight = 1.2f) { closeSymbols() },
                    actionKey("?123", weight = 1.2f, highlighted = true) { openPrimarySymbols() },
                    actionKey("Space", weight = 3.8f) { commitSpace() },
                    textKey("/"),
                    actionKey("Enter", weight = 1.6f) { handleEnter() },
                ),
            )
        }

    private fun letterKey(value: String): KeySpec =
        textKey(if (shifted) value.uppercase() else value)

    private fun createMixedRow(keys: List<KeySpec>): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                bottomMargin = dp(6)
            }

            keys.forEach { key ->
                addView(createKeyButton(key))
            }
        }

    private fun createKeyButton(spec: KeySpec): Button =
        Button(this).apply {
            text = spec.value
            isAllCaps = false
            setTextColor(Color.WHITE)
            textSize = 16f
            gravity = Gravity.CENTER
            minHeight = dp(52)
            minimumHeight = dp(52)
            setPadding(dp(4), dp(10), dp(4), dp(10))
            backgroundTintList = ColorStateList.valueOf(
                when {
                    spec.isSpecial && spec.highlighted -> Color.parseColor("#FF6E40")
                    spec.isSpecial -> Color.parseColor("#E95420")
                    else -> Color.parseColor("#5E2750")
                },
            )
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                spec.weight,
            ).apply {
                marginStart = dp(3)
                marginEnd = dp(3)
            }
            setOnClickListener { handleKey(spec) }
        }

    private fun handleKey(spec: KeySpec) {
        performKeyHaptic()
        spec.action?.invoke()
        if (spec.action != null) {
            return
        }

        commitText(spec.value)
        if (!symbolMode && shifted && spec.value.length == 1 && spec.value[0].isLetter()) {
            shifted = false
            rebuildKeyboardRows()
        }
        updateSuggestions()
    }

    private fun commitText(value: String) {
        currentInputConnection?.commitText(value, 1)
    }

    private fun toggleShift() {
        shifted = !shifted
        rebuildKeyboardRows()
    }

    private fun openPrimarySymbols() {
        symbolMode = true
        alternateSymbolMode = false
        rebuildKeyboardRows()
        updateSuggestions()
    }

    private fun openAlternateSymbols() {
        symbolMode = true
        alternateSymbolMode = true
        rebuildKeyboardRows()
        updateSuggestions()
    }

    private fun closeSymbols() {
        symbolMode = false
        alternateSymbolMode = false
        rebuildKeyboardRows()
        updateSuggestions()
    }

    private fun commitSpace() {
        commitText(" ")
        clearSuggestions()
    }

    private fun backspace() {
        currentInputConnection?.deleteSurroundingText(1, 0)
        updateSuggestions()
    }

    private fun handleEnter() {
        val editorInfo = currentInputEditorInfo
        if (editorInfo != null &&
            editorInfo.imeOptions and EditorInfo.IME_MASK_ACTION != EditorInfo.IME_ACTION_NONE &&
            editorInfo.inputType and InputType.TYPE_TEXT_FLAG_MULTI_LINE == 0
        ) {
            currentInputConnection?.performEditorAction(editorInfo.imeOptions and EditorInfo.IME_MASK_ACTION)
            clearSuggestions()
            return
        }

        currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
        currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
        clearSuggestions()
    }

    private fun updateSuggestions() {
        currentWord = currentWordBeforeCursor()
        if (currentWord.length < 2) {
            spellSuggestions = emptyList()
            refreshSuggestionStrip()
            return
        }

        spellCheckerSession?.getSuggestions(TextInfo(currentWord), 3)
        refreshSuggestionStrip()
    }

    private fun refreshSuggestionStrip() {
        if (!::suggestionStrip.isInitialized) {
            return
        }

        suggestionStrip.removeAllViews()
        val suggestions = buildList {
            if (currentWord.isNotBlank()) {
                add(SuggestionCandidate(text = currentWord))
            }
            addAll(appSuggestions)
            addAll(spellSuggestions)
        }
            .filter { candidate -> candidate.text.isNotBlank() }
            .distinctBy { candidate -> candidate.text.lowercase() }
            .take(3)

        if (suggestions.isEmpty()) {
            suggestionStrip.addView(
                createSuggestionButton(
                    text = if (symbolMode) "Symbols ready" else "Type for suggestions",
                    enabled = false,
                    primary = false,
                    onClick = {},
                ),
            )
            return
        }

        suggestions.forEachIndexed { index, suggestion ->
            suggestionStrip.addView(
                createSuggestionButton(
                    text = suggestion.text,
                    enabled = true,
                    primary = index == 0,
                ) {
                    applySuggestion(suggestion)
                },
            )
        }
    }

    private fun createSuggestionButton(
        text: String,
        enabled: Boolean,
        primary: Boolean,
        onClick: () -> Unit,
    ): View =
        Button(this).apply {
            this.text = text
            isAllCaps = false
            isEnabled = enabled
            gravity = Gravity.CENTER
            setTextColor(if (enabled) Color.WHITE else Color.parseColor("#AAFFFFFF"))
            textSize = 14f
            minHeight = dp(42)
            minimumHeight = dp(42)
            backgroundTintList = ColorStateList.valueOf(
                when {
                    !enabled -> Color.parseColor("#241A25")
                    primary -> Color.parseColor("#E95420")
                    else -> Color.parseColor("#43214D")
                },
            )
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                marginEnd = dp(6)
            }
            setPadding(dp(16), dp(8), dp(16), dp(8))
            setOnClickListener {
                performKeyHaptic()
                onClick()
            }
        }

    private fun applySuggestion(suggestion: SuggestionCandidate) {
        suggestion.completion?.let { completion ->
            currentInputConnection?.commitCompletion(completion)
            clearSuggestions()
            return
        }

        val activeWord = currentWordBeforeCursor()
        if (activeWord.isNotBlank()) {
            currentInputConnection?.deleteSurroundingText(activeWord.length, 0)
        }
        currentInputConnection?.commitText(suggestion.text, 1)
        clearSuggestions()
    }

    private fun clearSuggestions() {
        currentWord = ""
        spellSuggestions = emptyList()
        appSuggestions = emptyList()
        refreshSuggestionStrip()
    }

    private fun currentWordBeforeCursor(): String {
        val textBeforeCursor = currentInputConnection?.getTextBeforeCursor(64, 0)?.toString().orEmpty()
        return textBeforeCursor.takeLastWhile { character ->
            character.isLetterOrDigit() || character == '\'' || character == '_'
        }
    }

    private fun performKeyHaptic() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService<VibratorManager>()
                ?.defaultVibrator
                ?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        } else {
            @Suppress("DEPRECATION")
            getSystemService<Vibrator>()
                ?.vibrate(12)
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private data class KeySpec(
        val value: String,
        val isSpecial: Boolean,
        val weight: Float,
        val highlighted: Boolean = false,
        val action: (() -> Unit)? = null,
    )

    private data class SuggestionCandidate(
        val text: String,
        val completion: CompletionInfo? = null,
    )

    private fun textKey(value: String): KeySpec = KeySpec(value = value, isSpecial = false, weight = 1f)

    private fun actionKey(
        value: String,
        weight: Float = 1f,
        highlighted: Boolean = false,
        action: () -> Unit,
    ): KeySpec = KeySpec(
        value = value,
        isSpecial = true,
        weight = weight,
        highlighted = highlighted,
        action = action,
    )
}
