package org.wpfy.carlauncher.feature.navigation.search

import java.text.Normalizer
import java.util.Locale

/**
 * The canonical name form shared by runtime queries and the offline builder.
 *
 * It applies Unicode NFKD, removes combining marks, lowercases with ROOT, turns
 * non-letter/digit runs into one space, and trims the result.
 */
fun normalizePoiName(value: String): String {
    val decomposed = Normalizer.normalize(value, Normalizer.Form.NFKD)
        .lowercase(Locale.ROOT)
    val normalized = StringBuilder(decomposed.length)
    var pendingSpace = false
    var index = 0
    while (index < decomposed.length) {
        val character = decomposed.codePointAt(index)
        when {
            Character.getType(character) == Character.NON_SPACING_MARK.toInt() ||
                Character.getType(character) == Character.COMBINING_SPACING_MARK.toInt() ||
                Character.getType(character) == Character.ENCLOSING_MARK.toInt() -> Unit
            Character.isLetterOrDigit(character) -> {
                if (pendingSpace && normalized.isNotEmpty()) normalized.append(' ')
                normalized.appendCodePoint(character)
                pendingSpace = false
            }
            normalized.isNotEmpty() -> pendingSpace = true
        }
        index += Character.charCount(character)
    }
    return normalized.toString()
}

fun normalizedPoiTokens(value: String): List<String> {
    return normalizePoiName(value).split(' ').filter(String::isNotEmpty)
}
