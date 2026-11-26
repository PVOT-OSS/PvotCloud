// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.cloud.repo.tree

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

internal object GoStrings {

    private const val HEX = "0123456789abcdef"

    fun quote(value: String): String {
        val out = StringBuilder()
        var i = 0
        while (i < value.length) {
            val cp = value.codePointAt(i)
            i += Character.charCount(cp)
            when (cp) {
                0x07 -> out.append("\\a")
                0x08 -> out.append("\\b")
                0x0c -> out.append("\\f")
                0x0a -> out.append("\\n")
                0x0d -> out.append("\\r")
                0x09 -> out.append("\\t")
                0x0b -> out.append("\\v")
                '\\'.code -> out.append("\\\\")
                '"'.code -> out.append("\\\"")
                else ->
                    when {
                        isPrint(cp) -> out.appendCodePoint(cp)
                        cp < 0x80 -> out.append("\\x").append(hex(cp, 2))
                        cp < 0x10000 -> out.append("\\u").append(hex(cp, 4))
                        else -> out.append("\\U").append(hex(cp, 8))
                    }
            }
        }
        return out.toString()
    }

    fun unquote(value: String): String {
        val out = StringBuilder()
        var i = 0
        while (i < value.length) {
            val c = value[i++]
            if (c != '\\') {
                out.append(c)
                continue
            }
            require(i < value.length) { "dangling escape" }
            when (val e = value[i++]) {
                'a' -> out.append('\u0007')
                'b' -> out.append('\b')
                'f' -> out.append('\u000c')
                'n' -> out.append('\n')
                'r' -> out.append('\r')
                't' -> out.append('\t')
                'v' -> out.append('\u000b')
                '\\',
                '"',
                '\'' -> out.append(e)
                'x' ->
                    out.appendCodePoint(asciiOrReplacement(value.substring(i, i + 2).toInt(16)))
                        .also { i += 2 }
                'u' -> out.appendCodePoint(value.substring(i, i + 4).toInt(16)).also { i += 4 }
                'U' -> out.appendCodePoint(value.substring(i, i + 8).toInt(16)).also { i += 8 }
                in '0'..'7' ->
                    out.appendCodePoint(asciiOrReplacement(value.substring(i - 1, i + 2).toInt(8)))
                        .also { i += 2 }
                else -> throw IllegalArgumentException("invalid escape \\$e")
            }
        }
        return out.toString()
    }

    fun jsonString(value: String): String {
        val out = StringBuilder("\"")
        for (c in value) {
            when {
                c == '"' -> out.append("\\\"")
                c == '\\' -> out.append("\\\\")
                c == '\n' -> out.append("\\n")
                c == '\r' -> out.append("\\r")
                c == '\t' -> out.append("\\t")
                c < ' ' || c == '<' || c == '>' || c == '&' || c == ' ' || c == ' ' ->
                    out.append("\\u").append(hex(c.code, 4))
                else -> out.append(c)
            }
        }
        return out.append('"').toString()
    }

    fun time(value: OffsetDateTime): String = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(value)

    private fun asciiOrReplacement(value: Int): Int = if (value < 0x80) value else 0xfffd

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
        CharArray(digits) { HEX[(value ushr (4 * (digits - 1 - it))) and 0xf] }.concatToString()
}
