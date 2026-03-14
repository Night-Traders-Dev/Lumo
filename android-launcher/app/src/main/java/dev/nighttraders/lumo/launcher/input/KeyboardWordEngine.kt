package dev.nighttraders.lumo.launcher.input

import android.content.Context
import java.util.Locale
import kotlin.math.abs
import kotlin.math.min

internal class KeyboardWordEngine private constructor(
    private val words: List<DictionaryWord>,
    private val knownWords: Set<String>,
    private val prefixBuckets: Map<String, List<DictionaryWord>>,
    private val firstLetterBuckets: Map<Char, List<DictionaryWord>>,
) {
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

        val bucket = firstLetterBuckets[trace.first()] ?: words
        return bucket
            .asSequence()
            .filter { candidate ->
                candidate.value.length >= 2 &&
                    abs(candidate.value.length - trace.length) <= 8
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
        val limit = maxOf(1, trace.length / 2 + 1)
        if (distance > limit) {
            return Int.MAX_VALUE
        }

        var score = distance * 18 + abs(candidate.value.length - trace.length) * 3 + frequencyPenalty(candidate.rank)
        if (candidate.value.firstOrNull() != trace.firstOrNull()) {
            score += 40
        }
        if (candidate.value.lastOrNull() != trace.lastOrNull()) {
            score += 18
        }
        if (!isSubsequence(signature, trace)) {
            score += 22
        }
        if (!isSubsequence(trace, signature)) {
            score += 14
        }
        return score
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

        private fun load(context: Context): KeyboardWordEngine {
            val commonWords = COMMON_WORDS
                .lineSequence()
                .map(String::trim)
                .filter(String::isNotBlank)
                .map { word -> word.lowercase(Locale.getDefault()) }
                .toList()

            val mergedWords = linkedSetOf<String>()
            mergedWords += commonWords
            mergedWords += loadAssetWords(context)

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
                knownWords = dictionaryWords.mapTo(linkedSetOf()) { word -> word.value },
                prefixBuckets = prefixBuckets,
                firstLetterBuckets = firstLetterBuckets,
            )
        }

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
