package com.anomalyzed.simplespeechkeyboard.engine

import java.util.Locale

/**
 * Utility class for post-processing and cleaning text transcribed by Whisper.
 *
 * Performs:
 * - Removal of common Whisper hallucinations (e.g. "Subtitles by...", "[BLANK_AUDIO]").
 * - Removal of vocal hesitation fillers ("ehm", "uhm").
 * - Capitalization of sentence start.
 * - Normalization of spaces and punctuation spacing.
 */
object TextPostProcessor {

    private val hallucinationPatterns = listOf(
        Regex("""(?i)\[\s*(blank_audio|music|silence|applause|laughter)\s*\]"""),
        Regex("""(?i)\(\s*(musica|applausi|silenzio)\s*\)"""),
        Regex("""(?i)sottotitoli\s+creati\s+da.*""", RegexOption.IGNORE_CASE),
        Regex("""(?i)trascrizione\s+a\s+cura\s+di.*""", RegexOption.IGNORE_CASE),
        Regex("""(?i)subtitles\s+by.*""", RegexOption.IGNORE_CASE),
        Regex("""(?i)thanks?\s+for\s+watching.*""", RegexOption.IGNORE_CASE),
        Regex("""(?i)grazie\s+per\s+l['']ascolto.*""", RegexOption.IGNORE_CASE),
        Regex("""(?i)iscriviti\s+al\s+canale.*""", RegexOption.IGNORE_CASE)
    )

    private val fillerWordsRegex = Regex("""(?i)\b(ehm|uhm|mmm|ahh?|ehh?)\b""")

    /**
     * Cleans and formats the raw transcript string.
     */
    fun process(rawText: String): String {
        if (rawText.isBlank()) return ""

        var cleaned = rawText.trim()

        // 1. Remove hallucination patterns
        for (pattern in hallucinationPatterns) {
            cleaned = pattern.replace(cleaned, "")
        }

        // 2. Remove filler words
        cleaned = fillerWordsRegex.replace(cleaned, "")

        // 3. Fix multiple spaces and trim
        cleaned = cleaned.replace(Regex("""\s+"""), " ").trim()

        if (cleaned.isBlank()) return ""

        // 4. Ensure space after punctuation (if followed by a letter)
        cleaned = cleaned.replace(Regex("""([.,!?;:])([a-zA-ZàèéìòùÀÈÉÌÒÙ])""")) { matchResult ->
            "${matchResult.groupValues[1]} ${matchResult.groupValues[2]}"
        }

        // 5. Capitalize first letter if needed
        return if (cleaned.isNotEmpty() && cleaned[0].isLowerCase()) {
            cleaned.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        } else {
            cleaned
        }
    }
}
