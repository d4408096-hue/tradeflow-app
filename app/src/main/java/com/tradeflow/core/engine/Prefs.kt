package com.tradeflow.core.engine

import android.content.Context

/** All settings + engine scratch state. One place — M3 screens edit these. */
object Prefs {
    private const val F = "tradeflow_prefs"
    private fun sp(ctx: Context) = ctx.getSharedPreferences(F, Context.MODE_PRIVATE)

    // ---------- busy mode ----------
    fun isBusy(ctx: Context): Boolean = sp(ctx).getBoolean("busy", false)
    fun setBusy(ctx: Context, b: Boolean) { sp(ctx).edit().putBoolean("busy", b).apply() }

    // ---------- business identity ----------
    fun bizName(ctx: Context): String = sp(ctx).getString("biz_name", "") ?: ""
    fun bizPhone(ctx: Context): String = sp(ctx).getString("biz_phone", "") ?: ""
    fun setBiz(ctx: Context, name: String, phone: String) {
        sp(ctx).edit().putString("biz_name", name).putString("biz_phone", phone).apply()
    }

    /** "Mike's Plumbing" -> "Mike". Fallback keeps sentences grammatical. */
    fun firstName(ctx: Context): String =
        bizName(ctx).substringBefore("'").substringBefore(" ").trim().ifBlank { "the owner" }

    // ---------- services (LAST item is always the "Other" escalation option) ----------
    fun services(ctx: Context): List<String> {
        val raw = sp(ctx).getString("services_csv", "") ?: ""
        if (raw.isBlank()) return Defaults.SERVICES
        return raw.split("|").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun setServices(ctx: Context, list: List<String>) {
        sp(ctx).edit().putString("services_csv", list.joinToString("|")).apply()
    }

    // ---------- working hours (Calendar day constants: SUN=1..SAT=7) ----------
    fun workDays(ctx: Context): Set<Int> {
        val raw = sp(ctx).getString("work_days", "") ?: ""
        val parsed = raw.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
        return parsed.ifEmpty { setOf(2, 3, 4, 5, 6, 7) } // Mon..Sat
    }

    fun workStart(ctx: Context): Int = sp(ctx).getInt("work_start", 8)
    fun workEnd(ctx: Context): Int = sp(ctx).getInt("work_end", 18)

    fun setWork(ctx: Context, days: Set<Int>, startH: Int, endH: Int) {
        sp(ctx).edit().putString("work_days", days.sorted().joinToString(","))
            .putInt("work_start", startH).putInt("work_end", endH).apply()
    }

    fun typicalJobMins(ctx: Context): Int = sp(ctx).getInt("typical_mins", 120)
    fun setTypicalJobMins(ctx: Context, m: Int) { sp(ctx).edit().putInt("typical_mins", m).apply() }

    // ---------- links + behavior ----------
    fun paymentLink(ctx: Context): String = sp(ctx).getString("pay_link", "") ?: ""
    fun reviewLink(ctx: Context): String = sp(ctx).getString("review_link", "") ?: ""
    fun setLinks(ctx: Context, pay: String, review: String) {
        sp(ctx).edit().putString("pay_link", pay).putString("review_link", review).apply()
    }

    fun autoConfirm(ctx: Context): Boolean = sp(ctx).getBoolean("auto_confirm", false)
    fun setAutoConfirm(ctx: Context, b: Boolean) { sp(ctx).edit().putBoolean("auto_confirm", b).apply() }

    // ---------- call-tracking scratch ----------
    fun setLastRinging(ctx: Context, num: String?, at: Long) {
        sp(ctx).edit().putString("ring_num", num).putLong("ring_at", at).apply()
    }

    fun getLastRinging(ctx: Context): Pair<String?, Long> =
        sp(ctx).getString("ring_num", null) to sp(ctx).getLong("ring_at", 0)

    fun clearLastRinging(ctx: Context) {
        sp(ctx).edit().remove("ring_num").remove("ring_at").apply()
    }

    fun lastHandledCallDate(ctx: Context): Long = sp(ctx).getLong("handled_call_date", 0)
    fun setLastHandledCallDate(ctx: Context, d: Long) {
        sp(ctx).edit().putLong("handled_call_date", d).apply()
    }

    // ---------- alert throttles ----------
    fun lastPermNag(ctx: Context): Long = sp(ctx).getLong("perm_nag", 0)
    fun setLastPermNag(ctx: Context, t: Long) { sp(ctx).edit().putLong("perm_nag", t).apply() }
    fun lastFailAt(ctx: Context): Long = sp(ctx).getLong("fail_at", 0)
    fun setLastFailAt(ctx: Context, t: Long) { sp(ctx).edit().putLong("fail_at", t).apply() }

    // ---------- wording (vertical menu — approved format) ----------
    fun menuText(ctx: Context, knownName: String?): String {
        val biz = bizName(ctx).ifBlank { "our shop" }
        val hi = if (knownName.isNullOrBlank()) "Hi, $biz here 🔧" else "Hi $knownName! $biz here 🔧"
        val lines = services(ctx).mapIndexed { i, s -> "${i + 1}. $s" }
        return "$hi I'm on a job right now. What do you need? Reply with a number:\n" +
            lines.joinToString("\n")
    }
}
