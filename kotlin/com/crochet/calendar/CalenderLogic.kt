package com.crochet.calendar

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import java.util.Calendar as JC

class CalendarLogic(context: Context? = null) {
    private val appPrefs = context?.let { Prefs(it) }

    val months: Array<String> = arrayOf(
        "January", "February", "March",     "April",
        "May",     "June",     "July",      "August",
        "September","October", "November",  "December",
    )

    val monthLen: IntArray = intArrayOf(
        31, 28, 31, 30, 31, 30,
        31, 31, 30, 31, 30, 31
    )


    // Actual Day
    val todayDay:   Int
    val todayMonth: Int  // 0-based (Jan = 0)
    val todayYear:  Int

    // Currently-viewed Day

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

        // Load persisted data
        appPrefs?.let { prefs ->
            // Load holidays
            val savedHolidays = prefs.loadHolidays()
            if (savedHolidays.isNotEmpty()) {
                holidays.saved.clear()
                holidays.saved.addAll(savedHolidays)
            }
            // Load birthdays
            val savedBirthdays = prefs.loadBirthdays()
            if (savedBirthdays.isNotEmpty()) {
                birthDays.all.clear()
                birthDays.all.addAll(savedBirthdays)
            }
        }
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

    fun nextYear() {
        curYear++
        leapYear()
    }

    fun prevYear() {
        curYear--
        leapYear()
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

    fun saveCustomHolidays() {
        appPrefs?.saveHolidays(holidays.saved)
    }

    fun addHoliday(name: String, month: Int, day: Int, prefix: String) {
        holidays.addHoliday(name, month, day, prefix)
        saveCustomHolidays()
    }

    fun saveBirthdays() {
        appPrefs?.saveBirthdays(birthDays.all)
    }

    fun addBirthday(name: String, month: Int, day: Int, yours: Boolean) {
        birthDays.addBirthday(name, month, day, yours)
        saveBirthdays()
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
        return all.filter { it.month == month+1 && it.day == day }
    }
    fun getUpcomingBirthdays(month: Int, day: Int): List<birthday> {
        return all.filter { (it.month == month+1 && it.day >= day) || (it.month > month+1) }
    }
    fun getAllBirthdays(): List<birthday> {
        return all
    }
    fun addBirthday(name:  String, month: Int, day: Int, yours: Boolean) {
        all.add(birthday(name, month, day, yours))
    }
    fun insertBirthday(birth: birthday){
        all.add(birth)
    }
}

//no more funsies
data class holiday(
    val name:  String,
    val month: Int,
    val day:   Int,
    val emoji: String,
    val prefix: String = "happy"
)

object holidays {
    val saved= mutableStateListOf<holiday>(
        holiday("New Year's Day",         1,  1,  "🎆"),
        holiday("Valentine's Day",        2,  14, "💝"),
        holiday("St. Patrick's Day",      3,  17, "☘️"),
        holiday("Independence Day",       7,  4,  "🇺🇸"),
        holiday("Halloween",              10, 31, "🎃"),
        holiday("Veterans Day",           11, 11, "🎖️"),
        holiday("Christmas Eve",          12, 24, "🌟", "merry"),
        holiday("Christmas Day",          12, 25, "🎄", "merry"),
        holiday("New Year's Eve",         12, 31, "🥂"),
        holiday("Heilige Drei Könige",               1,  6,  "⭐"),   // Heilige Drei Könige (BY, BW, ST)
        holiday("Internationaler Frauentag", 3, 8, "💐"),  // Internationaler Frauentag (BE, MV, TH)
        holiday("Tag der Arbeit",             5,  1,  "⚒️"),   // Tag der Arbeit
        holiday("Mariä Himmelfahrt",     8,  15, "🕊️"),  // Mariä Himmelfahrt (BY, SL)
        holiday("Tag der Deutschen Einheit",       10, 3,  "🇩🇪"),  // Tag der Deutschen Einheit
        holiday("Reformationstag",        10, 31, "⛪"),   // Reformationstag (BB, HB, HH, MV, NI, SN, ST, SH, TH)
        holiday("Allerheiligen",        11, 1,  "🕯️"),  // Allerheiligen (BW, BY, NW, RP, SL)
        holiday("Nikolaustag",       12, 6,  "🎅"),   // Nikolaustag (traditional)
        holiday("1. Weinachtstag",          12, 25, "🎄", "frohe"),   // 1. Weihnachtstag
        holiday("2. Weihnachtstag",   12, 26, "🎁", "frohe"),   // 2. Weihnachtstag
    )

    //stupid easter logic(made by ai)
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

    fun moveable(year: Int): List<holiday> {
        val (easterMonth, easterDay) = easterSunday(year)

        fun offset(n: Int): Pair<Int, Int> = addDays(easterMonth, easterDay, year, n)

        val (goodFriM,   goodFriD)   = offset(-2)   // Good Friday / Karfreitag
        val (easterMonM, easterMonD) = offset(1)     // Easter Monday / Ostermontag
        val (ascM,       ascD)       = offset(39)    // Ascension / Christi Himmelfahrt
        val (whitSunM,   whitSunD)   = offset(49)    // Whit Sunday / Pfingstsonntag
        val (whitMonM,   whitMonD)   = offset(50)    // Whit Monday / Pfingstmontag
        val (corpusM,    corpusD)    = offset(60)    // Corpus Christi / Fronleichnam (BW, BY, HE, NW, RP, SL)
        val mlkDay        = nthWeekday(year, 1,  java.util.Calendar.MONDAY, 3)   // 3rd Mon Jan
        val presidentsDay = nthWeekday(year, 2,  java.util.Calendar.MONDAY, 3)   // 3rd Mon Feb
        val memorialDay   = lastWeekday(year, 5,  java.util.Calendar.MONDAY)     // Last Mon May
        val laborDay      = nthWeekday(year, 9,  java.util.Calendar.MONDAY, 1)   // 1st Mon Sep
        val columbusDay   = nthWeekday(year, 10, java.util.Calendar.MONDAY, 2)   // 2nd Mon Oct
        val thanksgiving  = nthWeekday(year, 11, java.util.Calendar.THURSDAY, 4) // 4th Thu Nov
        val motherDay     = nthWeekday(year, 5,  java.util.Calendar.SUNDAY, 2)   // 2nd Sun May
        val fatherDay     = nthWeekday(year, 6,  java.util.Calendar.SUNDAY, 3)   // 3rd Sun Jun

        return listOf(
            holiday("Martin Luther King Jr. Day", mlkDay.first,        mlkDay.second,        "✊"),
            holiday("Presidents' Day",            presidentsDay.first,  presidentsDay.second, "🏛️"),
            holiday("Mother's Day",               motherDay.first,      motherDay.second,     "💐"),
            holiday("Memorial Day",               memorialDay.first,    memorialDay.second,   "🪖"),
            holiday("Father's Day",               fatherDay.first,      fatherDay.second,     "👔"),
            holiday("Labor Day",                  laborDay.first,       laborDay.second,      "⚒️"),
            holiday("Columbus Day",               columbusDay.first,    columbusDay.second,   "⛵"),
            holiday("Thanksgiving",               thanksgiving.first,   thanksgiving.second,  "🦃"),
            holiday("Karfreitag",     goodFriM,   goodFriD,   "✝️"),  // Karfreitag
            holiday("Ostersonntag",   easterMonth, easterDay,  "🐣"),  // Ostersonntag
            holiday("Ostermontag",   easterMonM, easterMonD, "🥚"),  // Ostermontag
            holiday("Christi Himmelfahrt",   ascM,       ascD,       "☁️"),  // Christi Himmelfahrt
            holiday("Pfingstsonntag",     whitSunM,   whitSunD,   "🕊️"), // Pfingstsonntag
            holiday("Pfingstmontag",     whitMonM,   whitMonD,   "🌸"),  // Pfingstmontag
            holiday("Fronleichnam",  corpusM,    corpusD,    "🌿"),  // Fronleichnam
        )
    }

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

    private var cachedYear: Int = -1
    private var cachedMoveable: List<holiday> = emptyList()

    fun getForDay(month: Int, day: Int, year: Int): List<holiday> {
        if (year != cachedYear) {
            cachedYear = year
            cachedMoveable = moveable(year)
        }
        val allSaved = (saved).filter { it.month == month && it.day == day }
        val allMoveable = cachedMoveable.filter { it.month == month && it.day == day }
        return (allSaved + allMoveable).distinctBy { it.name }
    }
    fun addHoliday(name: String, month: Int, day: Int, prefix: String) {
        saved.add(holiday(name, month, day, "🎉", prefix))
    }
    fun insertHoliday(hol : holiday) {
        saved.add(hol)
    }
    fun getAllHolidays(year: Int): List<holiday>{
        if (year != cachedYear) {
            cachedYear = year
            cachedMoveable = moveable(year)
        }
        return (saved + cachedMoveable).distinctBy { it.name }
    }
    fun getUpcomingHolidays(month: Int, day: Int, year: Int): List<holiday>{
        if (year != cachedYear) {
            cachedYear = year
            cachedMoveable = moveable(year)
        }
        val allSaved = (saved).filter { (it.month == month+1 && it.day >= day) || (it.month>month+1) }
        val allMoveable = cachedMoveable.filter { (it.month == month+1 && it.day >= day) || (it.month>month+1) }
        return (allSaved + allMoveable).distinctBy { it.name }
    }
}
