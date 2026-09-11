package com.tradeflow.core.ui

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Date + money formatting. One place, US style. */
object Fmt {
    private val DT = DateTimeFormatter.ofPattern("EEE, MMM d · h:mma", Locale.US)
    private val DAY = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US)
    private val TIME = DateTimeFormatter.ofPattern("h:mma", Locale.US)

    fun dt(at: Long): String = ldt(at).format(DT).ampm()
    fun day(at: Long): String = ldt(at).format(DAY)
    fun time(at: Long): String = ldt(at).format(TIME).ampm()

    fun money(cents: Long): String = "$" + String.format(Locale.US, "%,d", cents / 100)

    /** "285" / "285.50" / "$285" -> 28500 */
    fun dollarsToCents(s: String): Int =
        ((s.filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: 0.0) * 100).toInt()

    private fun ldt(at: Long) =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(at), ZoneId.systemDefault())

    private fun String.ampm() = replace("AM", "am").replace("PM", "pm")
}
