package com.customapplock.studio.util

import java.util.Locale
import kotlin.math.abs

/** Tiny arithmetic evaluator for the calculator lock: + - × ÷ ( ) and decimals. */
fun evalExpression(src: String): Double? = try {
    val p = ExprParser(src.replace('×', '*').replace('÷', '/').replace('−', '-').replace(" ", ""))
    val v = p.parseExpr()
    if (p.pos != p.s.length || v.isNaN() || v.isInfinite()) null else v
} catch (e: Exception) {
    null
}

fun formatResult(d: Double): String =
    if (d == Math.rint(d) && abs(d) < 1e15) d.toLong().toString()
    else String.format(Locale.US, "%.4f", d).trimEnd('0').trimEnd('.')

private class ExprParser(val s: String) {
    var pos = 0

    fun parseExpr(): Double {
        var v = parseTerm()
        while (pos < s.length && (s[pos] == '+' || s[pos] == '-')) {
            val op = s[pos++]
            val r = parseTerm()
            v = if (op == '+') v + r else v - r
        }
        return v
    }

    private fun parseTerm(): Double {
        var v = parseFactor()
        while (pos < s.length && (s[pos] == '*' || s[pos] == '/')) {
            val op = s[pos++]
            val r = parseFactor()
            v = if (op == '*') v * r else v / r
        }
        return v
    }

    private fun parseFactor(): Double {
        if (pos >= s.length) throw IllegalArgumentException()
        if (s[pos] == '-') { pos++; return -parseFactor() }
        if (s[pos] == '(') {
            pos++
            val v = parseExpr()
            if (pos >= s.length || s[pos] != ')') throw IllegalArgumentException()
            pos++
            return v
        }
        val st = pos
        while (pos < s.length && (s[pos].isDigit() || s[pos] == '.')) pos++
        if (st == pos) throw IllegalArgumentException()
        return s.substring(st, pos).toDouble()
    }
}
