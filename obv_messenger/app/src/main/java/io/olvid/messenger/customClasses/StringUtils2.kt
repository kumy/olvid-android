/*
 *  Olvid for Android
 *  Copyright © 2019-2026 Olvid SAS
 *
 *  This file is part of Olvid for Android.
 *
 *  Olvid is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License, version 3,
 *  as published by the Free Software Foundation.
 *
 *  Olvid is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with Olvid.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.olvid.messenger.customClasses

import android.content.Context
import android.icu.lang.UCharacter
import android.os.Build
import android.text.SpannableString
import android.text.Spanned
import android.text.format.Formatter
import android.text.style.URLSpan
import android.text.util.Linkify
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation.Url
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.text.util.LinkifyCompat
import androidx.core.util.PatternsCompat
import androidx.emoji2.text.EmojiCompat
import androidx.emoji2.text.EmojiSpan
import io.olvid.engine.engine.types.JsonIdentityDetails
import io.olvid.messenger.App
import io.olvid.messenger.AppSingleton
import io.olvid.messenger.R
import io.olvid.messenger.customClasses.StringUtils.isEmojiCodepoint
import io.olvid.messenger.customClasses.StringUtils.unAccentPattern
import io.olvid.messenger.customClasses.StringUtils2.Companion.normalize
import io.olvid.messenger.settings.SettingsActivity
import java.text.Normalizer
import java.util.BitSet
import java.util.Locale
import kotlin.math.roundToLong

fun String.linkify(context: Context): AnnotatedString {
    val spannableString = SpannableString(this)
    LinkifyCompat.addLinks(
        spannableString,
        PatternsCompat.WEB_URL,
        "https://",
        { s: CharSequence?, start: Int, _: Int ->
            s?.let {
                (start == 0) || (it[start - 1] != '@')
            } ?: false
        },
        null
    )
    return buildAnnotatedString {
        append(spannableString)
        spannableString.getSpans(0, spannableString.length, URLSpan::class.java).forEach {
            addLink(
                url = Url(
                    url = it.url,
                    styles = TextLinkStyles(
                        style =
                            SpanStyle(
                                color = Color(
                                    ContextCompat.getColor(
                                        context,
                                        R.color.olvid_gradient_light
                                    )
                                ),
                                textDecoration = TextDecoration.Underline
                            )
                    ),
                    linkInteractionListener = { annotation ->
                        (annotation as? Url)?.let { url ->
                            App.openLink(
                                context,
                                url.url.toUri()
                            )
                        }
                    }
                ),
                start = spannableString.getSpanStart(it),
                end = spannableString.getSpanEnd(it),
            )
        }
    }
}


class StringUtils2 {
    companion object {
        val whitespaceRegex = Regex("\\s+")

        /**
         * Rank of [source] against a search [filter], for sorting search results so that
         * entries whose words start with the typed filter come first: the index of the first
         * whitespace-separated word of [source] (unaccented) starting with one of the
         * (unaccented) filter tokens, followed by [Int.MAX_VALUE]/2 + the index of the
         * first whitespace-separated word of [source] (unaccented) containing one of the filter
         * tokens, or [Int.MAX_VALUE] when no word starts/contains the filter. Sort ascending:
         * sorts are stable, so equally ranked entries keep their original relative order.
         */
        @JvmStatic
        fun searchMatchRank(source: String, filter: String?): Int {
            val tokens = filter?.trim()?.split(whitespaceRegex)
                ?.filter { it.isNotEmpty() }
                ?.map { StringUtils.unAccent(it) }
            if (tokens.isNullOrEmpty()) {
                return Int.MAX_VALUE
            }
            return StringUtils.unAccent(source).trim().split(whitespaceRegex)
                .let { words ->
                    val startIndex = words.indexOfFirst { word -> tokens.any { word.startsWith(it) } }
                    if (startIndex != -1) {
                        return@let startIndex
                    }
                    val middleIndex = words.indexOfFirst { word -> tokens.any { word.contains(it) } }
                    if (middleIndex != -1) {
                        return@let Int.MAX_VALUE / 2 + middleIndex
                    }
                    return@let Int.MAX_VALUE
                }
        }

        fun getLink(s: String?): Pair<String, String?>? {
            s?.let { nonNullS ->
                val source = SpannableString(nonNullS)
                val urlSpan = source
                    .apply {
                        LinkifyCompat.addLinks(
                            this,
                            PatternsCompat.WEB_URL,
                            "https://",
                            { s: CharSequence?, start: Int, _: Int ->
                                s?.let {
                                    (start == 0) || (it[start - 1] != '@')
                                } ?: false
                            },
                            null
                        )
                    }
                    .getSpans(0, nonNullS.length, URLSpan::class.java).firstOrNull()
                return urlSpan?.let {
                    Pair(
                        nonNullS.subSequence(source.getSpanStart(it), source.getSpanEnd(it)).toString(),
                        it.url
                    )
                }
            }
            return null
        }

        fun getExtensionFromFilename(fileName: String): String? {
            val pos = fileName.lastIndexOf('.')
            if (pos != -1) {
                return fileName.substring(pos + 1)
            }
            return null
        }


        fun CharSequence.normalize(): String {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                UCharacter.toLowerCase(
                    unAccentPattern.matcher(
                        Normalizer.normalize(
                            this,
                            Normalizer.Form.NFKD
                        )
                    ).replaceAll("")
                )
            } else {
                unAccentPattern.matcher(Normalizer.normalize(this, Normalizer.Form.NFKD))
                    .replaceAll("").lowercase(Locale.getDefault())
            }
        }

        fun computeHighlightRanges(
            input: String,
            unaccentedRegexes: List<Regex>
        ): List<Pair<Int, Int>> {
            val normalizedInput: String = input.normalize()
            val positionMapping = PositionsMapping(input)

            val highlighted = BitSet(input.length)
            unaccentedRegexes.forEach { regex ->
                regex.findAll(normalizedInput).forEach {
                    highlighted.set(
                        positionMapping.getIndex(it.range.first, true),
                        positionMapping.getIndex(
                            it.range.last,
                            false
                        ) + 1 // range.last is inclusive!
                    )
                }
            }


            if (highlighted.isEmpty) {
                return emptyList()
            }

            val result: MutableList<Pair<Int, Int>> = ArrayList()
            var start = highlighted.nextSetBit(0)
            while (start != -1) {
                val end = highlighted.nextClearBit(start + 1)
                result.add(Pair(start, end))
                start = highlighted.nextSetBit(end)
            }

            return result
        }
    }
}

fun SpannableString.linkify(): SpannableString {
    return apply {
        LinkifyCompat.addLinks(
            this,
            Linkify.WEB_URLS or Linkify.EMAIL_ADDRESSES or Linkify.PHONE_NUMBERS
        )
        getSpans(0, length, URLSpan::class.java).onEach { urlSpan ->
            setSpan(
                SecureUrlSpan(urlSpan.url),
                getSpanStart(urlSpan),
                getSpanEnd(urlSpan),
                getSpanFlags(urlSpan)
            )
            removeSpan(urlSpan)
        }
    }
}

fun List<String>.fullTextSearchEscape(): String {
    return this.joinToString(separator = " ", transform = {
        "$it*"
    })
}


data class PositionsMapping(val input: String) {

    // Differences from normalized text to input text.
    private var deltas: MutableMap<Int, Int> = LinkedHashMap()

    // Indexes in normalized text that are ignored in input text.
    private val gaps: BitSet = BitSet(input.length)

    init {
        compute()
    }

    /**
     * @param pos the position in normalize text
     * @param start whether the given position represents start or end of a range.
     * @return the position in the input text.
     */
    fun getIndex(pos: Int, start: Boolean): Int {
        // this SHOULD never happen, but it happened during a test...
        if ((pos < 0) || (pos >= gaps.size())) {
            return pos
        }
        var p = pos
        if (gaps[pos]) {
            // If the position is in a gap, find the first position outside it.
            p = if (start) gaps.previousClearBit(p) else gaps.nextClearBit(p)
        }
        var offset = 0
        // Deals with all deltas before the given position.
        for (delta in deltas.entries) {
            if (delta.key > p) {
                break
            }
            offset += delta.value
        }
        return p + offset
    }

    private fun compute() {
        // Compute Diacritical Marks and ligature delta from normalization
        var cur = 0
        for (i in input.indices) {
            val ch = input.substring(i, i + 1)
            val normalized = ch.normalize()
            // Compute the delta between a substring of length one input and its normalization
            val delta = ch.length - normalized.length
            if (delta != 0) {
                deltas[cur + normalized.length] = delta
            }
            // If the normalized is larger that the input string, mark sub position as gap.
            if (delta < 0) {
                gaps[cur + 1] = cur + normalized.length
            }
            cur += normalized.length
        }
    }
}

fun String.getCodePoints(): IntArray {
    /**
     * Returns an array of code points from this string.
     *
     * This function is equivalent to the `codePoints()` method in Java's String class.
     * It iterates through the string and extracts each Unicode code point,
     * handling surrogate pairs correctly.
     *
     * @return An IntArray containing the Unicode code points of the string.
     */
    val codePoints = mutableListOf<Int>()
    var i = 0
    while (i < length) {
        val codePoint = codePointAt(i)
        codePoints.add(codePoint)
        i += Character.charCount(codePoint)
    }
    return codePoints.toIntArray()
}

fun String.isStringOnlyEmojis(): Boolean {
    if (isEmpty()) {
        return false
    }

    val codePoints = getCodePoints()
    for (i in codePoints.indices) {
        val codePoint = codePoints[i]
        if (!isEmojiCodepoint(codePoint)) {
            // Some emojis start with a base character that is not an emoji codepoint on its own,
            // and only become an emoji thanks to a following codepoint that is already recognized
            // by isEmojiCodepoint:
            //  - the emoji variation selector U+FE0F, which promotes a text-default symbol to an
            //    emoji, e.g. "©️", "®️", "™️", "‼️", "⁉️", "ℹ️", "〰️", "㊗️" — and keycaps "1️⃣", "#️⃣";
            //  - the combining enclosing keycap U+20E3 for keycaps typed without U+FE0F, e.g. "0⃣".
            // Accept the base char in those cases. (issue #1277)
            if (!codePoints.startsEmojiPresentationSequenceAt(i)) {
                return false
            }
        }
    }
    return true
}

// True when the codepoint at [index] begins an emoji presentation sequence whose base is not
// itself an emoji codepoint:
//  - a keycap base (0-9, # or *) followed by the combining enclosing keycap U+20E3, optionally
//    preceded by U+FE0F, e.g. "1️⃣" or "0⃣" (a keycap base + U+FE0F alone, without U+20E3, is not
//    an emoji, so U+20E3 is required here);
//  - any other base followed by the emoji variation selector U+FE0F, which promotes a text-default
//    symbol to an emoji, e.g. "©️", "™️", "‼️", "〰️", "㊗️".
private fun IntArray.startsEmojiPresentationSequenceAt(index: Int): Boolean {
    val base = this[index]
    val isKeycapBase = base in '0'.code..'9'.code || base == '#'.code || base == '*'.code
    if (isKeycapBase) {
        var next = index + 1
        if (getOrNull(next) == 0xfe0f) {
            next++
        }
        return getOrNull(next) == 0x20e3
    }
    return getOrNull(index + 1) == 0xfe0f
}


fun String.getShortEmojis(maxLength: Int): List<String> {
    val emojiSequence = EmojiCompat.get().process(this, 0, length)
    if (emojiSequence is Spanned) {
        var regionEnd: Int
        var regionStart = 0
        return buildList {
            val spans = emojiSequence.getSpans(
                regionStart, emojiSequence.length,
                EmojiSpan::class.java
            ).asList()
            if (spans.isEmpty() || spans.size > maxLength || isStringOnlyEmojis().not()) {
                return emptyList()
            }
            spans.forEach {
                regionEnd = emojiSequence.getSpanEnd(it)
                add(emojiSequence.subSequence(regionStart, regionEnd).toString())
                regionStart = regionEnd
            }
        }
    }
    return emptyList()
}


fun String?.jsonIdentityDetails() : JsonIdentityDetails? {
    return runCatching {
        if (this == null) return null
        AppSingleton.getJsonObjectMapper()
            .readValue(this, JsonIdentityDetails::class.java)
    }.getOrNull()
}

/**
 * The display name carried by serialized [JsonIdentityDetails], or null when unparsable — the
 * canonical formatting wherever an identity is shown before a Contact row exists. Pass
 * [SettingsActivity.contactDisplayNameFormat] as [format] at call sites that must honour the
 * user's display-name format setting instead of the fixed invitation-style default.
 */
fun String?.formatSerializedIdentityDetails(
    format: String = JsonIdentityDetails.FORMAT_STRING_FIRST_LAST_POSITION_COMPANY,
): String? =
    jsonIdentityDetails()?.formatDisplayName(format, SettingsActivity.uppercaseLastName)


fun Float?.formatBytesSpeed(context: Context) : String? {
    return this?.let {
        context.getString(R.string.xxx_per_s, Formatter.formatShortFileSize(context, it.roundToLong()))
    }
}

fun Int?.formatEtaSeconds(context: Context) : String? {
    return this?.let {
        if (it > 5940) {
            context.getString(
                R.string.text_timer_h,
                it / 3600
            )
        } else if (it > 99) {
            context.getString(
                R.string.text_timer_m,
                it / 60
            )
        } else if (it > 0) {
            context.getString(R.string.text_timer_s, it)
        } else {
            "-"
        }
    }
}