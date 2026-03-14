package dev.nighttraders.lumo.launcher.input

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Rect
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.text.TextUtils
import android.text.InputType
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
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
import java.util.Locale
import kotlin.math.abs

class LumoInputMethodService : InputMethodService(), SpellCheckerSession.SpellCheckerSessionListener {
    private var shifted = false
    private var autoCapitalization = false
    private var symbolMode = false
    private var alternateSymbolMode = false

    private var spellCheckerSession: SpellCheckerSession? = null

    private lateinit var rootView: LinearLayout
    private lateinit var suggestionStrip: LinearLayout
    private lateinit var keysContainer: LinearLayout

    private var appSuggestions: List<SuggestionCandidate> = emptyList()
    private var localSuggestions: List<SuggestionCandidate> = emptyList()
    private var spellSuggestions: List<SuggestionCandidate> = emptyList()
    private var swipePreviewSuggestions: List<SuggestionCandidate> = emptyList()
    private var currentWord = ""
    private var lastSuggestionRequestWord = ""
    private var lastSuggestionResultWord = ""
    private var pendingAutoCorrection: SuggestionCandidate? = null

    private val letterKeyTargets = mutableListOf<KeyTouchTarget>()
    private val swipeTrail = mutableListOf<String>()
    private var swipeInProgress = false
    private var swipeStartX = 0f
    private var swipeStartY = 0f

    private var spaceCursorMode = false
    private var spaceTouchStartX = 0f
    private var spaceCursorAccumulator = 0f

    private val wordEngine by lazy { KeyboardWordEngine.get(applicationContext) }

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

        updateAutoCapitalization()
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
        localSuggestions = emptyList()
        spellSuggestions = emptyList()
        swipePreviewSuggestions = emptyList()
        currentWord = ""
        lastSuggestionRequestWord = ""
        lastSuggestionResultWord = ""
        pendingAutoCorrection = null
        updateAutoCapitalization()
        rebuildKeyboardRows()
        refreshSuggestionStrip()
    }

    override fun onDisplayCompletions(completions: Array<CompletionInfo>?) {
        appSuggestions = completions.orEmpty()
            .mapNotNull { completion ->
                completion.text
                    ?.toString()
                    ?.trim()
                    ?.takeIf(String::isNotBlank)
                    ?.let { text ->
                        SuggestionCandidate(
                            text = text,
                            completion = completion,
                        )
                    }
            }
            .distinctBy { suggestion -> suggestion.text.lowercase(Locale.getDefault()) }
            .take(3)
        refreshLocalSuggestionState()
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
        mergeSpellSuggestions(
            sourceWord = lastSuggestionRequestWord,
            suggestions = results.flatMap { info ->
                (0 until info.suggestionsCount).mapNotNull { index ->
                    info.getSuggestionAt(index)
                        ?.trim()
                        ?.takeIf(String::isNotBlank)
                }
            },
        )
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

        mergeSpellSuggestions(
            sourceWord = lastSuggestionRequestWord,
            suggestions = collected,
        )
    }

    private fun mergeSpellSuggestions(
        sourceWord: String,
        suggestions: List<String>,
    ) {
        val normalizedWord = sourceWord.trim()
        if (normalizedWord.isNotBlank() &&
            !normalizedWord.equals(currentWord, ignoreCase = true)
        ) {
            return
        }
        lastSuggestionResultWord = normalizedWord

        if (normalizedWord.length < 2) {
            spellSuggestions = emptyList()
            pendingAutoCorrection = null
            refreshSuggestionStrip()
            return
        }

        spellSuggestions = suggestions
            .filterNot { suggestion -> suggestion.equals(normalizedWord, ignoreCase = true) }
            .distinctBy { suggestion -> suggestion.lowercase(Locale.getDefault()) }
            .take(4)
            .map { suggestion ->
                SuggestionCandidate(
                    text = applyCaseToSuggestion(suggestion, normalizedWord),
                )
            }

        refreshLocalSuggestionState()
        refreshSuggestionStrip()
    }

    private fun rebuildKeyboardRows() {
        keysContainer.removeAllViews()
        letterKeyTargets.clear()

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
                    add(actionKey(if (isShiftActive()) "SHIFT" else "Shift", weight = 1.4f, highlighted = isShiftActive()) {
                        toggleShift()
                    })
                    addAll(listOf("z", "x", "c", "v", "b", "n", "m").map(::letterKey))
                    add(actionKey("Bksp", weight = 1.4f) { backspace() })
                },
                listOf(
                    actionKey("?123", weight = 1.3f, highlighted = symbolMode) { openPrimarySymbols() },
                    textKey(","),
                    spaceKey(weight = 4.2f),
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
                    spaceKey(weight = 3.8f),
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
                    spaceKey(weight = 3.8f),
                    textKey("/"),
                    actionKey("Enter", weight = 1.6f) { handleEnter() },
                ),
            )
        }

    private fun letterKey(value: String): KeySpec =
        textKey(
            if (isShiftActive()) {
                value.uppercase(Locale.getDefault())
            } else {
                value
            },
        )

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

            if (spec.isLetterKey) {
                letterKeyTargets += KeyTouchTarget(
                    button = this,
                    value = spec.value.lowercase(Locale.getDefault()),
                    displayValue = spec.value,
                )
                setOnTouchListener(LetterSwipeTouchListener(spec))
            } else if (spec.isSpaceKey) {
                setOnTouchListener(SpaceCursorTouchListener())
            } else {
                setOnClickListener { handleKey(spec) }
            }
        }

    private fun handleKey(spec: KeySpec) {
        performKeyHaptic()

        spec.action?.invoke()
        if (spec.action != null) {
            return
        }

        if (spec.isLetterKey || spec.value.all(Char::isDigit)) {
            commitWordCharacter(spec.value)
        } else {
            commitDelimiter(spec.value)
        }
    }

    private fun commitWordCharacter(value: String) {
        commitText(value)

        if (shifted) {
            shifted = false
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
        updateAutoCapitalization()
        rebuildKeyboardRows()
        updateSuggestions()
    }

    private fun commitSpace() {
        performKeyHaptic()
        applyAutoCorrectionIfNeeded()
        commitText(" ")
        updateSuggestions()
    }

    private fun commitDelimiter(delimiter: String) {
        applyAutoCorrectionIfNeeded()
        commitText(delimiter)
        updateSuggestions()
    }

    private fun applyAutoCorrectionIfNeeded() {
        val activeWord = currentWordBeforeCursor()
        val correction = resolveAutoCorrection(activeWord)

        if (activeWord.isBlank() || correction == null) {
            return
        }

        currentInputConnection?.deleteSurroundingText(activeWord.length, 0)
        currentInputConnection?.commitText(correction.text, 1)
    }

    private fun backspace() {
        performKeyHaptic()
        currentInputConnection?.deleteSurroundingText(1, 0)
        updateSuggestions()
    }

    private fun handleEnter() {
        performKeyHaptic()
        applyAutoCorrectionIfNeeded()

        val editorInfo = currentInputEditorInfo
        if (editorInfo != null &&
            editorInfo.imeOptions and EditorInfo.IME_MASK_ACTION != EditorInfo.IME_ACTION_NONE &&
            editorInfo.inputType and InputType.TYPE_TEXT_FLAG_MULTI_LINE == 0
        ) {
            currentInputConnection?.performEditorAction(editorInfo.imeOptions and EditorInfo.IME_MASK_ACTION)
            clearSuggestions()
            updateAutoCapitalization()
            return
        }

        currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
        currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
        clearSuggestions()
        updateAutoCapitalization()
    }

    private fun updateSuggestions() {
        currentWord = currentWordBeforeCursor()
        updateAutoCapitalization()

        if (!currentWord.equals(lastSuggestionResultWord, ignoreCase = true)) {
            spellSuggestions = emptyList()
        }
        swipePreviewSuggestions = emptyList()
        refreshLocalSuggestionState()

        if (currentWord.length < 2) {
            refreshSuggestionStrip()
            return
        }

        lastSuggestionRequestWord = currentWord
        spellCheckerSession?.getSuggestions(TextInfo(currentWord), 4)
        spellCheckerSession?.getSentenceSuggestions(arrayOf(TextInfo(currentWord)), 4)
        refreshSuggestionStrip()
    }

    private fun refreshSuggestionStrip() {
        if (!::suggestionStrip.isInitialized) {
            return
        }

        suggestionStrip.removeAllViews()
        val suggestions = if (swipeInProgress && swipePreviewSuggestions.isNotEmpty()) {
            swipePreviewSuggestions
        } else {
            buildList {
                pendingAutoCorrection?.let(::add)
                if (currentWord.isNotBlank()) {
                    add(SuggestionCandidate(text = currentWord))
                }
                addAll(localSuggestions)
                addAll(appSuggestions)
                addAll(spellSuggestions)
            }
        }
            .filter { suggestion -> suggestion.text.isNotBlank() }
            .distinctBy { suggestion -> suggestion.text.lowercase(Locale.getDefault()) }
            .take(4)

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
                    primary = index == 0 && suggestion != SuggestionCandidate(currentWord),
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
            updateAutoCapitalization()
            return
        }

        val activeWord = currentWordBeforeCursor()
        if (activeWord.isNotBlank()) {
            currentInputConnection?.deleteSurroundingText(activeWord.length, 0)
        }
        currentInputConnection?.commitText(applyCaseToSuggestion(suggestion.text, activeWord), 1)
        clearSuggestions()
        updateSuggestions()
    }

    private fun clearSuggestions() {
        currentWord = currentWordBeforeCursor()
        spellSuggestions = emptyList()
        appSuggestions = emptyList()
        localSuggestions = emptyList()
        swipePreviewSuggestions = emptyList()
        pendingAutoCorrection = null
        refreshSuggestionStrip()
    }

    private fun currentWordBeforeCursor(): String {
        val textBeforeCursor = currentInputConnection?.getTextBeforeCursor(64, 0)?.toString().orEmpty()
        return textBeforeCursor.takeLastWhile { character ->
            character.isLetterOrDigit() || character == '\'' || character == '_'
        }
    }

    private fun updateAutoCapitalization() {
        val nextAutoCapitalization = shouldAutoCapitalize()
        if (autoCapitalization != nextAutoCapitalization) {
            autoCapitalization = nextAutoCapitalization
            if (::keysContainer.isInitialized && !symbolMode) {
                rebuildKeyboardRows()
            }
        }
    }

    private fun shouldAutoCapitalize(): Boolean {
        val inputType = currentInputEditorInfo?.inputType ?: 0
        if (inputType and InputType.TYPE_MASK_CLASS != InputType.TYPE_CLASS_TEXT) {
            return false
        }

        if ((currentInputConnection?.getCursorCapsMode(TextUtils.CAP_MODE_SENTENCES) ?: 0) != 0) {
            return true
        }

        val textBeforeCursor = currentInputConnection?.getTextBeforeCursor(200, 0)?.toString().orEmpty()
        if (textBeforeCursor.isBlank()) {
            return true
        }

        var index = textBeforeCursor.length - 1
        while (index >= 0 && textBeforeCursor[index].isWhitespace()) {
            index -= 1
        }
        while (index >= 0 && textBeforeCursor[index] in AUTO_CAP_TRAILING_CHARACTERS) {
            index -= 1
        }

        if (index < 0) {
            return true
        }

        return textBeforeCursor[index] in ".!?\n"
    }

    private fun isShiftActive(): Boolean = shifted || autoCapitalization

    private fun refreshLocalSuggestionState() {
        localSuggestions = if (currentWord.isBlank() || symbolMode) {
            emptyList()
        } else {
            wordEngine.predictiveSuggestions(currentWord, maxResults = 4)
                .map { suggestion ->
                    SuggestionCandidate(
                        text = applyCaseToSuggestion(suggestion, currentWord),
                    )
                }
        }
        pendingAutoCorrection = resolveAutoCorrection(currentWord)
    }

    private fun applyCaseToSuggestion(
        suggestion: String,
        sourceWord: String,
    ): String {
        if (sourceWord.isNotBlank()) {
            return when {
                sourceWord.all(Char::isUpperCase) -> suggestion.uppercase(Locale.getDefault())
                sourceWord.first().isUpperCase() -> suggestion.replaceFirstChar { character ->
                    character.titlecase(Locale.getDefault())
                }
                else -> suggestion.lowercase(Locale.getDefault())
            }
        }

        if (isShiftActive()) {
            return suggestion.replaceFirstChar { character ->
                character.titlecase(Locale.getDefault())
            }
        }

        return suggestion
    }

    private fun resolveAutoCorrection(sourceWord: String): SuggestionCandidate? {
        if (sourceWord.length < 2) {
            return null
        }

        val correction = wordEngine.autoCorrection(
            rawInput = sourceWord,
            externalCandidates = buildList {
                addAll(appSuggestions.map { suggestion -> suggestion.text })
                addAll(spellSuggestions.map { suggestion -> suggestion.text })
                addAll(localSuggestions.map { suggestion -> suggestion.text })
            },
        )

        val correctedText = correction
            ?.takeIf { candidate ->
                !candidate.equals(sourceWord, ignoreCase = true)
            }
            ?.let { candidate ->
                applyCaseToSuggestion(candidate, sourceWord)
            }
            ?: return null

        return SuggestionCandidate(text = correctedText)
    }

    private fun resolveSwipeWord(trace: String): String {
        val sourceWord = currentWordBeforeCursor()
        val engineSuggestions = wordEngine.swipeSuggestions(trace, maxResults = 4)
        val mergedCandidates = buildList {
            addAll(engineSuggestions)
            if (lastSuggestionResultWord.equals(trace, ignoreCase = true)) {
                addAll(spellSuggestions.map { suggestion -> suggestion.text })
            }
            addAll(localSuggestions.map { suggestion -> suggestion.text })
        }

        val bestCandidate = mergedCandidates
            .firstOrNull { candidate -> candidate.isNotBlank() }
            ?: trace.lowercase(Locale.getDefault())

        return applyCaseToSuggestion(bestCandidate, sourceWord)
    }

    private fun commitSwipeTrail() {
        val trace = swipeTrail.joinToString(separator = "")
        if (trace.length < 2) {
            return
        }

        performKeyHaptic()
        val activeWord = currentWordBeforeCursor()
        val resolvedWord = resolveSwipeWord(trace)
        if (activeWord.isNotBlank()) {
            currentInputConnection?.deleteSurroundingText(activeWord.length, 0)
        }
        commitText(resolvedWord)

        if (shifted) {
            shifted = false
        }

        swipeTrail.clear()
        swipePreviewSuggestions = emptyList()
        updateSuggestions()
    }

    private fun findLetterKeyTarget(rawX: Float, rawY: Float): KeyTouchTarget? {
        val hitRect = Rect()
        return letterKeyTargets.firstOrNull { target ->
            target.button.getGlobalVisibleRect(hitRect) &&
                hitRect.contains(rawX.toInt(), rawY.toInt())
        }
    }

    private fun addSwipeValue(value: String) {
        if (swipeTrail.lastOrNull() != value) {
            swipeTrail += value
            swipePreviewSuggestions = wordEngine.swipeSuggestions(
                rawTrace = swipeTrail.joinToString(separator = ""),
                maxResults = 4,
            ).map { suggestion ->
                SuggestionCandidate(
                    text = applyCaseToSuggestion(suggestion, currentWordBeforeCursor()),
                )
            }
            refreshSuggestionStrip()
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

    private inner class LetterSwipeTouchListener(
        private val spec: KeySpec,
    ) : View.OnTouchListener {
        override fun onTouch(view: View, event: MotionEvent): Boolean =
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    swipeInProgress = false
                    swipeTrail.clear()
                    swipeStartX = event.rawX
                    swipeStartY = event.rawY
                    addSwipeValue(spec.value.lowercase(Locale.getDefault()))
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    findLetterKeyTarget(event.rawX, event.rawY)?.let { target ->
                        addSwipeValue(target.value)
                    }

                    if (!swipeInProgress &&
                        (abs(event.rawX - swipeStartX) > dp(10) ||
                            abs(event.rawY - swipeStartY) > dp(10) ||
                            swipeTrail.size > 1)
                    ) {
                        swipeInProgress = true
                    }
                    true
                }

                MotionEvent.ACTION_UP -> {
                    if (swipeInProgress && swipeTrail.size > 1) {
                        commitSwipeTrail()
                    } else {
                        handleKey(spec)
                    }
                    swipeTrail.clear()
                    swipePreviewSuggestions = emptyList()
                    swipeInProgress = false
                    true
                }

                MotionEvent.ACTION_CANCEL -> {
                    swipeTrail.clear()
                    swipePreviewSuggestions = emptyList()
                    refreshSuggestionStrip()
                    swipeInProgress = false
                    true
                }

                else -> false
            }
    }

    private data class KeySpec(
        val value: String,
        val isSpecial: Boolean,
        val weight: Float,
        val highlighted: Boolean = false,
        val isSpaceKey: Boolean = false,
        val action: (() -> Unit)? = null,
    ) {
        val isLetterKey: Boolean
            get() = !isSpecial && value.length == 1 && value[0].isLetter()
    }

    private data class SuggestionCandidate(
        val text: String,
        val completion: CompletionInfo? = null,
    )

    private data class KeyTouchTarget(
        val button: Button,
        val value: String,
        val displayValue: String,
    )

    private companion object {
        val AUTO_CAP_TRAILING_CHARACTERS = setOf('"', '\'', ')', ']', '}', '»', '”')
    }

    private fun textKey(value: String): KeySpec =
        KeySpec(
            value = value,
            isSpecial = false,
            weight = 1f,
        )

    private fun actionKey(
        value: String,
        weight: Float = 1f,
        highlighted: Boolean = false,
        action: () -> Unit,
    ): KeySpec =
        KeySpec(
            value = value,
            isSpecial = true,
            weight = weight,
            highlighted = highlighted,
            action = action,
        )

    private fun spaceKey(weight: Float): KeySpec =
        KeySpec(
            value = "Space",
            isSpecial = true,
            weight = weight,
            isSpaceKey = true,
        )

    private fun moveCursor(direction: Int) {
        val keyCode = if (direction < 0) KeyEvent.KEYCODE_DPAD_LEFT else KeyEvent.KEYCODE_DPAD_RIGHT
        currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        currentInputConnection?.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
    }

    private inner class SpaceCursorTouchListener : View.OnTouchListener {
        private val cursorThreshold = dp(18)

        override fun onTouch(view: View, event: MotionEvent): Boolean =
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    spaceCursorMode = false
                    spaceTouchStartX = event.rawX
                    spaceCursorAccumulator = 0f
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - spaceTouchStartX
                    if (!spaceCursorMode && abs(deltaX) > cursorThreshold) {
                        spaceCursorMode = true
                        spaceCursorAccumulator = 0f
                    }
                    if (spaceCursorMode) {
                        spaceCursorAccumulator += event.rawX - spaceTouchStartX
                        spaceTouchStartX = event.rawX
                        while (abs(spaceCursorAccumulator) >= cursorThreshold) {
                            val direction = if (spaceCursorAccumulator > 0) 1 else -1
                            moveCursor(direction)
                            performKeyHaptic()
                            spaceCursorAccumulator -= direction * cursorThreshold
                        }
                    }
                    true
                }

                MotionEvent.ACTION_UP -> {
                    if (!spaceCursorMode) {
                        commitSpace()
                    }
                    spaceCursorMode = false
                    true
                }

                MotionEvent.ACTION_CANCEL -> {
                    spaceCursorMode = false
                    true
                }

                else -> false
            }
    }
}
