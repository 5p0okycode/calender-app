package com.crochet.calendar

import java.util.Calendar as JC

class CalendarLogic {
    val months: Array<String> = arrayOf(
        "January", "February", "March",     "April",
        "May",     "June",     "July",      "August",
        "September","October", "November",  "December"
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
        get() {
            return this.curDay
        }
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

    // Fun helper Friends

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

    //check if day is the actual day
    fun isToday(day: Int): Boolean =
        curYear == todayYear && curMonth == todayMonth && day == todayDay


    companion object {
        fun isLeapYear(year: Int): Boolean =
            year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
    }
}
