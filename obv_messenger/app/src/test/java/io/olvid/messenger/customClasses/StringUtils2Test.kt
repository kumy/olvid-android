package io.olvid.messenger.customClasses

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StringUtils2Test {

    @Test
    fun searchMatchRank_returnsIndexOfFirstWordStartingWithFilter() {
        assertEquals(0, StringUtils2.searchMatchRank("Cindy Joy", "ci"))
        assertEquals(1, StringUtils2.searchMatchRank("Cindy Joy", "jo"))
        assertEquals(2, StringUtils2.searchMatchRank("Alice Bob Carol", "car"))
    }

    @Test
    fun searchMatchRank_ranksNonPrefixMatchesLast() {
        // "dy" matches inside "Cindy" but no word starts with it
        assertEquals(Int.MAX_VALUE / 2, StringUtils2.searchMatchRank("Cindy Joy", "dy"))
        assertEquals(Int.MAX_VALUE, StringUtils2.searchMatchRank("Cindy Joy", "zz"))
    }

    @Test
    fun searchMatchRank_isAccentAndCaseInsensitive() {
        assertEquals(0, StringUtils2.searchMatchRank("Éric Dupont", "eri"))
        assertEquals(1, StringUtils2.searchMatchRank("Jean Sébastien", "SEB"))
    }

    @Test
    fun searchMatchRank_usesBestTokenOfMultiWordFilter() {
        assertEquals(0, StringUtils2.searchMatchRank("Cindy Joy", "jo ci"))
        assertEquals(1, StringUtils2.searchMatchRank("Cindy Joy", "zz jo"))
    }

    @Test
    fun searchMatchRank_blankFilterRanksEverythingEqual() {
        assertEquals(Int.MAX_VALUE, StringUtils2.searchMatchRank("Cindy Joy", null))
        assertEquals(Int.MAX_VALUE, StringUtils2.searchMatchRank("Cindy Joy", "  "))
    }

    @Test
    fun searchMatchRank_sortsPrefixMatchesFirstAndIsStable() {
        val names = listOf("Big Joe", "Jonas", "Banjo", "Cindy Joy", "Joe")
        val sorted = names.sortedBy { StringUtils2.searchMatchRank(it, "jo") }
        assertEquals(listOf("Jonas", "Joe", "Big Joe", "Cindy Joy", "Banjo"), sorted)
    }

    // Keycap emojis: base char (0-9, # or *) + optional U+FE0F + combining enclosing keycap U+20E3
    private val keycap1 = "1️⃣"    // "1️⃣"
    private val keycapHash = "#️⃣" // "#️⃣"
    private val keycapStar = "*️⃣" // "*️⃣"
    private val keycap0NoVs = "0⃣"      // keycap without variation selector

    @Test
    fun isStringOnlyEmojis_detectsKeycaps() {
        // keycaps start with a non-emoji base char but are still emojis (issue #1277)
        assertTrue(keycap1.isStringOnlyEmojis())
        assertTrue(keycapHash.isStringOnlyEmojis())
        assertTrue(keycapStar.isStringOnlyEmojis())
        assertTrue(keycap0NoVs.isStringOnlyEmojis())
        assertTrue((keycap1 + keycapHash).isStringOnlyEmojis())
    }

    @Test
    fun isStringOnlyEmojis_keycapsMixedWithStandardEmoji() {
        assertTrue((keycap1 + "😀").isStringOnlyEmojis()) // "1️⃣😀"
    }

    // Category A: text-default symbols promoted to emoji by the variation selector U+FE0F,
    // whose base codepoint is below U+2194 and not an emoji on its own.
    @Test
    fun isStringOnlyEmojis_detectsVariationSelectorEmojis() {
        assertTrue("©️".isStringOnlyEmojis())  // U+00A9 U+FE0F
        assertTrue("®️".isStringOnlyEmojis())  // U+00AE U+FE0F
        assertTrue("™️".isStringOnlyEmojis())  // U+2122 U+FE0F
        assertTrue("‼️".isStringOnlyEmojis())  // U+203C U+FE0F
        assertTrue("⁉️".isStringOnlyEmojis())  // U+2049 U+FE0F
        assertTrue("ℹ️".isStringOnlyEmojis())  // U+2139 U+FE0F
        assertTrue("™️😀".isStringOnlyEmojis()) // mixed with a standard emoji
    }

    // Category B: emojis in the U+2B55..U+1F000 gap, in their fully-qualified U+FE0F form.
    @Test
    fun isStringOnlyEmojis_detectsGapRangeEmojis() {
        assertTrue("〰️".isStringOnlyEmojis()) // U+3030 U+FE0F
        assertTrue("〽️".isStringOnlyEmojis()) // U+303D U+FE0F
        assertTrue("㊗️".isStringOnlyEmojis()) // U+3297 U+FE0F
        assertTrue("㊙️".isStringOnlyEmojis()) // U+3299 U+FE0F
    }

    @Test
    fun isStringOnlyEmojis_rejectsPlainCharacters() {
        assertFalse("".isStringOnlyEmojis())
        assertFalse("1".isStringOnlyEmojis())
        assertFalse("12".isStringOnlyEmojis())
        assertFalse("hello".isStringOnlyEmojis())
        // keycap base without the enclosing keycap is not an emoji
        assertFalse("1️".isStringOnlyEmojis()) // "1" + variation selector only
    }

    @Test
    fun isStringOnlyEmojis_rejectsEmojiMixedWithText() {
        assertFalse("😀 hi".isStringOnlyEmojis()) // "😀 hi"
        assertFalse((keycap1 + "a").isStringOnlyEmojis())    // "1️⃣a"
    }

    @Test
    fun isStringOnlyEmojis_standardEmojiStillWorks() {
        assertTrue("😀".isStringOnlyEmojis()) // "😀"
    }
}
