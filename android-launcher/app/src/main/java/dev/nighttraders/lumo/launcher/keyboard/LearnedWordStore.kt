package dev.nighttraders.lumo.launcher.keyboard

import android.content.Context
import java.io.File
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Persists words the user types frequently to internal storage.
 * Words are learned when committed via space/enter/punctuation.
 * Frequency is tracked — higher frequency = higher suggestion priority.
 */
internal class LearnedWordStore private constructor(
    private val file: File,
) {
    private val words = ConcurrentHashMap<String, Int>()

    init {
        loadFromDisk()
    }

    /** Record that the user typed this word. Increments frequency count. */
    fun learn(rawWord: String) {
        val word = normalize(rawWord)
        if (word.length < 2 || word.length > 30) return
        // Don't learn single chars, numbers-only, or very long strings
        if (word.all(Char::isDigit)) return

        val newFreq = (words[word] ?: 0) + 1
        words[word] = newFreq
        saveToDisk()
    }

    /** All learned words sorted by frequency (most frequent first). */
    fun allWords(): List<String> =
        words.entries
            .sortedByDescending { it.value }
            .map { it.key }

    /** Check if a word has been learned. */
    fun isLearned(rawWord: String): Boolean =
        words.containsKey(normalize(rawWord))

    /** Remove a learned word. */
    fun forget(rawWord: String) {
        val word = normalize(rawWord)
        if (words.remove(word) != null) {
            saveToDisk()
        }
    }

    /** Clear all learned words. */
    fun clear() {
        words.clear()
        saveToDisk()
    }

    private fun normalize(raw: String): String =
        raw.trim().lowercase(Locale.getDefault())
            .filter { it.isLetter() || it == '\'' }

    private fun loadFromDisk() {
        if (!file.exists()) return
        runCatching {
            file.readLines().forEach { line ->
                val parts = line.split('\t', limit = 2)
                if (parts.size == 2) {
                    val word = parts[0]
                    val freq = parts[1].toIntOrNull() ?: 1
                    if (word.isNotBlank()) {
                        words[word] = freq
                    }
                }
            }
        }
    }

    private fun saveToDisk() {
        runCatching {
            file.parentFile?.mkdirs()
            file.bufferedWriter().use { writer ->
                words.entries
                    .sortedByDescending { it.value }
                    .forEach { (word, freq) ->
                        writer.write("$word\t$freq")
                        writer.newLine()
                    }
            }
        }
    }

    companion object {
        @Volatile
        private var instance: LearnedWordStore? = null

        fun get(context: Context): LearnedWordStore =
            instance ?: synchronized(this) {
                instance ?: LearnedWordStore(
                    File(context.filesDir, "learned_words.tsv"),
                ).also { instance = it }
            }
    }
}
