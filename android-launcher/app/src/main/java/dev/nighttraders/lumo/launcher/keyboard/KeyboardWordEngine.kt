package dev.nighttraders.lumo.launcher.keyboard

import android.content.Context
import android.provider.UserDictionary
import java.util.Locale
import kotlin.math.abs
import kotlin.math.min

internal class KeyboardWordEngine private constructor(
    private val words: List<DictionaryWord>,
    private val knownWords: MutableSet<String>,
    private val prefixBuckets: MutableMap<String, MutableList<DictionaryWord>>,
    private val firstLetterBuckets: MutableMap<Char, MutableList<DictionaryWord>>,
) {
    /** Hot-add a learned word so it's immediately available for suggestions. */
    fun addLearnedWord(rawWord: String) {
        val word = rawWord.trim().lowercase(Locale.getDefault())
            .filter { it.isLetter() || it == '\'' }
        if (word.length < 2 || word in knownWords) return
        knownWords.add(word)
        val dw = DictionaryWord(value = word, rank = 1) // highest priority
        (1..min(3, word.length)).forEach { prefixLen ->
            prefixBuckets.getOrPut(word.take(prefixLen), ::mutableListOf).add(0, dw)
        }
        word.firstOrNull()?.let { ch ->
            firstLetterBuckets.getOrPut(ch, ::mutableListOf).add(0, dw)
        }
    }

    fun predictiveSuggestions(rawInput: String, maxResults: Int = 4): List<String> {
        val input = normalize(rawInput)
        if (input.isBlank()) {
            return emptyList()
        }

        val candidates = linkedSetOf<DictionaryWord>()
        prefixBuckets[input.take(min(3, input.length))]?.let(candidates::addAll)
        if (input.length >= 2) {
            prefixBuckets[input.take(1)]?.take(256)?.let(candidates::addAll)
        }

        return candidates
            .asSequence()
            .filter { candidate ->
                abs(candidate.value.length - input.length) <= 8
            }
            .mapNotNull { candidate ->
                val score = predictionScore(input, candidate)
                if (score == Int.MAX_VALUE) {
                    null
                } else {
                    ScoredWord(candidate.value, score)
                }
            }
            .sortedWith(compareBy<ScoredWord> { it.score }.thenBy { it.value.length }.thenBy { it.value })
            .map { it.value }
            .distinct()
            .take(maxResults)
            .toList()
    }

    fun autoCorrection(
        rawInput: String,
        externalCandidates: List<String> = emptyList(),
    ): String? {
        val input = normalize(rawInput)
        if (input.length < 2 || isKnownWord(input)) {
            return null
        }

        val threshold = correctionThreshold(input.length)
        val candidates = linkedSetOf<String>()
        candidates += predictiveSuggestions(input, 8)
        firstLetterBuckets[input.first()]?.asSequence()
            ?.filter { candidate ->
                abs(candidate.value.length - input.length) <= threshold &&
                    candidate.value.length >= 2
            }
            ?.take(768)
            ?.mapTo(candidates) { candidate -> candidate.value }

        externalCandidates
            .map(::normalize)
            .filter { it.isNotBlank() }
            .forEach(candidates::add)

        return candidates
            .asSequence()
            .filterNot { candidate -> candidate == input || candidate.startsWith(input) }
            .mapNotNull { candidate ->
                val score = correctionScore(input, candidate)
                if (score == Int.MAX_VALUE) {
                    null
                } else {
                    ScoredWord(candidate, score)
                }
            }
            .sortedWith(compareBy<ScoredWord> { it.score }.thenBy { it.value.length }.thenBy { it.value })
            .map { it.value }
            .firstOrNull()
    }

    fun swipeSuggestions(rawTrace: String, maxResults: Int = 4): List<String> {
        val trace = collapseRepeats(normalize(rawTrace))
        if (trace.length < 2) {
            return emptyList()
        }

        val firstChar = trace.first()
        val lastChar = trace.last()

        // Search primary bucket (matching first letter) and nearby-letter bucket
        val primaryBucket = firstLetterBuckets[firstChar] ?: emptyList()
        val nearbyFirstChars = ADJACENT_KEYS[firstChar].orEmpty()
        val nearbyBuckets = nearbyFirstChars.flatMap { c -> firstLetterBuckets[c].orEmpty() }
        val combinedBucket = primaryBucket + nearbyBuckets

        return combinedBucket
            .asSequence()
            .filter { candidate ->
                candidate.value.length >= 2 &&
                    abs(candidate.value.length - trace.length) <= 10
            }
            .mapNotNull { candidate ->
                val score = swipeScore(trace, candidate)
                if (score == Int.MAX_VALUE) {
                    null
                } else {
                    ScoredWord(candidate.value, score)
                }
            }
            .sortedWith(compareBy<ScoredWord> { it.score }.thenBy { it.value.length }.thenBy { it.value })
            .map { it.value }
            .distinct()
            .take(maxResults)
            .toList()
    }

    fun isKnownWord(rawWord: String): Boolean = normalize(rawWord) in knownWords

    private fun predictionScore(input: String, candidate: DictionaryWord): Int {
        if (!candidate.value.startsWith(input)) {
            val distance = damerauLevenshtein(input, candidate.value)
            if (distance > correctionThreshold(input.length)) {
                return Int.MAX_VALUE
            }
            return 160 + distance * 20 + abs(candidate.value.length - input.length) * 5 + frequencyPenalty(candidate.rank)
        }

        return candidate.value.length - input.length + frequencyPenalty(candidate.rank)
    }

    private fun correctionScore(input: String, candidate: String): Int {
        val distance = damerauLevenshtein(input, candidate)
        val threshold = correctionThreshold(input.length)
        if (distance == 0 || distance > threshold || abs(candidate.length - input.length) > threshold) {
            return Int.MAX_VALUE
        }

        var score = distance * 24 + abs(candidate.length - input.length) * 6
        if (candidate.firstOrNull() != input.firstOrNull()) {
            score += 18
        }
        if (candidate.lastOrNull() != input.lastOrNull()) {
            score += 10
        }
        score += wordsByValue(candidate)?.let { frequencyPenalty(it.rank) } ?: 24
        return score
    }

    private fun swipeScore(trace: String, candidate: DictionaryWord): Int {
        val signature = collapseRepeats(candidate.value)
        val distance = damerauLevenshtein(trace, signature)
        // More forgiving limit — swipe traces often pick up extra keys or miss some
        val limit = maxOf(2, (trace.length * 2) / 3 + 2)
        if (distance > limit) {
            return Int.MAX_VALUE
        }

        var score = distance * 12 + abs(candidate.value.length - trace.length) * 2 + frequencyPenalty(candidate.rank)

        // First/last letter matching is very important for swipe
        val firstMatch = candidate.value.firstOrNull() == trace.firstOrNull()
        val firstAdjacent = !firstMatch && isAdjacentKey(candidate.value.firstOrNull(), trace.firstOrNull())
        if (!firstMatch && !firstAdjacent) {
            score += 50
        } else if (firstAdjacent) {
            score += 10
        } else {
            score -= 4 // reward exact first letter match
        }

        val lastMatch = candidate.value.lastOrNull() == trace.lastOrNull()
        val lastAdjacent = !lastMatch && isAdjacentKey(candidate.value.lastOrNull(), trace.lastOrNull())
        if (!lastMatch && !lastAdjacent) {
            score += 25
        } else if (lastAdjacent) {
            score += 6
        } else {
            score -= 4 // reward exact last letter match
        }

        // Check if the candidate's key signature is a subsequence of the trace
        val collapsed = collapseRepeats(candidate.value)
        if (isSubsequence(collapsed, trace)) {
            // Strong reward — all key letters appear in order within the trace
            score -= 10
        } else if (!isSubsequence(signature, trace)) {
            score += 16
        }

        // Check how many of the candidate's interior letters appear as adjacent keys in the trace
        val interiorMatchRatio = interiorKeyMatchRatio(candidate.value, trace)
        score -= (interiorMatchRatio * 8).toInt()

        // Length similarity bonus — prefer candidates close in length to the word
        if (abs(candidate.value.length - trace.length) <= 1) {
            score -= 3
        }

        return score
    }

    /**
     * Calculates what fraction of a word's interior letters (not first/last)
     * appear in the trace, either as exact matches or adjacent keys.
     * This helps swipe accuracy by rewarding words whose key path
     * aligns well with the actual trace.
     */
    private fun interiorKeyMatchRatio(word: String, trace: String): Float {
        if (word.length <= 2) return 1f
        val interior = word.substring(1, word.length - 1)
        if (interior.isEmpty()) return 1f
        var matched = 0
        var traceIdx = 1 // skip first char
        for (ch in interior) {
            var found = false
            for (i in traceIdx until trace.length) {
                if (trace[i] == ch || isAdjacentKey(trace[i], ch)) {
                    traceIdx = i + 1
                    found = true
                    break
                }
            }
            if (found) matched++
        }
        return matched.toFloat() / interior.length
    }

    private fun isAdjacentKey(a: Char?, b: Char?): Boolean {
        if (a == null || b == null) return false
        return ADJACENT_KEYS[a]?.contains(b) == true
    }

    private fun wordsByValue(value: String): DictionaryWord? {
        val normalized = normalize(value)
        return prefixBuckets[normalized.take(1)]?.firstOrNull { candidate -> candidate.value == normalized }
    }

    private fun frequencyPenalty(rank: Int): Int = rank / 24

    private fun correctionThreshold(length: Int): Int =
        when {
            length <= 3 -> 1
            length <= 6 -> 2
            else -> 3
        }

    private fun normalize(raw: String): String =
        raw
            .trim()
            .lowercase(Locale.getDefault())
            .filter { character -> character.isLetter() || character == '\'' }

    private fun collapseRepeats(raw: String): String {
        val builder = StringBuilder(raw.length)
        raw.forEach { character ->
            if (builder.isEmpty() || builder.last() != character) {
                builder.append(character)
            }
        }
        return builder.toString()
    }

    private fun isSubsequence(needle: String, haystack: String): Boolean {
        if (needle.isBlank()) {
            return true
        }

        var offset = 0
        needle.forEach { character ->
            val index = haystack.indexOf(character, offset)
            if (index == -1) {
                return false
            }
            offset = index + 1
        }
        return true
    }

    private fun damerauLevenshtein(left: String, right: String): Int {
        if (left == right) {
            return 0
        }
        if (left.isEmpty()) {
            return right.length
        }
        if (right.isEmpty()) {
            return left.length
        }

        val matrix = Array(left.length + 1) { rowIndex ->
            IntArray(right.length + 1) { columnIndex ->
                when {
                    rowIndex == 0 -> columnIndex
                    columnIndex == 0 -> rowIndex
                    else -> 0
                }
            }
        }

        for (row in 1..left.length) {
            for (column in 1..right.length) {
                val substitutionCost = if (left[row - 1] == right[column - 1]) 0 else 1
                matrix[row][column] = min(
                    min(
                        matrix[row - 1][column] + 1,
                        matrix[row][column - 1] + 1,
                    ),
                    matrix[row - 1][column - 1] + substitutionCost,
                )

                if (row > 1 &&
                    column > 1 &&
                    left[row - 1] == right[column - 2] &&
                    left[row - 2] == right[column - 1]
                ) {
                    matrix[row][column] = min(matrix[row][column], matrix[row - 2][column - 2] + substitutionCost)
                }
            }
        }

        return matrix[left.length][right.length]
    }

    companion object {
        @Volatile
        private var instance: KeyboardWordEngine? = null

        fun get(context: Context): KeyboardWordEngine =
            instance ?: synchronized(this) {
                instance ?: load(context.applicationContext).also { engine ->
                    instance = engine
                }
            }

        fun reload(context: Context): KeyboardWordEngine =
            synchronized(this) {
                load(context.applicationContext).also { engine ->
                    instance = engine
                }
            }

        private fun load(context: Context): KeyboardWordEngine {
            val learnedWords = LearnedWordStore.get(context).allWords()

            val commonWords = COMMON_WORDS
                .lineSequence()
                .map(String::trim)
                .filter(String::isNotBlank)
                .map { word -> word.lowercase(Locale.getDefault()) }
                .toList()

            // Learned words get highest priority (inserted first)
            val mergedWords = linkedSetOf<String>()
            mergedWords += learnedWords
            mergedWords += commonWords
            mergedWords += loadAssetWords(context)
            mergedWords += loadUserDictionaryWords(context)

            val dictionaryWords = mergedWords
                .mapIndexed { index, word ->
                    DictionaryWord(
                        value = word,
                        rank = index + 1,
                    )
                }

            val prefixBuckets = mutableMapOf<String, MutableList<DictionaryWord>>()
            val firstLetterBuckets = mutableMapOf<Char, MutableList<DictionaryWord>>()

            dictionaryWords.forEach { word ->
                (1..min(3, word.value.length)).forEach { prefixLength ->
                    prefixBuckets.getOrPut(word.value.take(prefixLength), ::mutableListOf).add(word)
                }
                word.value.firstOrNull()?.let { firstLetter ->
                    firstLetterBuckets.getOrPut(firstLetter, ::mutableListOf).add(word)
                }
            }

            return KeyboardWordEngine(
                words = dictionaryWords,
                knownWords = dictionaryWords.mapTo(linkedSetOf()) { word -> word.value }.toMutableSet(),
                prefixBuckets = prefixBuckets,
                firstLetterBuckets = firstLetterBuckets,
            )
        }

        private fun loadUserDictionaryWords(context: Context): List<String> =
            runCatching {
                val cursor = context.contentResolver.query(
                    UserDictionary.Words.CONTENT_URI,
                    arrayOf(UserDictionary.Words.WORD),
                    null,
                    null,
                    UserDictionary.Words.FREQUENCY + " DESC",
                )
                cursor?.use {
                    val wordIndex = it.getColumnIndex(UserDictionary.Words.WORD)
                    if (wordIndex < 0) return@use emptyList()
                    buildList {
                        while (it.moveToNext()) {
                            it.getString(wordIndex)
                                ?.trim()
                                ?.takeIf(String::isNotBlank)
                                ?.lowercase(Locale.getDefault())
                                ?.let(::add)
                        }
                    }
                } ?: emptyList()
            }.getOrDefault(emptyList())

        private fun loadAssetWords(context: Context): List<String> =
            runCatching {
                context.assets.open(DICTIONARY_ASSET).bufferedReader().useLines { lines ->
                    lines
                        .map(String::trim)
                        .filter(String::isNotBlank)
                        .map { word ->
                            word.lowercase(Locale.getDefault())
                        }
                        .toList()
                }
            }.getOrDefault(emptyList())

        private const val DICTIONARY_ASSET = "ime-words.txt"

        // QWERTY adjacency map for spatial-aware swipe matching
        val ADJACENT_KEYS: Map<Char, Set<Char>> = mapOf(
            'q' to setOf('w', 'a'),
            'w' to setOf('q', 'e', 'a', 's'),
            'e' to setOf('w', 'r', 's', 'd'),
            'r' to setOf('e', 't', 'd', 'f'),
            't' to setOf('r', 'y', 'f', 'g'),
            'y' to setOf('t', 'u', 'g', 'h'),
            'u' to setOf('y', 'i', 'h', 'j'),
            'i' to setOf('u', 'o', 'j', 'k'),
            'o' to setOf('i', 'p', 'k', 'l'),
            'p' to setOf('o', 'l'),
            'a' to setOf('q', 'w', 's', 'z'),
            's' to setOf('a', 'w', 'e', 'd', 'z', 'x'),
            'd' to setOf('s', 'e', 'r', 'f', 'x', 'c'),
            'f' to setOf('d', 'r', 't', 'g', 'c', 'v'),
            'g' to setOf('f', 't', 'y', 'h', 'v', 'b'),
            'h' to setOf('g', 'y', 'u', 'j', 'b', 'n'),
            'j' to setOf('h', 'u', 'i', 'k', 'n', 'm'),
            'k' to setOf('j', 'i', 'o', 'l', 'm'),
            'l' to setOf('k', 'o', 'p'),
            'z' to setOf('a', 's', 'x'),
            'x' to setOf('z', 's', 'd', 'c'),
            'c' to setOf('x', 'd', 'f', 'v'),
            'v' to setOf('c', 'f', 'g', 'b'),
            'b' to setOf('v', 'g', 'h', 'n'),
            'n' to setOf('b', 'h', 'j', 'm'),
            'm' to setOf('n', 'j', 'k'),
        )

        private const val COMMON_WORDS = """
            the
            be
            to
            of
            and
            a
            in
            that
            have
            i
            it
            for
            not
            on
            with
            he
            as
            you
            do
            at
            this
            but
            his
            by
            from
            they
            we
            say
            her
            she
            or
            an
            will
            my
            one
            all
            would
            there
            their
            what
            so
            up
            out
            if
            about
            who
            get
            which
            go
            me
            when
            make
            can
            like
            time
            no
            just
            him
            know
            take
            people
            into
            year
            your
            good
            some
            could
            them
            see
            other
            than
            then
            now
            look
            only
            come
            its
            over
            think
            also
            back
            after
            use
            two
            how
            our
            work
            first
            well
            way
            even
            new
            want
            because
            any
            these
            give
            day
            most
            us
            is
            are
            was
            were
            am
            being
            been
            i'm
            you're
            we're
            they're
            i've
            you've
            we've
            don't
            doesn't
            didn't
            can't
            couldn't
            won't
            shouldn't
            yes
            yeah
            okay
            ok
            hello
            hey
            thanks
            thank
            please
            sorry
            tonight
            tomorrow
            morning
            afternoon
            evening
            home
            phone
            text
            message
            call
            calling
            sent
            send
            see
            later
            now
            really
            right
            going
            gotta
            got
            gotcha
            sure
            maybe
            definitely
            absolutely
            awesome
            cool
            great
            love
            loved
            loving
            please
            need
            needed
            needs
            needed
            make
            making
            made
            done
            doing
            where
            when
            why
            who
            whose
            here's
            let's
            that's
            what's
            where's
            we're
            i'll
            i'd
            you'll
            you'd
            he'll
            she'd
            they'll
            enough
            around
            before
            after
            again
            always
            never
            maybe
            probably
            already
            still
            every
            everything
            everyone
            somebody
            someone
            something
            anything
            anyone
            nothing
            today
            yesterday
            weekend
            monday
            tuesday
            wednesday
            thursday
            friday
            saturday
            sunday
        """
    }

    private data class DictionaryWord(
        val value: String,
        val rank: Int,
    )

    private data class ScoredWord(
        val value: String,
        val score: Int,
    )
}
