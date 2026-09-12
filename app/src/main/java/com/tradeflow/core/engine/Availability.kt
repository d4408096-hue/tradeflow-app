package com.tradeflow.core.engine

import android.content.Context
import com.tradeflow.core.data.Repo
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Real free-slot calculation from working hours + existing jobs + days off. */
object Availability {
    data class Slot(val at: Long, val label: String)

    private val LBL = DateTimeFormatter.ofPattern("EEE ha", Locale.US) // "Tue 8AM"
    private val TONLY = DateTimeFormatter.ofPattern("h:mma", Locale.US)

    /**
     * Next 2 free slots (hourly starts, 30-min buffer). Returns EMPTY unless 2 found
     * within 7 days — callers escalate to Mike in that case. Skips days off.
     */
    suspend fun nextTwoSlots(ctx: Context): List<Slot> {
        val repo = appRepo(ctx)
        val days = Prefs.workDays(ctx)
        val startH = Prefs.workStart(ctx)
        val endH = Prefs.workEnd(ctx)
        val durMin = Prefs.typicalJobMins(ctx).coerceIn(30, 480)
        val zone = ZoneId.systemDefault()
        val nowMs = System.currentTimeMillis()
        val found = mutableListOf<Slot>()
        var day = LocalDateTime.now().toLocalDate()
        repeat(7) {
            if (repo.offOn(day.toString()) != null) {
                day = day.plusDays(1)
                return@repeat
            }
            // java DayOfWeek MON=1..SUN=7 -> Calendar SUN=1..SAT=7
            val calDow = day.dayOfWeek.value.let { v -> if (v == 7) 1 else v + 1 }
            if (days.contains(calDow)) {
                var t = day.atTime(startH, 0)
                val end = day.atTime(endH, 0)
                while (!t.plusMinutes(durMin.toLong()).isAfter(end) && found.size < 2) {
                    val at = t.atZone(zone).toInstant().toEpochMilli()
                    if (at > nowMs + 30 * 60 * 1000 &&
                        repo.clashingJobs(at, at + durMin * 60_000L).isEmpty()
                    ) {
                        found.add(Slot(at, pretty(t)))
                    }
                    t = t.plusHours(1)
                }
            }
            day = day.plusDays(1)
        }
        return if (found.size == 2) found else emptyList()
    }

    /**
     * Full callback clause. Normal: "He's on a job until ~2:30pm — he'll call
     * or text you right after". Day off: "He's back on Tue 15 — he'll call or text you then".
     */
    suspend fun freeAfterLabel(ctx: Context): String {
        val repo = appRepo(ctx)
        val off = repo.offOn(Repo.todayStr())
        if (off != null) {
            val back = try {
                LocalDate.parse(off.endDate).plusDays(1)
                    .format(DateTimeFormatter.ofPattern("EEE d", Locale.US))
            } catch (_: Exception) {
                "soon"
            }
            return "He's back on $back — he'll call or text you then"
        }
        val nowMs = System.currentTimeMillis()
        val cur = repo.currentJob()
        val at = cur?.endAt?.takeIf { it > nowMs }
            ?: (nowMs + Prefs.typicalJobMins(ctx) * 60_000L)
        val t = LocalDateTime.ofInstant(Instant.ofEpochMilli(at), ZoneId.systemDefault())
        return "He's on a job until ~" + TONLY.format(t).replace("AM", "am").replace("PM", "pm") +
            " — he'll call or text you right after"
    }

    private fun pretty(t: LocalDateTime): String =
        LBL.format(t).replace("AM", "am").replace("PM", "pm") // "Tue 8am"
}
