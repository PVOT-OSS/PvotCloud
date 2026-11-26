// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.repo.tree

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

internal object GoStrings {

    private const val HEX_DIGITS = "0123456789abcdef"
    private const val HEX_RADIX = 16
    private const val OCTAL_RADIX = 8
    private const val OCTAL_DIGITS = 3
    private const val NIBBLE_BITS = 4
    private const val NIBBLE_MASK = 0xf
    private const val ASCII_LIMIT = 0x80
    private const val BMP_LIMIT = 0x10000
    private const val REPLACEMENT = 0xfffd
    private const val BYTE_DIGITS = 2
    private const val SHORT_DIGITS = 4
    private const val LONG_DIGITS = 8

    private val quoteEscapes: Map<Int, String> =
        mapOf(
            '\u0007'.code to "\\a",
            '\b'.code to "\\b",
            '\u000c'.code to "\\f",
            '\n'.code to "\\n",
            '\r'.code to "\\r",
            '\t'.code to "\\t",
            '\u000b'.code to "\\v",
            '\\'.code to "\\\\",
            '"'.code to "\\\"",
        )

    private val unquoteEscapes: Map<Char, Char> =
        mapOf(
            'a' to '\u0007',
            'b' to '\b',
            'f' to '\u000c',
            'n' to '\n',
            'r' to '\r',
            't' to '\t',
            'v' to '\u000b',
            '\\' to '\\',
            '"' to '"',
            '\'' to '\'',
        )

    private val numericEscapeDigits: Map<Char, Int> =
        mapOf('x' to BYTE_DIGITS, 'u' to SHORT_DIGITS, 'U' to LONG_DIGITS)

    private val jsonEscapes: Map<Char, String> =
        mapOf('"' to "\\\"", '\\' to "\\\\", '\n' to "\\n", '\r' to "\\r", '\t' to "\\t")

    private val jsonUnicodeEscaped: Set<Char> = setOf('<', '>', '&', '\u2028', '\u2029')

    fun quote(value: String): String {
        val out = StringBuilder()
        var i = 0
        while (i < value.length) {
            val cp = value.codePointAt(i)
            i += Character.charCount(cp)
            val escape = quoteEscapes[cp]
            when {
                escape != null -> out.append(escape)
                isPrint(cp) -> out.appendCodePoint(cp)
                else -> out.append(numericEscape(cp))
            }
        }
        return out.toString()
    }

    fun unquote(value: String): String {
        val out = StringBuilder()
        var i = 0
        while (i < value.length) {
            val c = value[i++]
            if (c == '\\') i = appendEscape(value, i, out) else out.append(c)
        }
        return out.toString()
    }

    fun jsonString(value: String): String {
        val out = StringBuilder("\"")
        for (c in value) {
            val escape = jsonEscapes[c]
            when {
                escape != null -> out.append(escape)
                c < ' ' || c in jsonUnicodeEscaped ->
                    out.append("\\u").append(hex(c.code, SHORT_DIGITS))
                else -> out.append(c)
            }
        }
        return out.append('"').toString()
    }

    fun time(value: OffsetDateTime): String = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(value)

    private fun numericEscape(cp: Int): String =
        when {
            cp < ASCII_LIMIT -> "\\x" + hex(cp, BYTE_DIGITS)
            cp < BMP_LIMIT -> "\\u" + hex(cp, SHORT_DIGITS)
            else -> "\\U" + hex(cp, LONG_DIGITS)
        }

    private fun appendEscape(value: String, start: Int, out: StringBuilder): Int {
        require(start < value.length) { "dangling escape" }
        val e = value[start]
        val simple = unquoteEscapes[e]
        val digits = numericEscapeDigits[e]
        return when {
            simple != null -> {
                out.append(simple)
                start + 1
            }
            digits != null -> {
                val code = value.substring(start + 1, start + 1 + digits).toInt(HEX_RADIX)
                out.appendCodePoint(if (e == 'x') asciiOrReplacement(code) else code)
                start + 1 + digits
            }
            e in '0'..'7' -> {
                val code = value.substring(start, start + OCTAL_DIGITS).toInt(OCTAL_RADIX)
                out.appendCodePoint(asciiOrReplacement(code))
                start + OCTAL_DIGITS
            }
            else -> throw IllegalArgumentException("invalid escape \\$e")
        }
    }

    private fun asciiOrReplacement(value: Int): Int =
        if (value < ASCII_LIMIT) value else REPLACEMENT

    private fun isPrint(cp: Int): Boolean {
        if (cp == ' '.code) return true
        return when (Character.getType(cp).toByte()) {
            Character.UPPERCASE_LETTER,
            Character.LOWERCASE_LETTER,
            Character.TITLECASE_LETTER,
            Character.MODIFIER_LETTER,
            Character.OTHER_LETTER,
            Character.NON_SPACING_MARK,
            Character.ENCLOSING_MARK,
            Character.COMBINING_SPACING_MARK,
            Character.DECIMAL_DIGIT_NUMBER,
            Character.LETTER_NUMBER,
            Character.OTHER_NUMBER,
            Character.CONNECTOR_PUNCTUATION,
            Character.DASH_PUNCTUATION,
            Character.START_PUNCTUATION,
            Character.END_PUNCTUATION,
            Character.INITIAL_QUOTE_PUNCTUATION,
            Character.FINAL_QUOTE_PUNCTUATION,
            Character.OTHER_PUNCTUATION,
            Character.MATH_SYMBOL,
            Character.CURRENCY_SYMBOL,
            Character.MODIFIER_SYMBOL,
            Character.OTHER_SYMBOL -> true
            else -> false
        }
    }

    private fun hex(value: Int, digits: Int): String =
        CharArray(digits) {
                HEX_DIGITS[(value ushr (NIBBLE_BITS * (digits - 1 - it))) and NIBBLE_MASK]
            }
            .concatToString()
}
