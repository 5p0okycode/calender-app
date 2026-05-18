package com.crochet.calendar

import androidx.compose.runtime.mutableStateListOf
import java.util.Calendar as JC

class CalendarLogic {
    val months: Array<String> = arrayOf(
        "January", "February", "March",     "April",
        "May",     "June",     "July",      "August",
        "September","October", "November",  "December",
    )

    val monthLen: IntArray = intArrayOf(
        31, 28, 31, 30, 31, 30,
        31, 31, 30, 31, 30, 31
    )


    // **Actual Day
    val todayDay:   Int
    val todayMonth: Int  // 0-based (Jan = 0)
    val todayYear:  Int

    // **Currently-viewed Day

    var curMonth: Int
        private set //0 based again

    var curDay: Int = 1
        set(value) { field = value.coerceIn(1, daysInMonth) }

    var curYear: Int
        private set


    init {
        val now = JC.getInstance()
        todayDay   = now.get(JC.DAY_OF_MONTH)
        todayMonth = now.get(JC.MONTH)
        todayYear  = now.get(JC.YEAR)

        curMonth = todayMonth
        curDay   = todayDay
        curYear  = todayYear

        leapYear()  // set correct Feb length for the current year
    }

    fun nextMonth() {
        if (curMonth == 11) {
            curYear++
            leapYear()
        }
        curMonth = (curMonth + 1) % 12
    }

    fun prevMonth() {
        if (curMonth == 0) {
            curYear--
            leapYear()
        }
        curMonth = ((curMonth - 1) + 12) % 12
    }

    //leap years are every feb 29 2000+-4x
    fun leapYear() {
        monthLen[1] = if (isLeapYear(curYear)) 29 else 28
    }

    // num of days in month
    val daysInMonth: Int
        get() = monthLen[curMonth]

    // Display, e.g. "April 2026"
    val monthLabel: String
        get() = "${months[curMonth]} $curYear"

    // Day-of-week for the 1st of the viewed month
    val firstDayOfWeek: Int
        get() {
            val c = JC.getInstance()
            c.set(curYear, curMonth, 1)
            return c.get(JC.DAY_OF_WEEK)
        }


    companion object {
        fun isLeapYear(year: Int): Boolean =
            year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
    }
}
data class birthday(
    val name:  String,
    val month: Int,
    val day:   Int,
    val yours: Boolean
)

object birthDays {
    var all = mutableStateListOf<birthday>()
    //for testing
    //var all = mutableStateListOf<birthday>( birthday("Victor", 5, 14, true), birthday("Someone", 5, 13, false) )

    fun getBirthdayForDay(month: Int, day: Int): List<birthday> {
        return all.filter { it.month == month && it.day == day }
    }
    fun addBirthday(name:  String, month: Int, day: Int, yours: Boolean) {
        all.add(birthday(name, month, day, yours))
    }
}

//for funsies, got lazy and used ai to make a list of holidays
data class Holiday(
    val name:  String,
    val month: Int,
    val day:   Int,
    val emoji: String,
    val prefix: String = "happy"
)

object Holidays {
    // ── Custom holidays ───────────────────────────────────────────────────────
    val customHolidays = mutableStateListOf<Holiday>()

    // ── Fixed-date holidays ───────────────────────────────────────────────────

    val fixed = listOf(

        // ── American ─────────────────────────────────────────────────────────

        Holiday("New Year's Day",         1,  1,  "🎆"),
        Holiday("Valentine's Day",        2,  14, "💝"),
        Holiday("St. Patrick's Day",      3,  17, "☘️"),
        Holiday("Independence Day",       7,  4,  "🇺🇸"),
        Holiday("Halloween",              10, 31, "🎃"),
        Holiday("Veterans Day",           11, 11, "🎖️"),
        Holiday("Christmas Eve",          12, 24, "🌟", "merry"),
        Holiday("Christmas Day",          12, 25, "🎄", "merry"),
        Holiday("New Year's Eve",         12, 31, "🥂"),

        // ── German ───────────────────────────────────────────────────────────

        Holiday("Heilige Drei Könige",               1,  6,  "⭐"),   // Heilige Drei Könige (BY, BW, ST)
        Holiday("Internationaler Frauentag", 3, 8, "💐"),  // Internationaler Frauentag (BE, MV, TH)
        Holiday("Tag der Arbeit",             5,  1,  "⚒️"),   // Tag der Arbeit
        Holiday("Mariä Himmelfahrt",     8,  15, "🕊️"),  // Mariä Himmelfahrt (BY, SL)
        Holiday("Tag der Deutschen Einheit",       10, 3,  "🇩🇪"),  // Tag der Deutschen Einheit
        Holiday("Reformationstag",        10, 31, "⛪"),   // Reformationstag (BB, HB, HH, MV, NI, SN, ST, SH, TH)
        Holiday("Allerheiligen",        11, 1,  "🕯️"),  // Allerheiligen (BW, BY, NW, RP, SL)
        Holiday("Nikolaustag",       12, 6,  "🎅"),   // Nikolaustag (traditional)
        Holiday("1. Weinachtstag",          12, 25, "🎄", "frohe"),   // 1. Weihnachtstag
        Holiday("2. Weihnachtstag",   12, 26, "🎁", "frohe"),   // 2. Weihnachtstag
    )

    // ── Calculated holidays (change every year) ───────────────────────────────
    // Easter is the anchor for most moveable holidays.
    // Uses the Anonymous Gregorian algorithm.

    fun easterSunday(year: Int): Pair<Int, Int> {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day   = ((h + l - 7 * m + 114) % 31) + 1
        return Pair(month, day)
    }

    // Add n days to a month/day pair (handles month rollover)
    private fun addDays(month: Int, day: Int, year: Int, n: Int): Pair<Int, Int> {
        val cal = java.util.Calendar.getInstance()
        cal.set(year, month - 1, day)
        cal.add(java.util.Calendar.DAY_OF_YEAR, n)
        return Pair(cal.get(java.util.Calendar.MONTH) + 1, cal.get(java.util.Calendar.DAY_OF_MONTH))
    }

    fun moveable(year: Int): List<Holiday> {
        val (easterMonth, easterDay) = easterSunday(year)

        fun offset(n: Int): Pair<Int, Int> = addDays(easterMonth, easterDay, year, n)

        val (goodFriM,   goodFriD)   = offset(-2)   // Good Friday / Karfreitag
        val (easterMonM, easterMonD) = offset(1)     // Easter Monday / Ostermontag
        val (ascM,       ascD)       = offset(39)    // Ascension / Christi Himmelfahrt
        val (whitSunM,   whitSunD)   = offset(49)    // Whit Sunday / Pfingstsonntag
        val (whitMonM,   whitMonD)   = offset(50)    // Whit Monday / Pfingstmontag
        val (corpusM,    corpusD)    = offset(60)    // Corpus Christi / Fronleichnam (BW, BY, HE, NW, RP, SL)

        // US moveable holidays (calculated by weekday rules)
        val mlkDay        = nthWeekday(year, 1,  java.util.Calendar.MONDAY, 3)   // 3rd Mon Jan
        val presidentsDay = nthWeekday(year, 2,  java.util.Calendar.MONDAY, 3)   // 3rd Mon Feb
        val memorialDay   = lastWeekday(year, 5,  java.util.Calendar.MONDAY)     // Last Mon May
        val laborDay      = nthWeekday(year, 9,  java.util.Calendar.MONDAY, 1)   // 1st Mon Sep
        val columbusDay   = nthWeekday(year, 10, java.util.Calendar.MONDAY, 2)   // 2nd Mon Oct
        val thanksgiving  = nthWeekday(year, 11, java.util.Calendar.THURSDAY, 4) // 4th Thu Nov
        val motherDay     = nthWeekday(year, 5,  java.util.Calendar.SUNDAY, 2)   // 2nd Sun May
        val fatherDay     = nthWeekday(year, 6,  java.util.Calendar.SUNDAY, 3)   // 3rd Sun Jun

        return listOf(
            // ── American moveable ─────────────────────────────────────────────
            Holiday("Martin Luther King Jr. Day", mlkDay.first,        mlkDay.second,        "✊"),
            Holiday("Presidents' Day",            presidentsDay.first,  presidentsDay.second, "🏛️"),
            Holiday("Mother's Day",               motherDay.first,      motherDay.second,     "💐"),
            Holiday("Memorial Day",               memorialDay.first,    memorialDay.second,   "🪖"),
            Holiday("Father's Day",               fatherDay.first,      fatherDay.second,     "👔"),
            Holiday("Labor Day",                  laborDay.first,       laborDay.second,      "⚒️"),
            Holiday("Columbus Day",               columbusDay.first,    columbusDay.second,   "⛵"),
            Holiday("Thanksgiving",               thanksgiving.first,   thanksgiving.second,  "🦃"),

            // ── German moveable (Easter-based) ────────────────────────────────
            Holiday("Karfreitag",     goodFriM,   goodFriD,   "✝️"),  // Karfreitag
            Holiday("Ostersonntag",   easterMonth, easterDay,  "🐣"),  // Ostersonntag
            Holiday("Ostermontag",   easterMonM, easterMonD, "🥚"),  // Ostermontag
            Holiday("Christi Himmelfahrt",   ascM,       ascD,       "☁️"),  // Christi Himmelfahrt
            Holiday("Pfingstsonntag",     whitSunM,   whitSunD,   "🕊️"), // Pfingstsonntag
            Holiday("Pfingstmontag",     whitMonM,   whitMonD,   "🌸"),  // Pfingstmontag
            Holiday("Fronleichnam",  corpusM,    corpusD,    "🌿"),  // Fronleichnam
        )
    }

    // ── Weekday calculation helpers ───────────────────────────────────────────

    // nth occurrence of a weekday in a month, e.g. 3rd Monday of January
    private fun nthWeekday(year: Int, month: Int, weekday: Int, n: Int): Pair<Int, Int> {
        val cal = java.util.Calendar.getInstance()
        cal.set(year, month - 1, 1)
        var count = 0
        while (true) {
            if (cal.get(java.util.Calendar.DAY_OF_WEEK) == weekday) {
                count++
                if (count == n) return Pair(month, cal.get(java.util.Calendar.DAY_OF_MONTH))
            }
            cal.add(java.util.Calendar.DAY_OF_MONTH, 1)
        }
    }

    // Last occurrence of a weekday in a month, e.g. last Monday of May
    private fun lastWeekday(year: Int, month: Int, weekday: Int): Pair<Int, Int> {
        val cal = java.util.Calendar.getInstance()
        cal.set(year, month - 1, 1)
        cal.set(java.util.Calendar.DAY_OF_MONTH, cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH))
        while (cal.get(java.util.Calendar.DAY_OF_WEEK) != weekday) {
            cal.add(java.util.Calendar.DAY_OF_MONTH, -1)
        }
        return Pair(month, cal.get(java.util.Calendar.DAY_OF_MONTH))
    }

    // ── Main lookup — call this from your calendar grid ───────────────────────

    private var cachedYear: Int = -1
    private var cachedMoveable: List<Holiday> = emptyList()

    fun getForDay(month: Int, day: Int, year: Int): List<Holiday> {
        if (year != cachedYear) {
            cachedYear = year
            cachedMoveable = moveable(year)
        }
        val allFixed    = (fixed + customHolidays).filter { it.month == month && it.day == day }
        val allMoveable = cachedMoveable.filter { it.month == month && it.day == day }
        // Deduplicate Christmas which appears in both US and German fixed lists
        return (allFixed + allMoveable).distinctBy { it.name }
    }
    fun addHoliday(name: String, month: Int, day: Int, prefix: String) {
        customHolidays.add(Holiday(name, month, day, "🎉", prefix))
    }
}
